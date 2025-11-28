package com.calculator.calculatorguiwithgrpc.server;

import com.calculator.calculatorguiwithgrpc.proto.CalculatorProtos.*;
import com.calculator.calculatorguiwithgrpc.proto.CalculatorServiceGrpc;
import com.calculator.calculatorguiwithgrpc.utils.ValidationUtils;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
    public void healthCheck(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        logger.info("Health check request received for service: {}", request.getService());
        
        HealthCheckResponse response = HealthCheckResponse.newBuilder()
                .setStatus(HealthCheckResponse.ServingStatus.SERVING)
                .setMessage("Calculator service is healthy and ready")
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        
        logger.info("Health check completed successfully");
    }
    
    @Override
    public void calculate(CalculationRequest request, StreamObserver<CalculationResponse> responseObserver) {
        logger.info("Advanced calculation request: {} {} {}", request.getOperand1(), request.getOperator(), request.getOperand2());
        
        try {
            // Validate input
            if (!validationUtils.isValidAdvancedOperator(request.getOperator())) {
                sendErrorResponse(responseObserver, "Invalid operator: " + request.getOperator(), request.getRequestId());
                return;
            }
            
            // For single-operand operations, operand2 might not be needed
            boolean isSingleOperandOperation = isSingleOperandOperation(request.getOperator());
            if (!validationUtils.isValidNumber(request.getOperand1())) {
                sendErrorResponse(responseObserver, "Invalid number format for operand1", request.getRequestId());
                return;
            }
            
            if (!isSingleOperandOperation && !validationUtils.isValidNumber(request.getOperand2())) {
                sendErrorResponse(responseObserver, "Invalid number format for operand2", request.getRequestId());
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
     * Check if operator requires only one operand
     */
    private boolean isSingleOperandOperation(String operator) {
        return switch (operator) {
            case "sqrt", "cbrt", "exp", "log", "log10", "ln",
                 "sin", "cos", "tan", "asin", "acos", "atan",
                 "sinh", "cosh", "tanh",
                 "abs", "ceil", "floor", "round", "NOT" -> true;
            default -> false;
        };
    }
    
    /**
     * Perform advanced mathematical calculations
     * 
     * Chế độ Khoa học:
     * - Lượng giác: sin, cos, tan, asin, acos, atan, sinh, cosh, tanh
     * - Số mũ và Căn: exp, pow, sqrt, cbrt, nthroot
     * - Logarit: log (ln), log10
     * 
     * Chế độ Lập trình viên:
     * - Chuyển đổi hệ cơ số: convertBase
     */
    private double performAdvancedCalculation(double operand1, double operand2, String operator) {
        return switch (operator) {
            // Basic operations
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
            
            // Số mũ và Căn (Chế độ Khoa học)
            case "^", "pow" -> Math.pow(operand1, operand2);
            case "sqrt" -> {
                if (operand1 < 0) {
                    throw new ArithmeticException("Square root of negative number");
                }
                yield Math.sqrt(operand1);
            }
            case "cbrt" -> Math.cbrt(operand1);
            case "nthroot" -> {
                if (operand1 < 0 && operand2 % 2 == 0) {
                    throw new ArithmeticException("Even root of negative number");
                }
                if (operand2 == 0) {
                    throw new ArithmeticException("Root degree cannot be zero");
                }
                yield Math.pow(operand1, 1.0 / operand2);
            }
            case "exp" -> Math.exp(operand1);
            
            // Logarit (Chế độ Khoa học)
            case "log", "ln" -> {
                if (operand1 <= 0) {
                    throw new ArithmeticException("Logarithm of non-positive number");
                }
                yield Math.log(operand1); // Natural logarithm (ln)
            }
            case "log10" -> {
                if (operand1 <= 0) {
                    throw new ArithmeticException("Log base 10 of non-positive number");
                }
                yield Math.log10(operand1);
            }
            
            // Lượng giác (Chế độ Khoa học) - góc tính bằng độ
            case "sin" -> Math.sin(Math.toRadians(operand1));
            case "cos" -> Math.cos(Math.toRadians(operand1));
            case "tan" -> Math.tan(Math.toRadians(operand1));
            
            // Hàm lượng giác ngược (trả về độ)
            case "asin" -> {
                if (operand1 < -1 || operand1 > 1) {
                    throw new ArithmeticException("Arcsin domain is [-1, 1]");
                }
                yield Math.toDegrees(Math.asin(operand1));
            }
            case "acos" -> {
                if (operand1 < -1 || operand1 > 1) {
                    throw new ArithmeticException("Arccos domain is [-1, 1]");
                }
                yield Math.toDegrees(Math.acos(operand1));
            }
            case "atan" -> Math.toDegrees(Math.atan(operand1));
            
            // Hàm lượng giác hyperbolic
            case "sinh" -> Math.sinh(operand1);
            case "cosh" -> Math.cosh(operand1);
            case "tanh" -> Math.tanh(operand1);
            
            // Chuyển đổi hệ cơ số (Chế độ Lập trình viên)
            // operand1: số cần chuyển đổi (dạng số nguyên, được hiểu là số ở hệ cơ số fromBase)
            // operand2: fromBase * 100 + toBase (ví dụ: 2*100 + 10 = 210 nghĩa là từ nhị phân sang thập phân)
            // Lưu ý: Vì proto chỉ hỗ trợ double, kết quả trả về là số thập phân
            // Để chuyển đổi sang hệ cơ số khác và hiển thị, cần một RPC riêng trả về string
            case "convertBase" -> {
                int fromBase = (int) Math.floor(operand2 / 100);
                int toBase = (int) (operand2 % 100);
                if (fromBase < 2 || fromBase > 36 || toBase < 2 || toBase > 36) {
                    throw new ArithmeticException("Base must be between 2 and 36");
                }
                // Convert number from source base to decimal
                // operand1 is treated as a number in the source base
                String numberStr = String.valueOf((long) Math.abs(operand1));
                double decimalValue = convertBaseToDecimal(numberStr, fromBase);
                if (operand1 < 0) {
                    decimalValue = -decimalValue;
                }
                // Return decimal representation
                // Note: To get result in target base as string, a separate RPC would be needed
                yield decimalValue;
            }
            
            // Utility functions
            case "abs" -> Math.abs(operand1);
            case "ceil" -> Math.ceil(operand1);
            case "floor" -> Math.floor(operand1);
            case "round" -> Math.round(operand1);
            case "max" -> Math.max(operand1, operand2);
            case "min" -> Math.min(operand1, operand2);
            
            // Bitwise operations (Programmer mode)
            case "AND" -> {
                long op1Long = (long) operand1;
                long op2Long = (long) operand2;
                yield (double) (op1Long & op2Long);
            }
            case "OR" -> {
                long op1Long = (long) operand1;
                long op2Long = (long) operand2;
                yield (double) (op1Long | op2Long);
            }
            case "XOR" -> {
                long op1Long = (long) operand1;
                long op2Long = (long) operand2;
                yield (double) (op1Long ^ op2Long);
            }
            case "NOT" -> {
                long op1Long = (long) operand1;
                yield (double) (~op1Long);
            }
            case "LSH" -> {
                long op1Long = (long) operand1;
                int shiftAmount = (int) operand2;
                if (shiftAmount < 0 || shiftAmount > 63) {
                    throw new ArithmeticException("Shift amount must be between 0 and 63");
                }
                yield (double) (op1Long << shiftAmount);
            }
            case "RSH" -> {
                long op1Long = (long) operand1;
                int shiftAmount = (int) operand2;
                if (shiftAmount < 0 || shiftAmount > 63) {
                    throw new ArithmeticException("Shift amount must be between 0 and 63");
                }
                yield (double) (op1Long >> shiftAmount);
            }
            
            default -> throw new IllegalArgumentException("Unsupported advanced operator: " + operator);
        };
    }
    
    /**
     * Convert number from given base to decimal
     * Helper method for base conversion
     */
    private double convertBaseToDecimal(String number, int fromBase) {
        try {
            // Remove any non-alphanumeric characters except minus sign
            number = number.trim().toUpperCase();
            if (number.startsWith("-")) {
                return -Long.parseLong(number.substring(1), fromBase);
            }
            return Long.parseLong(number, fromBase);
        } catch (NumberFormatException e) {
            throw new ArithmeticException("Invalid number for base " + fromBase + ": " + number);
        }
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
