package com.calculator.calculatorguiwithgrpc.server;

import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Enhanced gRPC Interceptor for logging requests and responses
 * with network programming metrics tracking.
 * 
 * This interceptor demonstrates network programming concepts:
 * 
 * 1. CONNECTION LIFECYCLE TRACKING:
 *    - onMessage: When data arrives from client (network receive)
 *    - sendMessage: When data is sent to client (network send)
 *    - onHalfClose: When client closes its send stream (TCP half-close)
 *    - onCancel: When client cancels the request (connection abort)
 *    - onComplete: When connection is fully closed
 * 
 * 2. NETWORK STATISTICS:
 *    - Tracks bytes sent/received (approximate via message size)
 *    - Monitors connection establishment and termination
 *    - Tracks network errors and timeouts
 * 
 * 3. REQUEST/RESPONSE TIMING:
 *    - Measures time between request and response
 *    - Helps identify network latency issues
 * 
 * @author Calculator Team
 * @version 2.0 - Enhanced for Network Programming Education
 */
public class LogHandler implements ServerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(LogHandler.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final CalculatorServer server;
    
    /**
     * Constructor with server instance for network statistics tracking
     * 
     * @param server The CalculatorServer instance to track network metrics
     */
    public LogHandler(CalculatorServer server) {
        this.server = server;
    }
    
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        
        String timestamp = LocalDateTime.now().format(formatter);
        String methodName = call.getMethodDescriptor().getFullMethodName();
        long requestStartTime = System.currentTimeMillis();
        
        // NETWORK PROGRAMMING CONCEPT: Connection Establishment
        // When a new client connects, track the connection
        if (server != null) {
            server.onConnectionEstablished();
        }
        
        logger.info("[{}] gRPC Request - Method: {}, Headers: {}", timestamp, methodName, headers);
        
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                    @Override
                    public void sendMessage(RespT message) {
                        // NETWORK PROGRAMMING CONCEPT: Data Transmission
                        // Track bytes sent to client (outgoing network traffic)
                        if (server != null && message != null) {
                            // Approximate message size (gRPC uses protobuf serialization)
                            // In real network programming, you'd track actual bytes sent
                            try {
                                int estimatedSize = message.toString().getBytes().length;
                                server.addBytesSent(estimatedSize);
                            } catch (Exception e) {
                                // Ignore size estimation errors
                            }
                        }
                        
                        long responseTime = System.currentTimeMillis() - requestStartTime;
                        logger.info("[{}] gRPC Response - Method: {}, Response Time: {}ms, Message: {}", 
                                   timestamp, methodName, responseTime, message);
                        super.sendMessage(message);
                    }
                    
                    @Override
                    public void close(Status status, Metadata trailers) {
                        // NETWORK PROGRAMMING CONCEPT: Connection Termination
                        // Handle different connection close scenarios
                        if (status.isOk()) {
                            logger.info("[{}] gRPC Call completed successfully - Method: {}", 
                                       timestamp, methodName);
                        } else {
                            // NETWORK PROGRAMMING CONCEPT: Error Handling
                            // Track network errors (connection failures, timeouts, etc.)
                            if (server != null) {
                                server.onNetworkError();
                                
                                // Check if it's a timeout error
                                if (status.getCode() == Status.Code.DEADLINE_EXCEEDED) {
                                    server.onConnectionTimeout();
                                }
                            }
                            
                            logger.error("[{}] gRPC Call failed - Method: {}, Status: {}, Trailers: {}", 
                                        timestamp, methodName, status, trailers);
                        }
                        
                        // NETWORK PROGRAMMING CONCEPT: Connection Cleanup
                        if (server != null) {
                            server.onConnectionClosed();
                        }
                        
                        super.close(status, trailers);
                    }
                }, headers)) {
            
            @Override
            public void onMessage(ReqT message) {
                // NETWORK PROGRAMMING CONCEPT: Data Reception
                // Track bytes received from client (incoming network traffic)
                if (server != null && message != null) {
                    try {
                        int estimatedSize = message.toString().getBytes().length;
                        server.addBytesReceived(estimatedSize);
                    } catch (Exception e) {
                        // Ignore size estimation errors
                    }
                }
                
                logger.info("[{}] gRPC Request Message - Method: {}, Message: {}", 
                           timestamp, methodName, message);
                super.onMessage(message);
            }
            
            @Override
            public void onHalfClose() {
                // NETWORK PROGRAMMING CONCEPT: TCP Half-Close
                // Client has finished sending data but connection is still open for server response
                // This is a TCP feature where one side closes its write stream
                logger.info("[{}] gRPC Request half-closed - Method: {} (TCP half-close)", 
                           timestamp, methodName);
                super.onHalfClose();
            }
            
            @Override
            public void onCancel() {
                // NETWORK PROGRAMMING CONCEPT: Connection Abort
                // Client cancelled the request before completion
                // This could be due to timeout, user cancellation, or network issues
                if (server != null) {
                    server.onNetworkError();
                    server.onConnectionClosed();
                }
                
                logger.warn("[{}] gRPC Request cancelled - Method: {} (Connection aborted)", 
                           timestamp, methodName);
                super.onCancel();
            }
            
            @Override
            public void onComplete() {
                // NETWORK PROGRAMMING CONCEPT: Connection Completion
                // Full connection lifecycle completed successfully
                logger.info("[{}] gRPC Request completed - Method: {}", timestamp, methodName);
                super.onComplete();
            }
        };
    }
}
