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
 * @author Calculator
 * @version 2.0
 */
public class CalculatorGUI extends Application {

    private static final Logger logger = LoggerFactory.getLogger(CalculatorGUI.class);

    private TextField displayField;
    private Label statusLabel;
    private CalculatorClient calculatorClient;
    private ValidationUtils validationUtils;

    private String currentInput = "0";
    private String operator = "";
    private double firstOperand = 0;
    private boolean waitingForOperand = false;
    private boolean advancedMode = false;

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting Calculator GUI Application");

        calculatorClient = new CalculatorClient();
        validationUtils = new ValidationUtils();

        if (!calculatorClient.isServerHealthy()) {
            showAlert("Server Error", "Cannot connect to Calculator Server. Please ensure the server is running.");
            logger.error("Failed to connect to Calculator Server");
        }

        Scene scene = createCalculatorScene();
        // 🟢 Load external CSS
        scene.getStylesheets().add(getClass().getResource("/css/calculator.css").toExternalForm());

        primaryStage.setTitle("Calculator GUI with gRPC");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(e -> {
            if (calculatorClient != null) calculatorClient.shutdown();
            Platform.exit();
        });

        primaryStage.show();
        logger.info("Calculator GUI started successfully");
    }

    private Scene createCalculatorScene() {
        VBox mainLayout = new VBox(12);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.getStyleClass().add("root");

        // Title
        Label titleLabel = new Label("Calculator GUI with gRPC");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.DARKBLUE);
        mainLayout.getChildren().add(titleLabel);

        // Display field
        displayField = new TextField("0");
        displayField.setEditable(false);
        displayField.setAlignment(Pos.CENTER_RIGHT);
        displayField.setPrefHeight(60);
        displayField.getStyleClass().add("text-field");
        mainLayout.getChildren().add(displayField);

        // Mode toggle
        ToggleButton modeToggle = new ToggleButton("Basic Mode");
        modeToggle.setOnAction(e -> {
            advancedMode = modeToggle.isSelected();
            modeToggle.setText(advancedMode ? "Advanced Mode" : "Basic Mode");
            clearAll();
        });
        mainLayout.getChildren().add(modeToggle);

        // Buttons
        GridPane buttonGrid = createButtonGrid();
        mainLayout.getChildren().add(buttonGrid);

        // Status label
        statusLabel = new Label("Ready");
        statusLabel.setFont(Font.font("Arial", 12));
        statusLabel.setTextFill(Color.GRAY);
        mainLayout.getChildren().add(statusLabel);

        return new Scene(mainLayout, 400, 600);
    }

    private GridPane createButtonGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        String[][] buttonTexts = {
                {"C", "±", "%", "÷"},
                {"7", "8", "9", "×"},
                {"4", "5", "6", "-"},
                {"1", "2", "3", "+"},
                {"0", ".", "="}
        };

        for (int row = 0; row < buttonTexts.length; row++) {
            for (int col = 0; col < buttonTexts[row].length; col++) {
                String text = buttonTexts[row][col];
                Button button = createButton(text);

                if ("=".equals(text)) {
                    GridPane.setColumnSpan(button, 2);
                    button.getStyleClass().add("button-equal");
                }
                grid.add(button, col, row);
            }
        }
        return grid;
    }

    private Button createButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(70, 50);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        button.getStyleClass().add("button");

        if (isNumber(text) || ".".equals(text)) {
            button.getStyleClass().add("button-number");
        } else if (isOperator(text)) {
            button.getStyleClass().add("button-operator");
        } else if ("C".equals(text) || "±".equals(text)) {
            button.getStyleClass().add("button-clear");
        }

        button.setOnAction(e -> handleButtonClick(text));
        return button;
    }

    private void handleButtonClick(String text) {
        if (isNumber(text)) handleNumberInput(text);
        else if (isOperator(text)) handleOperatorInput(text);
        else if (".".equals(text)) handleDecimalInput();
        else if ("C".equals(text)) clearAll();
        else if ("±".equals(text)) handleSignChange();
        else if ("=".equals(text)) handleEquals();
    }

    private void handleNumberInput(String number) {
        if (waitingForOperand) {
            currentInput = number;
            waitingForOperand = false;
        } else {
            currentInput = currentInput.equals("0") ? number : currentInput + number;
        }
        updateDisplay();
    }

    private void handleOperatorInput(String op) {
        if (!operator.isEmpty() && !waitingForOperand) handleEquals();

        firstOperand = Double.parseDouble(currentInput);
        operator = convertOperatorSymbol(op);
        waitingForOperand = true;
        currentInput = op;
        updateDisplay();
    }

    private void handleDecimalInput() {
        if (waitingForOperand) {
            currentInput = "0.";
            waitingForOperand = false;
        } else if (!currentInput.contains(".")) {
            currentInput += ".";
        }
        updateDisplay();
    }

    private void handleEquals() {
        if (operator.isEmpty() || waitingForOperand) return;

        double secondOperand = Double.parseDouble(currentInput);
        String currentOperator = operator;

        statusLabel.setText("Calculating...");
        statusLabel.setTextFill(Color.BLUE);

        new Thread(() -> {
            try {
                CalculatorClient.CalculationResult result =
                        calculatorClient.calculate(firstOperand, secondOperand, operator);

                Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        currentInput = formatResult(result.getResult());
                        operator = "";
                        waitingForOperand = true;
                        updateDisplay();
                        statusLabel.setText("Result ready");
                        statusLabel.setTextFill(Color.GREEN);
                    } else {
                        showAlert("Calculation Error", result.getErrorMessage());
                        statusLabel.setText("Error: " + result.getErrorMessage());
                        statusLabel.setTextFill(Color.RED);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Network Error", "Server not responding");
                    statusLabel.setText("Connection failed");
                    statusLabel.setTextFill(Color.RED);
                });
            }
        }).start();
    }

    private void handleSignChange() {
        if (!currentInput.equals("0")) {
            if (currentInput.startsWith("-"))
                currentInput = currentInput.substring(1);
            else
                currentInput = "-" + currentInput;
            updateDisplay();
        }
    }

    private void clearAll() {
        currentInput = "0";
        operator = "";
        firstOperand = 0;
        waitingForOperand = false;
        updateDisplay();
        statusLabel.setText("Ready");
        statusLabel.setTextFill(Color.GRAY);
    }

    private void updateDisplay() {
        displayField.setText(currentInput);
    }

    private String formatResult(double result) {
        if (result == (long) result) {
            return String.valueOf((long) result);
        } else {
            return String.format("%.10g", result);
        }
    }

    private boolean isNumber(String text) {
        return text.matches("[0-9]");
    }

    private boolean isOperator(String text) {
        String convertedSymbol = convertOperatorSymbol(text);
        return validationUtils.isValidOperator(convertedSymbol);
    }

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

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
