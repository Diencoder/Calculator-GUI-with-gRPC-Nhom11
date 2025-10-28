package com.calculator.calculatorguiwithgrpc.utils;

import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * Utility class for input validation
 * Provides validation methods for calculator operations
 * 
 * @author Calculator Team
 * @version 1.0
 */
public class ValidationUtils {
    
    // Valid operators for basic calculations
    private static final Set<String> BASIC_OPERATORS = Set.of("+", "-", "*", "/", "%", "^");
    
    // Valid operators for advanced calculations
    private static final Set<String> ADVANCED_OPERATORS = Set.of(
        "+", "-", "*", "/", "%", "^", 
        "sqrt", "log", "sin", "cos", "tan", 
        "abs", "ceil", "floor", "round", "max", "min"
    );
    
    // Pattern for valid numbers (including decimals and scientific notation)
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?$");
    
    // Pattern for valid request IDs
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9-_]+$");
    
    /**
     * Validate if the operator is valid for basic calculations
     */
    public boolean isValidOperator(String operator) {
        if (operator == null || operator.trim().isEmpty()) {
            return false;
        }
        return BASIC_OPERATORS.contains(operator.trim());
    }
    
    /**
     * Validate if the operator is valid for advanced calculations
     */
    public boolean isValidAdvancedOperator(String operator) {
        if (operator == null || operator.trim().isEmpty()) {
            return false;
        }
        return ADVANCED_OPERATORS.contains(operator.trim());
    }
    
    /**
     * Validate if the input is a valid number
     */
    public boolean isValidNumber(double number) {
        return !Double.isNaN(number) && !Double.isInfinite(number);
    }
    
    /**
     * Validate if the input string represents a valid number
     */
    public boolean isValidNumberString(String numberStr) {
        if (numberStr == null || numberStr.trim().isEmpty()) {
            return false;
        }
        
        try {
            double number = Double.parseDouble(numberStr.trim());
            return isValidNumber(number);
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Validate if the request ID is valid
     */
    public boolean isValidRequestId(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return false;
        }
        return REQUEST_ID_PATTERN.matcher(requestId.trim()).matches();
    }
    
    /**
     * Validate calculation inputs
     */
    public ValidationResult validateCalculationInputs(double operand1, double operand2, String operator) {
        StringBuilder errors = new StringBuilder();
        
        // Validate operands
        if (!isValidNumber(operand1)) {
            errors.append("Invalid operand1: ").append(operand1).append("; ");
        }
        
        if (!isValidNumber(operand2)) {
            errors.append("Invalid operand2: ").append(operand2).append("; ");
        }
        
        // Validate operator
        if (!isValidOperator(operator)) {
            errors.append("Invalid operator: ").append(operator).append("; ");
        }
        
        // Check for division by zero
        if ("/".equals(operator) && operand2 == 0) {
            errors.append("Division by zero is not allowed; ");
        }
        
        // Check for modulo by zero
        if ("%".equals(operator) && operand2 == 0) {
            errors.append("Modulo by zero is not allowed; ");
        }
        
        boolean isValid = errors.length() == 0;
        String errorMessage = isValid ? null : errors.toString();
        
        return new ValidationResult(isValid, errorMessage);
    }
    
    /**
     * Validate advanced calculation inputs
     */
    public ValidationResult validateAdvancedCalculationInputs(double operand1, double operand2, String operator) {
        StringBuilder errors = new StringBuilder();
        
        // Validate operands
        if (!isValidNumber(operand1)) {
            errors.append("Invalid operand1: ").append(operand1).append("; ");
        }
        
        if (!isValidNumber(operand2)) {
            errors.append("Invalid operand2: ").append(operand2).append("; ");
        }
        
        // Validate operator
        if (!isValidAdvancedOperator(operator)) {
            errors.append("Invalid advanced operator: ").append(operator).append("; ");
        }
        
        // Check for division by zero
        if ("/".equals(operator) && operand2 == 0) {
            errors.append("Division by zero is not allowed; ");
        }
        
        // Check for modulo by zero
        if ("%".equals(operator) && operand2 == 0) {
            errors.append("Modulo by zero is not allowed; ");
        }
        
        // Check for negative numbers in sqrt and log
        if ("sqrt".equals(operator) && operand1 < 0) {
            errors.append("Square root of negative number is not allowed; ");
        }
        
        if ("log".equals(operator) && operand1 <= 0) {
            errors.append("Logarithm of non-positive number is not allowed; ");
        }
        
        boolean isValid = errors.length() == 0;
        String errorMessage = isValid ? null : errors.toString();
        
        return new ValidationResult(isValid, errorMessage);
    }
    
    /**
     * Sanitize input string
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replaceAll("[^0-9.+-eE]", "");
    }
    
    /**
     * Get all valid basic operators
     */
    public Set<String> getBasicOperators() {
        return new HashSet<>(BASIC_OPERATORS);
    }
    
    /**
     * Get all valid advanced operators
     */
    public Set<String> getAdvancedOperators() {
        return new HashSet<>(ADVANCED_OPERATORS);
    }
    
    /**
     * Result class for validation operations
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
        
        @Override
        public String toString() {
            if (isValid) {
                return "ValidationResult{valid=true}";
            } else {
                return "ValidationResult{valid=false, error='" + errorMessage + "'}";
            }
        }
    }
}
