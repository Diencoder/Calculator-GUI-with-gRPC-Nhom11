package com.calculator.calculatorguiwithgrpc.gui;

import com.calculator.calculatorguiwithgrpc.client.CalculatorClient;
import com.calculator.calculatorguiwithgrpc.utils.ValidationUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for Calculator FXML
 * Handles UI events and interactions
 * 
 * @author Calculator Team
 * @version 1.0
 */
public class CalculatorController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(CalculatorController.class);
    
    // FXML injected components
    @FXML private TextField displayField;
    @FXML private ToggleButton modeToggle;
    @FXML private Label statusLabel;
    
    // Calculator components
    private CalculatorClient calculatorClient;
    private ValidationUtils validationUtils;
    
    // Calculator state
    private String currentInput = "0";
    private String operator = "";
    private double firstOperand = 0;
    private boolean waitingForOperand = false;
    private boolean advancedMode = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Calculator Controller");
        
        // Initialize components
        calculatorClient = new CalculatorClient();
        validationUtils = new ValidationUtils();
        
        // Check server health
        if (!calculatorClient.isServerHealthy()) {
            updateStatus("Server Error - Check connection");
            logger.error("Failed to connect to Calculator Server");
        } else {
            updateStatus("Ready");
        }
        
        // Setup display
        displayField.setText(currentInput);
    }
    
    /**
     * Handle number button clicks
     */
    @FXML
    private void handleNumber(javafx.event.ActionEvent event) {
        Button button = (Button) event.getSource();
        String number = button.getText();
        
        logger.info("Number button clicked: {}", number);
        
        if (waitingForOperand) {
            currentInput = number;
            waitingForOperand = false;
        } else {
            currentInput = currentInput.equals("0") ? number : currentInput + number;
        }
        
        updateDisplay();
    }
    
    /**
     * Handle operator button clicks
     */
    @FXML
    private void handleOperator(javafx.event.ActionEvent event) {
        Button button = (Button) event.getSource();
        String op = button.getText();
        
        logger.info("Operator button clicked: {}", op);
        
        // Convert display operator to gRPC operator
        String grpcOperator = convertToGrpcOperator(op);
        
        if (!operator.isEmpty() && !waitingForOperand) {
            handleEquals();
        }
        
        try {
            firstOperand = Double.parseDouble(currentInput);
            operator = grpcOperator;
            waitingForOperand = true;
            updateStatus("Enter second number");
        } catch (NumberFormatException e) {
            logger.error("Invalid number format", e);
            updateStatus("Invalid input");
        }
    }
    
    /**
     * Handle decimal button click
     */
    @FXML
    private void handleDecimal(javafx.event.ActionEvent event) {
        logger.info("Decimal button clicked");
        
        if (waitingForOperand) {
            currentInput = "0.";
            waitingForOperand = false;
        } else if (!currentInput.contains(".")) {
            currentInput += ".";
        }
        
        updateDisplay();
    }
    
    /**
     * Handle equals button click
     */
    @FXML
    private void handleEquals(javafx.event.ActionEvent event) {
        handleEquals();
    }
    
    /**
     * Handle equals calculation
     */
    private void handleEquals() {
        if (operator.isEmpty() || waitingForOperand) {
            return;
        }
        
        try {
            double secondOperand = Double.parseDouble(currentInput);
            
            logger.info("Performing calculation: {} {} {}", firstOperand, operator, secondOperand);
            updateStatus("Calculating...");
            
            // Perform calculation using gRPC
            CalculatorClient.CalculationResult result = calculatorClient.calculate(firstOperand, secondOperand, operator);
            
            if (result.isSuccess()) {
                currentInput = formatResult(result.getResult());
                operator = "";
                waitingForOperand = true;
                updateDisplay();
                updateStatus("Ready");
                logger.info("Calculation successful: {} {} {} = {}", firstOperand, operator, secondOperand, result.getResult());
            } else {
                showAlert("Calculation Error", result.getErrorMessage());
                updateStatus("Error");
                logger.error("Calculation failed: {}", result.getErrorMessage());
            }
            
        } catch (NumberFormatException e) {
            showAlert("Input Error", "Invalid number format");
            updateStatus("Error");
            logger.error("Invalid number format", e);
        }
    }
    
    /**
     * Handle clear button click
     */
    @FXML
    private void handleClear(javafx.event.ActionEvent event) {
        clearAll();
    }
    
    /**
     * Clear all calculator state
     */
    private void clearAll() {
        logger.info("Clearing calculator");
        currentInput = "0";
        operator = "";
        firstOperand = 0;
        waitingForOperand = false;
        updateDisplay();
        updateStatus("Ready");
    }
    
    /**
     * Handle sign change button click
     */
    @FXML
    private void handleSignChange(javafx.event.ActionEvent event) {
        logger.info("Sign change button clicked");
        
        if (!currentInput.equals("0")) {
            if (currentInput.startsWith("-")) {
                currentInput = currentInput.substring(1);
            } else {
                currentInput = "-" + currentInput;
            }
            updateDisplay();
        }
    }
    
    /**
     * Handle mode toggle
     */
    @FXML
    private void handleModeToggle(javafx.event.ActionEvent event) {
        advancedMode = modeToggle.isSelected();
        modeToggle.setText(advancedMode ? "Advanced Mode" : "Basic Mode");
        clearAll();
        updateStatus(advancedMode ? "Advanced Mode Active" : "Basic Mode Active");
        logger.info("Mode changed to: {}", advancedMode ? "Advanced" : "Basic");
    }
    
    /**
     * Convert display operator to gRPC operator
     */
    private String convertToGrpcOperator(String displayOperator) {
        return switch (displayOperator) {
            case "÷" -> "/";
            case "×" -> "*";
            case "+" -> "+";
            case "-" -> "-";
            case "%" -> "%";
            default -> displayOperator;
        };
    }
    
    /**
     * Update display field
     */
    private void updateDisplay() {
        displayField.setText(currentInput);
    }
    
    /**
     * Update status label
     */
    private void updateStatus(String status) {
        statusLabel.setText(status);
    }
    
    /**
     * Format result for display
     */
    private String formatResult(double result) {
        if (result == (long) result) {
            return String.valueOf((long) result);
        } else {
            return String.format("%.10g", result);
        }
    }
    
    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (calculatorClient != null) {
            calculatorClient.shutdown();
        }
    }
}
