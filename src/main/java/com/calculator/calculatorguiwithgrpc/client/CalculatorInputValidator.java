package com.calculator.calculatorguiwithgrpc.client;

import com.calculator.calculatorguiwithgrpc.config.AppConfig;
import com.calculator.calculatorguiwithgrpc.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class that encapsulates CalculatorClient validation logic so it can be
 * reused and unit-tested independently from the gRPC channel.
 */
public class CalculatorInputValidator {

    private static final Logger logger = LoggerFactory.getLogger(CalculatorInputValidator.class);
    private static final AppConfig appConfig = AppConfig.getInstance();

    private final double maxSafeValue;
    private final double minSafeValue;
    private final double maxExponent;
    private final ValidationUtils validationUtils = new ValidationUtils();
    
    public CalculatorInputValidator() {
        this.maxSafeValue = appConfig.getValidationMaxSafeValue();
        this.minSafeValue = appConfig.getValidationMinSafeValue();
        this.maxExponent = appConfig.getValidationMaxExponent();
    }

    /**
     * Validate inputs for calculator operations.
     */
    public ValidationResult validateInputs(double operand1, double operand2, String operator) {
        return validateInputs(operand1, operand2, operator, false);
    }
    
    /**
     * Validate inputs for calculator operations (basic or advanced).
     */
    public ValidationResult validateInputs(double operand1, double operand2, String operator, boolean isAdvanced) {
        StringBuilder errors = new StringBuilder();

        // 1. Kiểm tra toán tử
        if (operator == null || operator.trim().isEmpty()) {
            errors.append("Toán tử không được để trống. ");
            logger.debug("Toán tử null hoặc rỗng");
        } else {
            boolean isValidOp = isAdvanced 
                ? validationUtils.isValidAdvancedOperator(operator.trim())
                : validationUtils.isValidOperator(operator.trim());
            
            if (!isValidOp) {
                errors.append("Toán tử không hợp lệ: '").append(operator).append("'. ");
                if (isAdvanced) {
                    errors.append("Vui lòng sử dụng toán tử nâng cao hợp lệ. ");
                } else {
                    errors.append("Các toán tử hợp lệ: +, -, *, /, %, ^. ");
                }
                logger.debug("Toán tử không hợp lệ: {}", operator);
            }
        }

        // 2. Kiểm tra số operand1
        String operand1Error = validateOperand(operand1, "Số thứ nhất");
        if (operand1Error != null) {
            errors.append(operand1Error).append(" ");
        }

        // 3. Kiểm tra số operand2 (chỉ khi không phải unary operation)
        boolean isUnaryOperation = isAdvanced && operator != null && 
            (operator.equals("sqrt") || operator.equals("cbrt") || operator.equals("exp") ||
             operator.equals("log") || operator.equals("log10") || operator.equals("ln") ||
             operator.equals("sin") || operator.equals("cos") || operator.equals("tan") ||
             operator.equals("asin") || operator.equals("acos") || operator.equals("atan") ||
             operator.equals("sinh") || operator.equals("cosh") || operator.equals("tanh") ||
             operator.equals("abs") || operator.equals("ceil") || operator.equals("floor") ||
             operator.equals("round") || operator.equals("NOT"));
        
        if (!isUnaryOperation) {
            String operand2Error = validateOperand(operand2, "Số thứ hai");
            if (operand2Error != null) {
                errors.append(operand2Error).append(" ");
            }
        }

        // 4. Kiểm tra các trường hợp đặc biệt theo toán tử
        if (operator != null) {
            boolean isValidOp = isAdvanced 
                ? validationUtils.isValidAdvancedOperator(operator.trim())
                : validationUtils.isValidOperator(operator.trim());
            
            if (isValidOp) {
                String operatorError = validateOperatorSpecificCases(operand1, operand2, operator.trim(), isAdvanced);
                if (operatorError != null) {
                    errors.append(operatorError).append(" ");
                }
            }
        }

        // 5. Kiểm tra overflow tiềm năng (chỉ cho basic operators)
        if (!isAdvanced) {
            String overflowError = checkPotentialOverflow(operand1, operand2, operator);
            if (overflowError != null) {
                errors.append(overflowError).append(" ");
            }
        }

        boolean isValid = errors.length() == 0;
        String errorMessage = isValid ? null : errors.toString().trim();
        return new ValidationResult(isValid, errorMessage);
    }

    private String validateOperand(double operand, String operandName) {
        if (Double.isNaN(operand)) {
            logger.debug("{} là NaN", operandName);
            return operandName + " không phải là số hợp lệ (NaN).";
        }

        if (Double.isInfinite(operand)) {
            logger.debug("{} là Infinity", operandName);
            return operandName + " không được là vô cực (Infinity).";
        }

        if (operand > maxSafeValue) {
            logger.debug("{} quá lớn: {}", operandName, operand);
            return operandName + " quá lớn (>" + maxSafeValue + ").";
        }

        if (operand < minSafeValue) {
            logger.debug("{} quá nhỏ: {}", operandName, operand);
            return operandName + " quá nhỏ (<" + minSafeValue + ").";
        }

        return null; // Hợp lệ
    }

    private String validateOperatorSpecificCases(double operand1, double operand2, String operator, boolean isAdvanced) {
        switch (operator) {
            case "/":
                if (operand2 == 0.0) {
                    logger.debug("Phát hiện chia cho 0");
                    return "Không thể chia cho 0.";
                }
                break;

            case "%":
                if (operand2 == 0.0) {
                    logger.debug("Phát hiện modulo cho 0");
                    return "Không thể lấy phần dư khi chia cho 0.";
                }
                break;

            case "^":
            case "pow":
                if (Math.abs(operand2) > maxExponent) {
                    logger.debug("Số mũ quá lớn: {}", operand2);
                    return "Số mũ quá lớn (|" + operand2 + "| > " + maxExponent + ").";
                }
                if (operand1 < 0 && operand2 != (long) operand2) {
                    logger.debug("Cơ số âm với số mũ không nguyên: {} ^ {}", operand1, operand2);
                    return "Không thể tính lũy thừa với cơ số âm và số mũ không nguyên.";
                }
                break;
        }
        
        // Advanced operator validations
        if (isAdvanced) {
            switch (operator) {
                case "sqrt":
                    if (operand1 < 0) {
                        logger.debug("Căn bậc hai của số âm: {}", operand1);
                        return "Không thể tính căn bậc hai của số âm.";
                    }
                    break;
                    
                case "nthroot":
                    if (operand1 < 0 && operand2 % 2 == 0) {
                        logger.debug("Căn bậc chẵn của số âm: {} root {}", operand1, operand2);
                        return "Không thể tính căn bậc chẵn của số âm.";
                    }
                    if (operand2 == 0) {
                        logger.debug("Bậc căn bằng 0");
                        return "Bậc căn không thể bằng 0.";
                    }
                    break;
                    
                case "log":
                case "ln":
                case "log10":
                    if (operand1 <= 0) {
                        logger.debug("Logarit của số không dương: {}", operand1);
                        return "Không thể tính logarit của số không dương.";
                    }
                    break;
                    
                case "asin":
                case "acos":
                    if (operand1 < -1 || operand1 > 1) {
                        logger.debug("Arcsin/Arccos ngoài miền [-1, 1]: {}", operand1);
                        return "Giá trị phải nằm trong khoảng [-1, 1].";
                    }
                    break;
                    
                case "LSH":
                case "RSH":
                    if (operand2 < 0 || operand2 > 63) {
                        logger.debug("Shift amount không hợp lệ: {}", operand2);
                        return "Số lượng dịch phải nằm trong khoảng [0, 63].";
                    }
                    break;
            }
        }

        return null;
    }

    private String checkPotentialOverflow(double operand1, double operand2, String operator) {
        if (operator == null || !validationUtils.isValidOperator(operator.trim())) {
            return null;
        }

        try {
            switch (operator.trim()) {
                case "*":
                    if (Math.abs(operand1) > 1 && Math.abs(operand2) > Double.MAX_VALUE / Math.abs(operand1)) {
                        logger.debug("Overflow tiềm năng khi nhân: {} * {}", operand1, operand2);
                        return "Kết quả phép nhân có thể quá lớn.";
                    }
                    break;

                case "^":
                    if (Math.abs(operand1) > 1 &&
                            operand2 > Math.log(Double.MAX_VALUE) / Math.log(Math.abs(operand1))) {
                        logger.debug("Overflow tiềm năng khi lũy thừa: {} ^ {}", operand1, operand2);
                        return "Kết quả lũy thừa có thể quá lớn.";
                    }
                    break;
            }
        } catch (Exception e) {
            logger.debug("Lỗi khi kiểm tra overflow: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Result class for validation.
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final String errorMessage;

        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return isValid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

