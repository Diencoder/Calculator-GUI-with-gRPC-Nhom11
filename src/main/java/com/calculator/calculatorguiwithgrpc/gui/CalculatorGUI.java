package com.calculator.calculatorguiwithgrpc.gui;

import com.calculator.calculatorguiwithgrpc.client.CalculatorClient;
import com.calculator.calculatorguiwithgrpc.utils.ValidationUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

    private TextField displayField; // Màn hình hiển thị chính
    private Label expressionLabel; // Hiển thị biểu thức (ví dụ: "8 × 9 =")
    private Label statusLabel;
    private ListView<String> historyListView;
    private CalculatorClient calculatorClient;
    private ValidationUtils validationUtils;

    private String currentInput = "0";
    private String operator = "";
    private String expression = ""; // Biểu thức hiện tại
    private double firstOperand = 0;
    private boolean waitingForOperand = false;
    private boolean advancedMode = false;
    private ObservableList<String> calculationHistory = FXCollections.observableArrayList();

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting Calculator GUI Application");

        calculatorClient = new CalculatorClient();
        validationUtils = new ValidationUtils();

        if (!calculatorClient.isServerHealthy()) {
            showError("Lỗi kết nối Server", "Không thể kết nối đến Calculator Server. Vui lòng đảm bảo server đang chạy.");
            logger.error("Failed to connect to Calculator Server");
        }

        Scene scene = createCalculatorScene();
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
        // Layout chính: BorderPane để có display ở trên, bàn phím ở giữa, lịch sử bên phải
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(15));
        mainLayout.getStyleClass().add("root");

        // === PHẦN BÊN TRÁI: Display và Bàn phím ===
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));

        // Title
        Label titleLabel = new Label("Máy tính với gRPC");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.DARKBLUE);
        leftPanel.getChildren().add(titleLabel);

        // Display Section - Giống máy tính Microsoft
        VBox displaySection = new VBox(5);
        displaySection.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 5; -fx-padding: 15;");
        
        // Expression label (hiển thị biểu thức như "8 × 9 =")
        expressionLabel = new Label("");
        expressionLabel.setFont(Font.font("Arial", 16));
        expressionLabel.setTextFill(Color.GRAY);
        expressionLabel.setAlignment(Pos.CENTER_RIGHT);
        expressionLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(expressionLabel, Priority.ALWAYS);
        
        // Display field - Màn hình hiển thị chính
        displayField = new TextField("0");
        displayField.setEditable(false);
        displayField.setAlignment(Pos.CENTER_RIGHT);
        displayField.setPrefHeight(80);
        displayField.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        displayField.setStyle("-fx-background-color: #1e1e1e; -fx-text-fill: white; -fx-border-width: 0;");
        displayField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(displayField, Priority.ALWAYS);
        
        displaySection.getChildren().addAll(expressionLabel, displayField);
        leftPanel.getChildren().add(displaySection);

        // Bàn phím số và phép toán
        GridPane calculatorPad = createCalculatorPad();
        leftPanel.getChildren().add(calculatorPad);

        // Status label
        statusLabel = new Label("Sẵn sàng");
        statusLabel.setFont(Font.font("Arial", 11));
        statusLabel.setTextFill(Color.GRAY);
        leftPanel.getChildren().add(statusLabel);

        // === PHẦN BÊN PHẢI: Lịch sử ===
        VBox rightPanel = new VBox(10);
        rightPanel.setPrefWidth(250);
        rightPanel.setPadding(new Insets(10));
        
        Label historyLabel = new Label("Lịch sử");
        historyLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        historyLabel.setTextFill(Color.DARKSLATEGRAY);
        
        historyListView = new ListView<>(calculationHistory);
        historyListView.setPrefHeight(600);
        historyListView.getStyleClass().add("list-view");
        historyListView.setStyle("-fx-font-size: 14px;");
        
        Button clearHistoryButton = new Button("Xóa lịch sử");
        clearHistoryButton.setPrefWidth(Double.MAX_VALUE);
        clearHistoryButton.setPrefHeight(35);
        clearHistoryButton.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        clearHistoryButton.getStyleClass().add("button");
        clearHistoryButton.getStyleClass().add("button-clear");
        clearHistoryButton.setOnAction(e -> clearHistory());
        
        rightPanel.getChildren().addAll(historyLabel, historyListView, clearHistoryButton);

        // Gắn các panel vào BorderPane
        mainLayout.setLeft(leftPanel);
        mainLayout.setRight(rightPanel);
        BorderPane.setMargin(rightPanel, new Insets(0, 0, 0, 15));

        return new Scene(mainLayout, 800, 700);
    }

    /**
     * Tạo bàn phím máy tính đầy đủ (giống Microsoft Calculator)
     */
    private GridPane createCalculatorPad() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setAlignment(Pos.CENTER);

        // Hàng 1: %, CE, C, ⌫
        grid.add(createFunctionButton("%"), 0, 0);
        grid.add(createFunctionButton("CE"), 1, 0);
        grid.add(createFunctionButton("C"), 2, 0);
        grid.add(createFunctionButton("⌫"), 3, 0);

        // Hàng 2: 1/x, x², √x, ÷
        grid.add(createFunctionButton("1/x"), 0, 1);
        grid.add(createFunctionButton("x²"), 1, 1);
        grid.add(createFunctionButton("√x"), 2, 1);
        grid.add(createOperatorButton("÷"), 3, 1);

        // Hàng 3: 7, 8, 9, ×
        grid.add(createNumberButton("7"), 0, 2);
        grid.add(createNumberButton("8"), 1, 2);
        grid.add(createNumberButton("9"), 2, 2);
        grid.add(createOperatorButton("×"), 3, 2);

        // Hàng 4: 4, 5, 6, -
        grid.add(createNumberButton("4"), 0, 3);
        grid.add(createNumberButton("5"), 1, 3);
        grid.add(createNumberButton("6"), 2, 3);
        grid.add(createOperatorButton("-"), 3, 3);

        // Hàng 5: 1, 2, 3, +
        grid.add(createNumberButton("1"), 0, 4);
        grid.add(createNumberButton("2"), 1, 4);
        grid.add(createNumberButton("3"), 2, 4);
        grid.add(createOperatorButton("+"), 3, 4);

        // Hàng 6: ±, 0, ., =
        grid.add(createFunctionButton("±"), 0, 5);
        grid.add(createNumberButton("0"), 1, 5);
        grid.add(createNumberButton("."), 2, 5);
        Button btnEquals = createOperatorButton("=");
        btnEquals.getStyleClass().add("button-equal");
        btnEquals.setOnAction(e -> handleEquals());
        grid.add(btnEquals, 3, 5);

        return grid;
    }

    /**
     * Tạo nút số
     */
    private Button createNumberButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(70, 55);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        button.getStyleClass().add("button");
        button.getStyleClass().add("button-number");
        
        button.setOnAction(e -> handleNumberInput(text));
        return button;
    }

    /**
     * Tạo nút phép toán
     */
    private Button createOperatorButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(70, 55);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        button.getStyleClass().add("button");
        button.getStyleClass().add("button-operator");
        
        button.setOnAction(e -> handleOperatorInput(text));
        return button;
    }

    /**
     * Tạo nút chức năng
     */
    private Button createFunctionButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(70, 55);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        button.getStyleClass().add("button");
        button.getStyleClass().add("button-clear");
        
        switch (text) {
            case "C":
                button.setOnAction(e -> handleClearAll());
                break;
            case "CE":
                button.setOnAction(e -> handleClearEntry());
                break;
            case "±":
                button.setOnAction(e -> handleSignChange());
                break;
            case "⌫":
                button.setOnAction(e -> handleBackspace());
                break;
            case "%":
                button.setOnAction(e -> handleOperatorInput("%"));
                break;
            case "1/x":
                button.setOnAction(e -> handleReciprocal());
                break;
            case "x²":
                button.setOnAction(e -> handleSquare());
                break;
            case "√x":
                button.setOnAction(e -> handleSquareRoot());
                break;
        }
        return button;
    }

    /**
     * Xử lý nhập số
     */
    private void handleNumberInput(String input) {
        if (".".equals(input)) {
            // Xử lý dấu chấm
            if (waitingForOperand) {
                currentInput = "0.";
                waitingForOperand = false;
            } else if (!currentInput.contains(".")) {
                currentInput += ".";
            }
        } else {
            // Xử lý số
            if (waitingForOperand) {
                currentInput = input;
                waitingForOperand = false;
            } else {
                if (currentInput.equals("0")) {
                    currentInput = input;
                } else {
                    currentInput += input;
                }
            }
        }
        updateDisplay();
    }

    /**
     * Xử lý nhập phép toán
     */
    private void handleOperatorInput(String op) {
        if (!operator.isEmpty() && !waitingForOperand) {
            // Nếu đã có phép toán, tính kết quả trước
            handleEquals();
        }
        
        if (!waitingForOperand) {
            try {
                firstOperand = Double.parseDouble(currentInput);
                operator = convertOperatorSymbol(op);
                waitingForOperand = true;
                expression = formatNumber(firstOperand) + " " + op;
                expressionLabel.setText(expression);
            } catch (NumberFormatException e) {
                showError("Lỗi", "Số không hợp lệ");
            }
        }
    }

    /**
     * Xóa tất cả (C)
     */
    private void handleClearAll() {
        clearAll();
    }

    /**
     * Xóa entry hiện tại (CE)
     */
    private void handleClearEntry() {
        currentInput = "0";
        updateDisplay();
    }

    /**
     * Đổi dấu
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
     * Xóa ký tự cuối cùng
     */
    private void handleBackspace() {
        if (!currentInput.equals("0") && currentInput.length() > 1) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
        } else {
            currentInput = "0";
        }
        updateDisplay();
    }

    /**
     * Nghịch đảo (1/x)
     */
    private void handleReciprocal() {
        try {
            double value = Double.parseDouble(currentInput);
            if (value == 0) {
                showError("Lỗi", "Không thể chia cho 0");
                return;
            }
            performCalculation(value, 1.0, "/");
        } catch (NumberFormatException e) {
            showError("Lỗi", "Số không hợp lệ");
        }
    }

    /**
     * Bình phương (x²)
     */
    private void handleSquare() {
        try {
            double value = Double.parseDouble(currentInput);
            performCalculation(value, value, "*");
        } catch (NumberFormatException e) {
            showError("Lỗi", "Số không hợp lệ");
        }
    }

    /**
     * Căn bậc hai (√x)
     */
    private void handleSquareRoot() {
        try {
            double value = Double.parseDouble(currentInput);
            if (value < 0) {
                showError("Lỗi", "Không thể tính căn số âm");
                return;
            }
            double result = Math.sqrt(value);
            currentInput = formatResult(result);
            expression = "√(" + formatNumber(value) + ") =";
            expressionLabel.setText(expression);
            updateDisplay();
            addToHistory(expression + " " + currentInput);
        } catch (NumberFormatException e) {
            showError("Lỗi", "Số không hợp lệ");
        }
    }

    /**
     * Thực hiện tính toán
     */
    private void performCalculation(double op1, double op2, String op) {
        updateStatus("Đang tính toán...", Color.BLUE);
        
        new Thread(() -> {
            try {
                CalculatorClient.CalculationResult result = calculatorClient.calculate(op1, op2, op);
                
                Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        currentInput = formatResult(result.getResult());
                        expression = formatNumber(op1) + " " + getOperatorSymbol(op) + " " + formatNumber(op2) + " =";
                        expressionLabel.setText(expression);
                        updateDisplay();
                        addToHistory(expression + " " + currentInput);
                        updateStatus("Tính toán thành công!", Color.GREEN);
                    } else {
                        showError("Lỗi tính toán", result.getErrorMessage());
                        updateStatus("Lỗi: " + result.getErrorMessage(), Color.RED);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Lỗi kết nối", "Không thể kết nối đến server");
                    updateStatus("Lỗi kết nối", Color.RED);
                });
            }
        }).start();
    }

    /**
     * Thêm vào lịch sử
     */
    private void addToHistory(String entry) {
        calculationHistory.add(0, entry);
        if (calculationHistory.size() > 100) {
            calculationHistory.remove(calculationHistory.size() - 1);
        }
    }



    /**
     * Xử lý nút bằng (=)
     */
    private void handleEquals() {
        if (operator.isEmpty() || waitingForOperand) return;

        try {
            double secondOperand = Double.parseDouble(currentInput);
            
            performCalculation(firstOperand, secondOperand, operator);
            
            operator = "";
            waitingForOperand = true;
        } catch (NumberFormatException e) {
            showError("Lỗi", "Số không hợp lệ");
        }
    }


    private void clearAll() {
        currentInput = "0";
        operator = "";
        expression = "";
        firstOperand = 0;
        waitingForOperand = false;
        
        updateDisplay();
        if (expressionLabel != null) {
            expressionLabel.setText("");
        }
        
        updateStatus("Sẵn sàng", Color.GRAY);
    }

    private void clearHistory() {
        calculationHistory.clear();
        historyListView.setItems(calculationHistory);
        updateStatus("Đã xóa lịch sử", Color.GRAY);
        logger.info("Calculator history cleared");
    }

    private void updateDisplay() {
        if (displayField != null) {
            displayField.setText(currentInput);
        }
    }

    /**
     * Cập nhật trạng thái với màu sắc
     */
    private void updateStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setTextFill(color);
    }

    /**
     * Hiển thị lỗi với alert dialog
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Lấy ký hiệu phép toán để hiển thị
     */
    private String getOperatorSymbol(String operator) {
        return switch (operator) {
            case "*" -> "×";
            case "/" -> "÷";
            default -> operator;
        };
    }

    /**
     * Định dạng số để hiển thị
     */
    private String formatNumber(double number) {
        if (number == (long) number) {
            return String.valueOf((long) number);
        } else {
            return String.format("%.10g", number);
        }
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

    /**
     * Hiển thị thông báo thành công
     */
    private void showSuccess(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Giữ lại phương thức cũ để tương thích
     */
    private void showAlert(String title, String message) {
        showError(title, message);
    }

    public static void main(String[] args) {
        launch(args);
    }
}