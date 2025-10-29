package com.calculator.calculatorguiwithgrpc.gui;

import com.calculator.calculatorguiwithgrpc.client.CalculatorClient;
import com.calculator.calculatorguiwithgrpc.utils.ValidationUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculator GUI Application using JavaFX
 * Provides a graphical interface for the Calculator gRPC service
 * 
 * @author Calculator Team
 * @version 1.0
 */
public class CalculatorGUI extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(CalculatorGUI.class);
    
    // UI Components
    private TextField displayField;
    private CalculatorClient calculatorClient;
    private ValidationUtils validationUtils;
    
    // Calculator state
    private String currentInput = "";
    private String operator = "";
    private double firstOperand = 0;
    private boolean waitingForOperand = false;
    private boolean advancedMode = false;
    
    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting Calculator GUI Application");
        
        // Initialize components
        calculatorClient = new CalculatorClient();
        validationUtils = new ValidationUtils();
        
        // Check server health
        if (!calculatorClient.isServerHealthy()) {
            showAlert("Server Error", "Cannot connect to Calculator Server. Please ensure the server is running.");
            logger.error("Failed to connect to Calculator Server");
        }
        
        // Create UI
        Scene scene = createCalculatorScene();
        
        // Setup stage
        primaryStage.setTitle("Calculator GUI with gRPC");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(e -> {
            logger.info("Closing Calculator GUI Application");
            if (calculatorClient != null) {
                calculatorClient.shutdown();
            }
            Platform.exit();
        });
        
        primaryStage.show();
        logger.info("Calculator GUI Application started successfully");
    }
    
    /**
     * Create the main calculator scene
     */
    private Scene createCalculatorScene() {
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setStyle("-fx-background-color: #f0f0f0;");
        
        // Title
        Label titleLabel = new Label("Calculator GUI with gRPC");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.DARKBLUE);
        mainLayout.getChildren().add(titleLabel);
        
        // Display field
        displayField = new TextField("0");
        displayField.setEditable(false);
        displayField.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        displayField.setAlignment(Pos.CENTER_RIGHT);
        displayField.setPrefHeight(60);
        displayField.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-width: 2;");
        mainLayout.getChildren().add(displayField);
        
        // Mode toggle
        ToggleButton modeToggle = new ToggleButton("Basic Mode");
        modeToggle.setSelected(false);
        modeToggle.setOnAction(e -> {
            advancedMode = modeToggle.isSelected();
            modeToggle.setText(advancedMode ? "Advanced Mode" : "Basic Mode");
            clearAll();
        });
        mainLayout.getChildren().add(modeToggle);
        
        // Button grid
        GridPane buttonGrid = createButtonGrid();
        mainLayout.getChildren().add(buttonGrid);
        
        // Status label
        Label statusLabel = new Label("Ready");
        statusLabel.setFont(Font.font("Arial", 12));
        statusLabel.setTextFill(Color.GRAY);
        mainLayout.getChildren().add(statusLabel);
        
        return new Scene(mainLayout, 400, 600);
    }
    
    /**
     * Create the button grid
     */
    private GridPane createButtonGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);
        
        // Button definitions
        String[][] buttonTexts = {
            {"C", "±", "%", "÷"},
            {"7", "8", "9", "×"},
            {"4", "5", "6", "-"},
            {"1", "2", "3", "+"},
            {"0", ".", "="}
        };
        
        // Create buttons
        for (int row = 0; row < buttonTexts.length; row++) {
            for (int col = 0; col < buttonTexts[row].length; col++) {
                String text = buttonTexts[row][col];
                Button button = createButton(text);
                
                // Special handling for "=" button
                if ("=".equals(text)) {
                    GridPane.setColumnSpan(button, 2);
                    button.setStyle("-fx-background-color: #ff6b35; -fx-text-fill: white; -fx-font-weight: bold;");
                }
                
                grid.add(button, col, row);
            }
        }
        
        return grid;
    }
    
    /**
     * Create a calculator button
     */
    private Button createButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(70, 50);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        // Set button styles
        if (isNumber(text) || ".".equals(text)) {
            button.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #ccc;");
        } else if (isOperator(text)) {
            button.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        } else if ("C".equals(text) || "±".equals(text)) {
            button.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        }
        
        // Set button action
        button.setOnAction(e -> handleButtonClick(text));
        
        return button;
    }
    
    /**
     * Handle button click events
     */
    private void handleButtonClick(String text) {
        logger.info("Button clicked: {}", text);
        
        if (isNumber(text)) {
            handleNumberInput(text);
        } else if (isOperator(text)) {
            handleOperatorInput(text);
        } else if (".".equals(text)) {
            handleDecimalInput();
        } else if ("C".equals(text)) {
            clearAll();
        } else if ("±".equals(text)) {
            handleSignChange();
        } else if ("=".equals(text)) {
            handleEquals();
        }
    }
    
    /**
     * Handle number input
     */
    private void handleNumberInput(String number) {
        if (waitingForOperand) {
            currentInput = number;
            waitingForOperand = false;
        } else {
            currentInput = currentInput.equals("0") ? number : currentInput + number;
        }
        updateDisplay();
    }
    
    /**
     * Handle operator input
     */
    private void handleOperatorInput(String op) {
        if (!operator.isEmpty() && !waitingForOperand) {
            handleEquals();
        }
        
        firstOperand = Double.parseDouble(currentInput);
        operator = convertOperatorSymbol(op);
        waitingForOperand = true;
        
        // Display the operator on screen
        currentInput = op;
        updateDisplay();
    }
    
    /**
     * Handle decimal input
     */
    private void handleDecimalInput() {
        if (waitingForOperand) {
            currentInput = "0.";
            waitingForOperand = false;
        } else if (!currentInput.contains(".")) {
            currentInput += ".";
        }
        updateDisplay();
    }
    
    /**
     * Handle equals button
     */
    private void handleEquals() {
        if (operator.isEmpty() || waitingForOperand) {
            return;
        }
        
        try {
            double secondOperand = Double.parseDouble(currentInput);
            String currentOperator = operator; // Store operator before reset
            

            // Thực hiện phép tính sử dụng gRPC
            CalculatorClient.CalculationResult result = calculatorClient.performCalculation(firstOperand, secondOperand, operator);


            if (result.isSuccess()) {
                currentInput = formatResult(result.getResult());
                logger.info("Formatted result: {}", currentInput);
                operator = "";
                waitingForOperand = true;
                updateDisplay();
                logger.info("Calculation successful: {} {} {} = {}", firstOperand, currentOperator, secondOperand, result.getResult());
            } else {
                showAlert("Calculation Error", result.getErrorMessage());
                logger.error("Calculation failed: {}", result.getErrorMessage());
            }
            
        } catch (NumberFormatException e) {
            showAlert("Input Error", "Invalid number format");
            logger.error("Invalid number format", e);
        }
    }
    
    /**
     * Handle sign change
     */
    private void handleSignChange() {
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
     * Clear all
     */
    private void clearAll() {
        currentInput = "0";
        operator = "";
        firstOperand = 0;
        waitingForOperand = false;
        updateDisplay();
    }
    
    /**
     * Update display
     */
    private void updateDisplay() {
        logger.info("Updating display with: {}", currentInput);
        displayField.setText(currentInput);
        logger.info("Display field text after update: {}", displayField.getText());
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
     * Check if text is a number
     */
    private boolean isNumber(String text) {
        return text.matches("[0-9]");
    }
    
    /**
     * Check if text is an operator
     */
    private boolean isOperator(String text) {
        String convertedSymbol = convertOperatorSymbol(text);
        boolean isValid = validationUtils.isValidOperator(convertedSymbol);
        logger.info("Checking operator: '{}' -> '{}' -> {}", text, convertedSymbol, isValid);
        return isValid;
    }
    
    /**
     * Convert GUI operator symbols to gRPC service symbols
     */
    private String convertOperatorSymbol(String guiSymbol) {
        switch (guiSymbol) {
            case "×":
                return "*";
            case "÷":
                return "/";
            case "+":
            case "-":
            case "%":
            case "^":
                return guiSymbol;
            default:
                return guiSymbol;
        }
    }
    
    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * Main method
     */
    public static void main(String[] args) {
        logger.info("Launching Calculator GUI Application");
        launch(args);
    }
}
