package com.calculator.calculatorguiwithgrpc.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorInputValidatorTest {

    private CalculatorInputValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CalculatorInputValidator();
    }

    @Test
    void validateInputs_withValidAddition_shouldPass() {
        CalculatorInputValidator.ValidationResult result =
                validator.validateInputs(10, 5, "+");

        assertTrue(result.isValid(), "Expected validation to pass for valid addition");
        assertNull(result.getErrorMessage());
    }

    @Test
    void validateInputs_withInvalidOperator_shouldFail() {
        CalculatorInputValidator.ValidationResult result =
                validator.validateInputs(10, 5, "#");

        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Toán tử không hợp lệ"));
    }

    @Test
    void validateInputs_divisionByZero_shouldFail() {
        CalculatorInputValidator.ValidationResult result =
                validator.validateInputs(10, 0, "/");

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("chia cho 0"));
    }

    @Test
    void validateInputs_moduloByZero_shouldFail() {
        CalculatorInputValidator.ValidationResult result =
                validator.validateInputs(10, 0, "%");

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("phần dư"));
    }

    @Test
    void validateInputs_negativeBaseWithFractionalExponent_shouldFail() {
        CalculatorInputValidator.ValidationResult result =
                validator.validateInputs(-4, 0.5, "^");

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("cơ số âm"));
    }

    @Test
    void validateInputs_operandTooLarge_shouldFail() {
        CalculatorInputValidator.ValidationResult result =
                validator.validateInputs(1e20, 5, "+");

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("quá lớn"));
    }
}

