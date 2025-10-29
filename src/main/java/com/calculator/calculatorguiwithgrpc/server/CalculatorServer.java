package com.calculator.calculatorguiwithgrpc.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import com.calculator.calculatorguiwithgrpc.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * gRPC Server for Calculator Service
 * Handles calculation requests from clients
 * 
 * @author Calculator Team
 * @version 1.0
 */
public class CalculatorServer {
    
    private static final Logger logger = LoggerFactory.getLogger(CalculatorServer.class);
    private static final int PORT = 9090;
    
    private Server server;
    
    /**
     * Start the gRPC server
     */
    public void start() throws IOException {
        logger.info("Starting Calculator gRPC Server on port {}", PORT);
        
        server = ServerBuilder.forPort(PORT)
                .addService(new CalculatorServiceImpl())
                .addService(new AdvancedCalculatorService())
                .intercept(new LogHandler())
                .build()
                .start();
        
        logger.info("Calculator gRPC Server started successfully on port {}", PORT);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Calculator gRPC Server...");
            try {
                CalculatorServer.this.stop();
            } catch (InterruptedException e) {
                logger.error("Error during server shutdown", e);
                Thread.currentThread().interrupt();
            }
        }));
    }
    
    /**
     * Stop the gRPC server
     */
    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
            logger.info("Calculator gRPC Server stopped");
        }
    }
    
    /**
     * Block until the server is terminated
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
    
    /**
     * Main method to start the server
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        CalculatorServer server = new CalculatorServer();
        server.start();
        server.blockUntilShutdown();
    }
}
