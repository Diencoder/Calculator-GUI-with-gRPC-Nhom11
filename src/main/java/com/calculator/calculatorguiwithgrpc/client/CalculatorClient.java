package com.calculator.calculatorguiwithgrpc.client;

import com.calculator.calculatorguiwithgrpc.proto.CalculatorProtos.*;
import com.calculator.calculatorguiwithgrpc.proto.CalculatorServiceGrpc;
import com.calculator.calculatorguiwithgrpc.utils.ValidationUtils;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * gRPC Client for Calculator Service
 * Handles communication with the Calculator Server
 * 
 * @author Calculator Team
 * @version 1.0
 */
public class CalculatorClient {
    
    private static final Logger logger = LoggerFactory.getLogger(CalculatorClient.class);
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9090;
    
    private final ManagedChannel channel;
    private final CalculatorServiceGrpc.CalculatorServiceBlockingStub blockingStub;
    private final CalculatorServiceGrpc.CalculatorServiceStub asyncStub;
    private final ValidationUtils validationUtils;
    
    /**
     * Constructor with default host and port
     */
    public CalculatorClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }
    
    /**
     * Constructor with custom host and port
     */
    public CalculatorClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = CalculatorServiceGrpc.newBlockingStub(channel);
        this.asyncStub = CalculatorServiceGrpc.newStub(channel);
        this.validationUtils = new ValidationUtils();
        
        logger.info("Calculator Client initialized - Host: {}, Port: {}", host, port);
    }
    
    /**
     * Perform a simple calculation
     */
    public CalculationResult calculate(double operand1, double operand2, String operator) {
        logger.info("Sending calculation request: {} {} {}", operand1, operator, operand2);
        
        try {
            // Validate input
            if (!validationUtils.isValidOperator(operator)) {
                return new CalculationResult(false, 0.0, "Invalid operator: " + operator);
            }
            
            if (!validationUtils.isValidNumber(operand1) || !validationUtils.isValidNumber(operand2)) {
                return new CalculationResult(false, 0.0, "Invalid number format");
            }
            
            // Create request
            String requestId = UUID.randomUUID().toString();
            CalculationRequest request = CalculationRequest.newBuilder()
                    .setOperand1(operand1)
                    .setOperand2(operand2)
                    .setOperator(operator)
                    .setRequestId(requestId)
                    .build();
            
            // Send request and get response
            CalculationResponse response = blockingStub.calculate(request);
            
            if (response.getSuccess()) {
                logger.info("Calculation successful: {} {} {} = {}", operand1, operator, operand2, response.getResult());
                return new CalculationResult(true, response.getResult(), null);
            } else {
                logger.error("Calculation failed: {}", response.getErrorMessage());
                return new CalculationResult(false, 0.0, response.getErrorMessage());
            }
            
        } catch (StatusRuntimeException e) {
            logger.error("gRPC call failed", e);
            return new CalculationResult(false, 0.0, "gRPC call failed: " + e.getStatus().getDescription());
        } catch (Exception e) {
            logger.error("Unexpected error during calculation", e);
            return new CalculationResult(false, 0.0, "Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Perform an advanced calculation
     */
    public CalculationResult calculateAdvanced(double operand1, double operand2, String operator) {
        logger.info("Sending advanced calculation request: {} {} {}", operand1, operator, operand2);
        
        try {
            // Create request for advanced calculation
            String requestId = UUID.randomUUID().toString();
            CalculationRequest request = CalculationRequest.newBuilder()
                    .setOperand1(operand1)
                    .setOperand2(operand2)
                    .setOperator(operator)
                    .setRequestId(requestId)
                    .build();
            
            // Send request and get response
            CalculationResponse response = blockingStub.calculate(request);
            
            if (response.getSuccess()) {
                logger.info("Advanced calculation successful: {} {} {} = {}", operand1, operator, operand2, response.getResult());
                return new CalculationResult(true, response.getResult(), null);
            } else {
                logger.error("Advanced calculation failed: {}", response.getErrorMessage());
                return new CalculationResult(false, 0.0, response.getErrorMessage());
            }
            
        } catch (StatusRuntimeException e) {
            logger.error("gRPC call failed for advanced calculation", e);
            return new CalculationResult(false, 0.0, "gRPC call failed: " + e.getStatus().getDescription());
        } catch (Exception e) {
            logger.error("Unexpected error during advanced calculation", e);
            return new CalculationResult(false, 0.0, "Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Check server health
     */
    public boolean isServerHealthy() {
        try {
            HealthCheckRequest request = HealthCheckRequest.newBuilder()
                    .setService("calculator")
                    .build();
            
            HealthCheckResponse response = blockingStub.healthCheck(request);
            
            boolean isHealthy = response.getStatus() == HealthCheckResponse.ServingStatus.SERVING;
            logger.info("Server health check: {}", isHealthy ? "HEALTHY" : "UNHEALTHY");
            
            return isHealthy;
            
        } catch (Exception e) {
            logger.error("Health check failed", e);
            return false;
        }
    }
    
    /**
     * Shutdown the client
     */
    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            logger.info("Calculator Client shutdown completed");
        } catch (InterruptedException e) {
            logger.error("Error during client shutdown", e);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Result class for calculation operations
     */
    public static class CalculationResult {
        private final boolean success;
        private final double result;
        private final String errorMessage;
        
        public CalculationResult(boolean success, double result, String errorMessage) {
            this.success = success;
            this.result = result;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public double getResult() {
            return result;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        @Override
        public String toString() {
            if (success) {
                return "CalculationResult{success=true, result=" + result + "}";
            } else {
                return "CalculationResult{success=false, error='" + errorMessage + "'}";
            }
        }
    }
}
