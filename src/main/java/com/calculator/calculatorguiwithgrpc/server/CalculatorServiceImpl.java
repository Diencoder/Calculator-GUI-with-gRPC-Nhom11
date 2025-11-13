package com.calculator.calculatorguiwithgrpc.server;

import com.calculator.calculatorguiwithgrpc.proto.CalculatorProtos.*;
import com.calculator.calculatorguiwithgrpc.proto.CalculatorServiceGrpc;
import com.calculator.calculatorguiwithgrpc.utils.ValidationUtils;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of Calculator Service
 * Handles basic calculation operations
 * 
 * @author Calculator Team
 * @version 1.0
 */
public class CalculatorServiceImpl extends CalculatorServiceGrpc.CalculatorServiceImplBase {
    
    private static final Logger logger = LoggerFactory.getLogger(CalculatorServiceImpl.class);
    private final ValidationUtils validationUtils;
    
    public CalculatorServiceImpl() {
        this.validationUtils = new ValidationUtils();
    }
    
    @Override
    public void calculate(CalculationRequest request, StreamObserver<CalculationResponse> responseObserver) {
        logger.info("Received calculation request: {} {} {}", request.getOperand1(), request.getOperator(), request.getOperand2());
        
        try {
            // Validate input
            if (!validationUtils.isValidOperator(request.getOperator())) {
                sendErrorResponse(responseObserver, "Invalid operator: " + request.getOperator(), request.getRequestId());
                return;
            }
            
            if (!validationUtils.isValidNumber(request.getOperand1()) || !validationUtils.isValidNumber(request.getOperand2())) {
                sendErrorResponse(responseObserver, "Invalid number format", request.getRequestId());
                return;
            }
            
            // Perform calculation
            double result = performCalculation(request.getOperand1(), request.getOperand2(), request.getOperator());
            
            // Send success response
            CalculationResponse response = CalculationResponse.newBuilder()
                    .setResult(result)
                    .setSuccess(true)
                    .setRequestId(request.getRequestId())
                    .setTimestamp(System.currentTimeMillis())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
            logger.info("Calculation completed successfully: {} {} {} = {}", 
                       request.getOperand1(), request.getOperator(), request.getOperand2(), result);
            
        } catch (ArithmeticException e) {
            logger.error("Arithmetic error in calculation", e);
            sendErrorResponse(responseObserver, "Arithmetic error: " + e.getMessage(), request.getRequestId());
        } catch (Exception e) {
            logger.error("Unexpected error in calculation", e);
            sendErrorResponse(responseObserver, "Internal server error", request.getRequestId());
        }
    }
    
    @Override
    public StreamObserver<CalculationRequest> streamCalculate(StreamObserver<CalculationResponse> responseObserver) {
        return new StreamObserver<CalculationRequest>() {
            @Override
            public void onNext(CalculationRequest request) {
                logger.info("Received stream calculation request: {} {} {}", 
                           request.getOperand1(), request.getOperator(), request.getOperand2());
                
                try {
                    // Validate and calculate
                    if (!validationUtils.isValidOperator(request.getOperator()) ||
                        !validationUtils.isValidNumber(request.getOperand1()) ||
                        !validationUtils.isValidNumber(request.getOperand2())) {
                        
                        sendErrorResponse(responseObserver, "Invalid input", request.getRequestId());
                        return;
                    }
                    
                    double result = performCalculation(request.getOperand1(), request.getOperand2(), request.getOperator());
                    
                    CalculationResponse response = CalculationResponse.newBuilder()
                            .setResult(result)
                            .setSuccess(true)
                            .setRequestId(request.getRequestId())
                            .setTimestamp(System.currentTimeMillis())
                            .build();
                    
                    responseObserver.onNext(response);
                    
                } catch (Exception e) {
                    logger.error("Error in stream calculation", e);
                    sendErrorResponse(responseObserver, "Calculation error: " + e.getMessage(), request.getRequestId());
                }
            }
            
            @Override
            public void onError(Throwable t) {
                logger.error("Error in stream calculation", t);
            }
            
            @Override
            public void onCompleted() {
                logger.info("Stream calculation completed");
                responseObserver.onCompleted();
            }
        };
    }
    
    @Override
    public void healthCheck(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        logger.info("Health check requested for service: {}", request.getService());
        
        HealthCheckResponse response = HealthCheckResponse.newBuilder()
                .setStatus(HealthCheckResponse.ServingStatus.SERVING)
                .setMessage("Calculator service is running")
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    
    /**
     * Perform the actual calculation
     */
    private double performCalculation(double operand1, double operand2, String operator) {
        return switch (operator) {
            case "+" -> operand1 + operand2;
            case "-" -> operand1 - operand2;
            case "*" -> operand1 * operand2;
            case "/" -> {
                if (operand2 == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                yield operand1 / operand2;
            }
            case "%" -> operand1 % operand2;
            case "^" -> Math.pow(operand1, operand2);
            default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
        };
    }
    
    /**
     * Send error response
     */
    private void sendErrorResponse(StreamObserver<CalculationResponse> responseObserver, String errorMessage, String requestId) {
        CalculationResponse errorResponse = CalculationResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage(errorMessage)
                .setRequestId(requestId)
                .setTimestamp(System.currentTimeMillis())
                .build();
        
        responseObserver.onNext(errorResponse);
        responseObserver.onCompleted();
    }
}
