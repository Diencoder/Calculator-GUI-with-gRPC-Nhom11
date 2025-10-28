package com.calculator.calculatorguiwithgrpc.server;

import com.calculator.calculatorguiwithgrpc.proto.CalculatorProtos.*;
import com.calculator.calculatorguiwithgrpc.proto.CalculatorServiceGrpc;
import com.calculator.calculatorguiwithgrpc.utils.ValidationUtils;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Advanced Calculator Service Implementation
 * Provides advanced mathematical operations
 * 
 * @author Calculator Team
 * @version 1.0
 */
public class AdvancedCalculatorService extends CalculatorServiceGrpc.CalculatorServiceImplBase {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedCalculatorService.class);
    private final ValidationUtils validationUtils;
    
    public AdvancedCalculatorService() {
        this.validationUtils = new ValidationUtils();
    }
    
    @Override
    public void calculate(CalculationRequest request, StreamObserver<CalculationResponse> responseObserver) {
        logger.info("Advanced calculation request: {} {} {}", request.getOperand1(), request.getOperator(), request.getOperand2());
        
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
            
            // Perform advanced calculation
            double result = performAdvancedCalculation(request.getOperand1(), request.getOperand2(), request.getOperator());
            
            // Send success response
            CalculationResponse response = CalculationResponse.newBuilder()
                    .setResult(result)
                    .setSuccess(true)
                    .setRequestId(request.getRequestId())
                    .setTimestamp(System.currentTimeMillis())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
            logger.info("Advanced calculation completed: {} {} {} = {}", 
                       request.getOperand1(), request.getOperator(), request.getOperand2(), result);
            
        } catch (ArithmeticException e) {
            logger.error("Arithmetic error in advanced calculation", e);
            sendErrorResponse(responseObserver, "Arithmetic error: " + e.getMessage(), request.getRequestId());
        } catch (Exception e) {
            logger.error("Unexpected error in advanced calculation", e);
            sendErrorResponse(responseObserver, "Internal server error", request.getRequestId());
        }
    }
    
    /**
     * Perform advanced mathematical calculations
     */
    private double performAdvancedCalculation(double operand1, double operand2, String operator) {
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
            case "sqrt" -> Math.sqrt(operand1);
            case "log" -> Math.log(operand1);
            case "sin" -> Math.sin(Math.toRadians(operand1));
            case "cos" -> Math.cos(Math.toRadians(operand1));
            case "tan" -> Math.tan(Math.toRadians(operand1));
            case "abs" -> Math.abs(operand1);
            case "ceil" -> Math.ceil(operand1);
            case "floor" -> Math.floor(operand1);
            case "round" -> Math.round(operand1);
            case "max" -> Math.max(operand1, operand2);
            case "min" -> Math.min(operand1, operand2);
            default -> throw new IllegalArgumentException("Unsupported advanced operator: " + operator);
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
