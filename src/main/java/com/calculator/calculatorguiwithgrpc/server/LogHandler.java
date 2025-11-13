package com.calculator.calculatorguiwithgrpc.server;

import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * gRPC Interceptor for logging requests and responses
 * 
 * @author Calculator Team
 * @version 1.0
 */
public class LogHandler implements ServerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(LogHandler.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        
        String timestamp = LocalDateTime.now().format(formatter);
        String methodName = call.getMethodDescriptor().getFullMethodName();
        
        logger.info("[{}] gRPC Request - Method: {}, Headers: {}", timestamp, methodName, headers);
        
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                    @Override
                    public void sendMessage(RespT message) {
                        logger.info("[{}] gRPC Response - Method: {}, Message: {}", 
                                   timestamp, methodName, message);
                        super.sendMessage(message);
                    }
                    
                    @Override
                    public void close(Status status, Metadata trailers) {
                        if (status.isOk()) {
                            logger.info("[{}] gRPC Call completed successfully - Method: {}", 
                                       timestamp, methodName);
                        } else {
                            logger.error("[{}] gRPC Call failed - Method: {}, Status: {}, Trailers: {}", 
                                        timestamp, methodName, status, trailers);
                        }
                        super.close(status, trailers);
                    }
                }, headers)) {
            
            @Override
            public void onMessage(ReqT message) {
                logger.info("[{}] gRPC Request Message - Method: {}, Message: {}", 
                           timestamp, methodName, message);
                super.onMessage(message);
            }
            
            @Override
            public void onHalfClose() {
                logger.info("[{}] gRPC Request half-closed - Method: {}", timestamp, methodName);
                super.onHalfClose();
            }
            
            @Override
            public void onCancel() {
                logger.warn("[{}] gRPC Request cancelled - Method: {}", timestamp, methodName);
                super.onCancel();
            }
            
            @Override
            public void onComplete() {
                logger.info("[{}] gRPC Request completed - Method: {}", timestamp, methodName);
                super.onComplete();
            }
        };
    }
}
