package com.calculator.calculatorguiwithgrpc.gui;

import com.calculator.calculatorguiwithgrpc.client.CalculatorClient;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller cho Calculator FXML
 * Xử lý các sự kiện và tương tác của giao diện người dùng
 *
 * @author Nhóm Máy tính
 * @version 1.0
 */
public class CalculatorController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(CalculatorController.class);

    // Các thành phần FXML được tiêm vào
    @FXML private TextField displayField;
    @FXML private ToggleButton modeToggle;
    @FXML private Label statusLabel;

    // Các thành phần của Máy tính
    private CalculatorClient calculatorClient;

    // Trạng thái máy tính
    private String currentInput = "0";
    private String operator = "";
    private double firstOperand = 0;
    private boolean waitingForOperand = false;
    private boolean advancedMode = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Khởi tạo Controller cho Máy tính");

        // Khởi tạo các thành phần
        calculatorClient = new CalculatorClient();

        // Kiểm tra tình trạng của máy chủ
        if (!calculatorClient.isServerHealthy()) {
            updateStatus("Lỗi Máy chủ - Kiểm tra kết nối");
            logger.error("Không thể kết nối đến Máy chủ Máy tính");
        } else {
            updateStatus("Sẵn sàng");
        }

        // Thiết lập màn hình hiển thị
        displayField.setText(currentInput);
    }

    /**
     * Xử lý khi người dùng nhấn nút số
     */
    @FXML
    private void handleNumber(javafx.event.ActionEvent event) {
        Button button = (Button) event.getSource();
        String number = button.getText();

        logger.info("Nút số đã được nhấn: {}", number);

        if (waitingForOperand) {
            currentInput = number;
            waitingForOperand = false;
        } else {
            currentInput = currentInput.equals("0") ? number : currentInput + number;
        }

        updateDisplay();
    }

    /**
     * Xử lý khi người dùng nhấn nút toán tử
     */
    @FXML
    private void handleOperator(javafx.event.ActionEvent event) {
        Button button = (Button) event.getSource();
        String op = button.getText();

        logger.info("Nút toán tử đã được nhấn: {}", op);

        // Chuyển đổi toán tử hiển thị thành toán tử gRPC
        String grpcOperator = convertToGrpcOperator(op);

        if (!operator.isEmpty() && !waitingForOperand) {
            handleEquals(); // Tính toán khi có toán tử và không chờ toán hạng
        }

        try {
            firstOperand = Double.parseDouble(currentInput);
            operator = grpcOperator;
            waitingForOperand = true;
            updateStatus("Nhập số thứ hai");
        } catch (NumberFormatException e) {
            logger.error("Định dạng số không hợp lệ", e);
            updateStatus("Đầu vào không hợp lệ");
        }
    }

    /**
     * Xử lý khi người dùng nhấn nút dấu chấm thập phân
     */
    @FXML
    private void handleDecimal(javafx.event.ActionEvent event) {
        logger.info("Nút dấu chấm thập phân đã được nhấn");

        if (waitingForOperand) {
            currentInput = "0.";
            waitingForOperand = false;
        } else if (!currentInput.contains(".")) {
            currentInput += ".";
        }

        updateDisplay();
    }

    /**
     * Xử lý khi người dùng nhấn nút "="
     */
    @FXML
    private void handleEquals(javafx.event.ActionEvent event) {
        handleEquals(); // Gọi phương thức tính toán
    }

    /**
     * Xử lý phép tính khi người dùng nhấn "="
     */
    private void handleEquals() {
        if (operator.isEmpty() || waitingForOperand) {
            return;
        }

        try {
            double secondOperand = Double.parseDouble(currentInput);

            logger.info("Thực hiện phép tính: {} {} {}", firstOperand, operator, secondOperand);
            updateStatus("Đang tính...");

            // Thực hiện phép tính sử dụng gRPC
            CalculatorClient.CalculationResult result = calculatorClient.performCalculation(firstOperand, secondOperand, operator);

            if (result.isSuccess()) {
                currentInput = formatResult(result.getResult());
                operator = "";
                waitingForOperand = true;
                updateDisplay();
                updateStatus("Sẵn sàng");
                logger.info("Tính toán thành công: {} {} {} = {}", firstOperand, operator, secondOperand, result.getResult());
            } else {
                showAlert("Lỗi Tính toán", result.getErrorMessage());
                updateStatus("Lỗi");
                logger.error("Tính toán thất bại: {}", result.getErrorMessage());
            }

        } catch (NumberFormatException e) {
            showAlert("Lỗi Đầu vào", "Định dạng số không hợp lệ");
            updateStatus("Lỗi");
            logger.error("Định dạng số không hợp lệ", e);
        }
    }

    /**
     * Xử lý khi người dùng nhấn nút C (Clear)
     */
    @FXML
    private void handleClear(javafx.event.ActionEvent event) {
        clearAll();
    }

    /**
     * Xóa toàn bộ trạng thái của máy tính
     */
    private void clearAll() {
        logger.info("Xóa máy tính");
        currentInput = "0";
        operator = "";
        firstOperand = 0;
        waitingForOperand = false;
        updateDisplay();
        updateStatus("Sẵn sàng");
    }

    /**
     * Xử lý khi người dùng nhấn nút thay đổi dấu
     */
    @FXML
    private void handleSignChange(javafx.event.ActionEvent event) {
        logger.info("Nút thay đổi dấu đã được nhấn");

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
     * Xử lý khi người dùng thay đổi chế độ
     */
    @FXML
    private void handleModeToggle(javafx.event.ActionEvent event) {
        advancedMode = modeToggle.isSelected();
        modeToggle.setText(advancedMode ? "Chế độ Nâng cao" : "Chế độ Cơ bản");
        clearAll();
        updateStatus(advancedMode ? "Chế độ Nâng cao đã kích hoạt" : "Chế độ Cơ bản đang hoạt động");
        logger.info("Chế độ đã thay đổi: {}", advancedMode ? "Nâng cao" : "Cơ bản");
    }

    /**
     * Chuyển đổi toán tử hiển thị thành toán tử gRPC
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
     * Cập nhật màn hình hiển thị
     */
    private void updateDisplay() {
        displayField.setText(currentInput);
    }

    /**
     * Cập nhật trạng thái của máy tính
     */
    private void updateStatus(String status) {
        statusLabel.setText(status);
    }

    /**
     * Định dạng kết quả cho hiển thị
     */
    private String formatResult(double result) {
        if (result == (long) result) {
            return String.valueOf((long) result);
        } else {
            return String.format("%.10g", result);
        }
    }

    /**
     * Hiển thị hộp thoại cảnh báo
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Dọn dẹp tài nguyên
     */
    public void cleanup() {
        if (calculatorClient != null) {
            calculatorClient.shutdown();
        }
    }
}
