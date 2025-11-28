package com.calculator.calculatorguiwithgrpc.gui;

import com.calculator.calculatorguiwithgrpc.client.CalculatorClient;
import com.calculator.calculatorguiwithgrpc.config.AppConfig;
import com.calculator.calculatorguiwithgrpc.utils.ValidationUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
    private static final AppConfig appConfig = AppConfig.getInstance();

    private TextField displayField; // Màn hình hiển thị chính
    private Label expressionLabel; // Hiển thị biểu thức (ví dụ: "8 × 9 =")
    private Label statusLabel;
    private ListView<String> historyListView;
    private CalculatorClient calculatorClient;
    private ValidationUtils validationUtils;
    private final int maxHistoryEntries = appConfig.getGuiHistoryMaxEntries();

    private String currentInput = "0";
    private String operator = "";
    private String expression = ""; // Biểu thức hiện tại
    private double firstOperand = 0;
    private boolean waitingForOperand = false;
    private String currentMode = "Chuẩn"; // Chuẩn, Khoa học, Lập trình viên
    private ObservableList<String> calculationHistory = FXCollections.observableArrayList();
    
    // Memory functions - Hỗ trợ nhiều memory slots
    private final java.util.Map<Integer, Double> memorySlots = new java.util.HashMap<>();
    private int currentMemorySlot = 0;
    private ListView<String> memoryListView;
    
    private final int windowWidth = appConfig.getGuiWindowWidth();
    private final int windowHeight = appConfig.getGuiWindowHeight();
    private final double buttonWidth = Math.max(70, windowWidth / 12.0);
    private final double buttonHeight = Math.max(55, windowHeight / 10.0);
    
    // UI Components
    private GridPane calculatorPad;
    private VBox modeMenu;

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
        setupKeyboardShortcuts(scene);

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
        // Layout chính: BorderPane với menu bên trái, display và bàn phím ở giữa, lịch sử bên phải
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(0));
        mainLayout.getStyleClass().add("root");
        mainLayout.setStyle("-fx-background-color: #f5f5f5;");

        // === MENU CHẾ ĐỘ BÊN TRÁI ===
        modeMenu = createModeMenu();
        mainLayout.setLeft(modeMenu);

        // === PHẦN GIỮA: Display và Bàn phím ===
        VBox centerPanel = new VBox(0);
        centerPanel.setPadding(new Insets(15));
        centerPanel.setSpacing(12);

        // Display Section - Giống máy tính Microsoft
        VBox displaySection = new VBox(0);
        displaySection.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 0; -fx-padding: 15;");
        
        // Expression label (hiển thị biểu thức như "8 × 9 =")
        expressionLabel = new Label("");
        expressionLabel.setFont(Font.font("Segoe UI", 14));
        expressionLabel.setTextFill(Color.rgb(200, 200, 200));
        expressionLabel.setAlignment(Pos.CENTER_RIGHT);
        expressionLabel.setMaxWidth(Double.MAX_VALUE);
        expressionLabel.setPrefHeight(30);
        HBox.setHgrow(expressionLabel, Priority.ALWAYS);
        
        // Display field - Màn hình hiển thị chính
        displayField = new TextField("0");
        displayField.setEditable(false);
        displayField.setAlignment(Pos.CENTER_RIGHT);
        displayField.setPrefHeight(Math.max(70, windowHeight * 0.12));
        int displayFontSize = appConfig.getGuiDisplayFontSize();
        displayField.setFont(Font.font("Segoe UI", FontWeight.BOLD, displayFontSize));
        displayField.setStyle("-fx-background-color: #1e1e1e; -fx-text-fill: white; -fx-border-width: 0; -fx-padding: 10 0 10 0;");
        displayField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(displayField, Priority.ALWAYS);
        
        displaySection.getChildren().addAll(expressionLabel, displayField);
        centerPanel.getChildren().add(displaySection);

        // Bàn phím số và phép toán
        calculatorPad = createCalculatorPad();
        centerPanel.getChildren().add(calculatorPad);

        // Status label
        statusLabel = new Label("Sẵn sàng");
        statusLabel.setFont(Font.font("Segoe UI", 11));
        statusLabel.setTextFill(Color.GRAY);
        statusLabel.setAlignment(Pos.CENTER);
        centerPanel.getChildren().add(statusLabel);

        // === PHẦN BÊN PHẢI: Tab Lịch sử và Bộ nhớ ===
        VBox rightPanel = createRightPanel();
        int rightPanelWidth = Math.max(220, (int) (windowWidth * 0.22));
        rightPanel.setPrefWidth(rightPanelWidth);
        rightPanel.setStyle("-fx-background-color: white;");

        // Gắn các panel vào BorderPane
        mainLayout.setCenter(centerPanel);
        mainLayout.setRight(rightPanel);

        return new Scene(mainLayout, windowWidth, windowHeight);
    }
    
    /**
     * Tạo panel bên phải với tab Lịch sử và Bộ nhớ
     */
    private VBox createRightPanel() {
        VBox panel = new VBox(0);
        panel.setPadding(new Insets(0));
        
        // Tab bar
        HBox tabBar = new HBox(0);
        tabBar.setStyle("-fx-background-color: #f0f0f0;");
        
        ToggleButton historyTab = new ToggleButton("Lịch sử");
        historyTab.setSelected(true);
        historyTab.setPrefWidth(160);
        historyTab.setPrefHeight(40);
        historyTab.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        historyTab.setStyle("-fx-background-color: white; -fx-border-width: 0 0 2 0; -fx-border-color: #0078d4;");
        
        ToggleButton memoryTab = new ToggleButton("Bộ nhớ");
        memoryTab.setPrefWidth(160);
        memoryTab.setPrefHeight(40);
        memoryTab.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        memoryTab.setStyle("-fx-background-color: #f0f0f0;");
        
        ToggleGroup tabGroup = new ToggleGroup();
        historyTab.setToggleGroup(tabGroup);
        memoryTab.setToggleGroup(tabGroup);
        
        historyTab.setOnAction(e -> {
            historyTab.setStyle("-fx-background-color: white; -fx-border-width: 0 0 2 0; -fx-border-color: #0078d4;");
            memoryTab.setStyle("-fx-background-color: #f0f0f0;");
            showHistoryPanel();
        });
        
        memoryTab.setOnAction(e -> {
            memoryTab.setStyle("-fx-background-color: white; -fx-border-width: 0 0 2 0; -fx-border-color: #0078d4;");
            historyTab.setStyle("-fx-background-color: #f0f0f0;");
            showMemoryPanel();
        });
        
        tabBar.getChildren().addAll(historyTab, memoryTab);
        
        // Content area
        StackPane contentArea = new StackPane();
        double contentHeight = Math.max(420, windowHeight - 120);
        contentArea.setPrefHeight(contentHeight);
        contentArea.setStyle("-fx-background-color: white;");
        
        // History panel
        VBox historyPanel = new VBox(10);
        historyPanel.setPadding(new Insets(15));
        historyPanel.setVisible(true);
        
        historyListView = new ListView<>(calculationHistory);
        historyListView.setPrefHeight(Math.max(300, contentHeight - 90));
        historyListView.getStyleClass().add("list-view");
        historyListView.setStyle("-fx-font-size: 13px; -fx-background-color: white;");
        
        Button clearHistoryButton = new Button("Xóa lịch sử");
        clearHistoryButton.setPrefWidth(Double.MAX_VALUE);
        clearHistoryButton.setPrefHeight(35);
        clearHistoryButton.setFont(Font.font("Segoe UI", 12));
        clearHistoryButton.setStyle("-fx-background-color: #e81123; -fx-text-fill: white; -fx-background-radius: 3;");
        clearHistoryButton.setOnAction(e -> clearHistory());
        
        historyPanel.getChildren().addAll(historyListView, clearHistoryButton);
        
        // Memory panel - Cải thiện với danh sách memory slots
        VBox memoryPanel = new VBox(10);
        memoryPanel.setPadding(new Insets(15));
        memoryPanel.setVisible(false);
        
        Label memoryTitleLabel = new Label("Bộ nhớ");
        memoryTitleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        memoryTitleLabel.setTextFill(Color.rgb(0, 120, 212));
        
        // Danh sách memory slots
        ObservableList<String> memoryList = FXCollections.observableArrayList();
        memoryListView = new ListView<>(memoryList);
        memoryListView.setPrefHeight(Math.max(200, contentHeight - 200));
        memoryListView.getStyleClass().add("list-view");
        memoryListView.setStyle("-fx-font-size: 13px; -fx-background-color: white;");
        memoryListView.setCellFactory(listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("-fx-padding: 5px;");
                }
            }
        });
        
        // Memory buttons - Hàng 1
        HBox memoryButtonsRow1 = new HBox(5);
        memoryButtonsRow1.setSpacing(5);
        
        Button msButton = createMemoryButton("MS", () -> handleMemoryStore());
        Button mrButton = createMemoryButton("MR", () -> handleMemoryRecall());
        Button mcButton = createMemoryButton("MC", () -> handleMemoryClear());
        
        memoryButtonsRow1.getChildren().addAll(msButton, mrButton, mcButton);
        
        // Memory buttons - Hàng 2
        HBox memoryButtonsRow2 = new HBox(5);
        memoryButtonsRow2.setSpacing(5);
        
        Button mPlusButton = createMemoryButton("M+", () -> handleMemoryAdd());
        Button mMinusButton = createMemoryButton("M-", () -> handleMemorySubtract());
        Button mSelectButton = createMemoryButton("M", () -> handleMemorySelect());
        
        memoryButtonsRow2.getChildren().addAll(mPlusButton, mMinusButton, mSelectButton);
        
        // Nút xóa tất cả memory
        Button clearAllMemoryButton = new Button("Xóa tất cả");
        clearAllMemoryButton.setPrefWidth(Double.MAX_VALUE);
        clearAllMemoryButton.setPrefHeight(35);
        clearAllMemoryButton.setFont(Font.font("Segoe UI", 12));
        clearAllMemoryButton.setStyle("-fx-background-color: #e81123; -fx-text-fill: white; -fx-background-radius: 3;");
        clearAllMemoryButton.setOnAction(e -> handleClearAllMemory());
        HBox.setHgrow(clearAllMemoryButton, Priority.ALWAYS);
        
        memoryPanel.getChildren().addAll(
            memoryTitleLabel, 
            memoryListView, 
            memoryButtonsRow1, 
            memoryButtonsRow2,
            clearAllMemoryButton
        );
        
        contentArea.getChildren().addAll(historyPanel, memoryPanel);
        
        // Store references for tab switching
        this.historyPanel = historyPanel;
        this.memoryPanel = memoryPanel;
        
        panel.getChildren().addAll(tabBar, contentArea);
        
        return panel;
    }
    
    private VBox historyPanel;
    private VBox memoryPanel;
    
    private void showHistoryPanel() {
        historyPanel.setVisible(true);
        memoryPanel.setVisible(false);
    }
    
    private void showMemoryPanel() {
        historyPanel.setVisible(false);
        memoryPanel.setVisible(true);
        updateMemoryDisplay();
    }
    
    /**
     * Tạo nút Memory
     */
    private Button createMemoryButton(String text, Runnable action) {
        Button button = new Button();
        button.setPrefWidth(60);
        button.setPrefHeight(30);
        button.setMinWidth(50);
        button.setMinHeight(30);
        button.setMaxWidth(80);
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        button.setAlignment(Pos.CENTER);
        button.setContentDisplay(javafx.scene.control.ContentDisplay.CENTER);
        button.setStyle("-fx-background-color: #0078d4; -fx-text-fill: white; -fx-background-radius: 5; -fx-border-width: 0; -fx-padding: 2px 5px;");
        button.setText(text);
        button.setWrapText(false);
        button.setOnAction(e -> action.run());
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #106ebe; -fx-text-fill: white; -fx-background-radius: 5; -fx-border-width: 0; -fx-padding: 2px 5px;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #0078d4; -fx-text-fill: white; -fx-background-radius: 5; -fx-border-width: 0; -fx-padding: 2px 5px;"));
        return button;
    }
    
    /**
     * Cập nhật hiển thị danh sách memory
     */
    private void updateMemoryDisplay() {
        if (memoryListView != null) {
            ObservableList<String> memoryList = memoryListView.getItems();
            memoryList.clear();
            
            if (memorySlots.isEmpty()) {
                memoryList.add("Chưa có bộ nhớ nào được lưu");
            } else {
                memorySlots.entrySet().stream()
                    .sorted(java.util.Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        int slot = entry.getKey();
                        double value = entry.getValue();
                        String marker = (slot == currentMemorySlot) ? "→ " : "  ";
                        memoryList.add(marker + "M" + slot + " = " + formatNumber(value));
                    });
            }
        }
    }
    
    /**
     * Memory Store - Lưu giá trị hiện tại vào memory slot hiện tại
     */
    private void handleMemoryStore() {
        try {
            double value = Double.parseDouble(currentInput);
            memorySlots.put(currentMemorySlot, value);
            updateMemoryDisplay();
            updateStatus("Đã lưu vào M" + currentMemorySlot + ": " + formatNumber(value), Color.GREEN);
            logger.info("Memory stored in M{}: {}", currentMemorySlot, value);
        } catch (NumberFormatException e) {
            showError("Lỗi", "Số không hợp lệ");
        }
    }
    
    /**
     * Memory Recall - Lấy giá trị từ memory slot hiện tại
     */
    private void handleMemoryRecall() {
        if (!memorySlots.containsKey(currentMemorySlot)) {
            showError("Lỗi", "Bộ nhớ M" + currentMemorySlot + " trống");
            return;
        }
        
        double value = memorySlots.get(currentMemorySlot);
        currentInput = formatNumber(value);
        updateDisplay();
        updateStatus("Đã lấy từ M" + currentMemorySlot + ": " + formatNumber(value), Color.BLUE);
        logger.info("Memory recalled from M{}: {}", currentMemorySlot, value);
    }
    
    /**
     * Memory Clear - Xóa memory slot hiện tại
     */
    private void handleMemoryClear() {
        if (memorySlots.remove(currentMemorySlot) != null) {
            updateMemoryDisplay();
            updateStatus("Đã xóa M" + currentMemorySlot, Color.GRAY);
            logger.info("Memory cleared: M{}", currentMemorySlot);
        } else {
            updateStatus("M" + currentMemorySlot + " đã trống", Color.GRAY);
        }
    }
    
    /**
     * Memory Add - Cộng giá trị hiện tại vào memory slot hiện tại
     */
    private void handleMemoryAdd() {
        try {
            double value = Double.parseDouble(currentInput);
            double currentMemory = memorySlots.getOrDefault(currentMemorySlot, 0.0);
            double newValue = currentMemory + value;
            memorySlots.put(currentMemorySlot, newValue);
            updateMemoryDisplay();
            updateStatus("M" + currentMemorySlot + " += " + formatNumber(value) + " = " + formatNumber(newValue), Color.GREEN);
            logger.info("Memory add to M{}: {} (new value: {})", currentMemorySlot, value, newValue);
        } catch (NumberFormatException e) {
            showError("Lỗi", "Số không hợp lệ");
        }
    }
    
    /**
     * Memory Subtract - Trừ giá trị hiện tại từ memory slot hiện tại
     */
    private void handleMemorySubtract() {
        try {
            double value = Double.parseDouble(currentInput);
            double currentMemory = memorySlots.getOrDefault(currentMemorySlot, 0.0);
            double newValue = currentMemory - value;
            memorySlots.put(currentMemorySlot, newValue);
            updateMemoryDisplay();
            updateStatus("M" + currentMemorySlot + " -= " + formatNumber(value) + " = " + formatNumber(newValue), Color.GREEN);
            logger.info("Memory subtract from M{}: {} (new value: {})", currentMemorySlot, value, newValue);
        } catch (NumberFormatException e) {
            showError("Lỗi", "Số không hợp lệ");
        }
    }
    
    /**
     * Memory Select - Chọn memory slot (0-9)
     */
    private void handleMemorySelect() {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(currentMemorySlot));
        dialog.setTitle("Chọn Bộ nhớ");
        dialog.setHeaderText("Nhập số bộ nhớ (0-9):");
        dialog.setContentText("Bộ nhớ:");
        
        dialog.showAndWait().ifPresent(input -> {
            try {
                int slot = Integer.parseInt(input.trim());
                if (slot >= 0 && slot <= 9) {
                    currentMemorySlot = slot;
                    updateMemoryDisplay();
                    updateStatus("Đã chọn M" + currentMemorySlot, Color.BLUE);
                    logger.info("Memory slot selected: M{}", currentMemorySlot);
                } else {
                    showError("Lỗi", "Số bộ nhớ phải từ 0 đến 9");
                }
            } catch (NumberFormatException e) {
                showError("Lỗi", "Số không hợp lệ");
            }
        });
    }
    
    /**
     * Xóa tất cả memory slots
     */
    private void handleClearAllMemory() {
        int count = memorySlots.size();
        memorySlots.clear();
        currentMemorySlot = 0;
        updateMemoryDisplay();
        updateStatus("Đã xóa " + count + " bộ nhớ", Color.GRAY);
        logger.info("All memory cleared ({} slots)", count);
    }
    
    /**
     * Tạo menu chọn chế độ (Chuẩn, Khoa học, Lập trình viên)
     */
    private VBox createModeMenu() {
        VBox menu = new VBox(0);
        int sideMenuWidth = Math.max(190, (int) (windowWidth * 0.2));
        menu.setPrefWidth(sideMenuWidth);
        menu.setStyle("-fx-background-color: #2d2d30;");
        
        // Header với icon hamburger
        HBox header = new HBox(10);
        header.setPadding(new Insets(15, 15, 15, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #1e1e1e;");
        
        Label hamburgerIcon = new Label("☰");
        hamburgerIcon.setFont(Font.font("Segoe UI", 18));
        hamburgerIcon.setTextFill(Color.WHITE);
        
        Label titleLabel = new Label("Máy tính tay");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.WHITE);
        
        header.getChildren().addAll(hamburgerIcon, titleLabel);
        menu.getChildren().add(header);
        
        // Nút Chuẩn
        Button btnChuan = createModeButton("Chuẩn", "Chuẩn");
        if (!"Chuẩn".equals(currentMode)) {
            btnChuan.setStyle("-fx-background-color: #2d2d30; -fx-background-radius: 0;");
        }
        menu.getChildren().add(btnChuan);
        
        // Nút Khoa học
        Button btnKhoaHoc = createModeButton("Khoa học", "Khoa học");
        menu.getChildren().add(btnKhoaHoc);
        
        // Nút Lập trình viên
        Button btnLapTrinhVien = createModeButton("Lập trình viên", "Lập trình viên");
        if (!"Lập trình viên".equals(currentMode)) {
            btnLapTrinhVien.setStyle("-fx-background-color: #2d2d30; -fx-background-radius: 0;");
        }
        menu.getChildren().add(btnLapTrinhVien);
        
        return menu;
    }
    
    /**
     * Tạo nút chế độ
     */
    private Button createModeButton(String text, String mode) {
        Button button = new Button(text);
        button.setPrefWidth(Double.MAX_VALUE);
        double modeButtonHeight = Math.max(40, windowHeight / 13.0);
        button.setPrefHeight(modeButtonHeight);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPadding(new Insets(12, 18, 12, 18));
        button.setFont(Font.font("Segoe UI", 14));
        button.setTextFill(Color.WHITE);
        button.setStyle("-fx-background-color: #2d2d30; -fx-background-radius: 0; -fx-border-width: 0;");
        
        button.setOnAction(e -> switchMode(mode, button));
        
        // Highlight chế độ hiện tại
        if (mode.equals(currentMode)) {
            button.setStyle("-fx-background-color: #0078d4; -fx-background-radius: 0; -fx-border-width: 0;");
        }
        
        // Hover effect
        button.setOnMouseEntered(e -> {
            if (!mode.equals(currentMode)) {
                button.setStyle("-fx-background-color: #3e3e42; -fx-background-radius: 0; -fx-border-width: 0;");
            }
        });
        
        button.setOnMouseExited(e -> {
            if (!mode.equals(currentMode)) {
                button.setStyle("-fx-background-color: #2d2d30; -fx-background-radius: 0; -fx-border-width: 0;");
            }
        });
        
        return button;
    }
    
    /**
     * Chuyển đổi chế độ
     */
    private void switchMode(String mode, Button clickedButton) {
        currentMode = mode;
        logger.info("Switching to mode: {}", mode);
        
        // Reset highlight cho tất cả nút
        for (var child : modeMenu.getChildren()) {
            if (child instanceof Button) {
                Button btn = (Button) child;
                if (btn != clickedButton) {
                    btn.setStyle("-fx-background-color: #2d2d30; -fx-background-radius: 0; -fx-border-width: 0;");
                }
            }
        }
        
        // Highlight nút được chọn
        clickedButton.setStyle("-fx-background-color: #0078d4; -fx-background-radius: 0; -fx-border-width: 0;");
        
        // Cập nhật bàn phím theo chế độ
        updateCalculatorPad();
        
        // Reset calculator
        clearAll();
        updateStatus("Chế độ: " + mode, Color.rgb(0, 120, 212));
    }
    
    /**
     * Cập nhật bàn phím theo chế độ
     */
    private void updateCalculatorPad() {
        calculatorPad.getChildren().clear();
        
        switch (currentMode) {
            case "Chuẩn":
                createStandardPad();
                break;
            case "Khoa học":
                createScientificPad();
                break;
            case "Lập trình viên":
                createProgrammerPad();
                break;
        }
    }

    /**
     * Tạo bàn phím máy tính đầy đủ (giống Microsoft Calculator)
     */
    private GridPane createCalculatorPad() {
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(10));
        
        // Gán vào biến instance trước khi gọi các phương thức tạo pad
        calculatorPad = grid;
        createStandardPad();
        
        return grid;
    }
    
    /**
     * Tạo bàn phím chế độ Chuẩn
     */
    private void createStandardPad() {
        calculatorPad.getChildren().clear();
        
        // Hàng 1: %, CE, C, ⌫
        calculatorPad.add(createFunctionButton("%"), 0, 0);
        calculatorPad.add(createFunctionButton("CE"), 1, 0);
        calculatorPad.add(createFunctionButton("C"), 2, 0);
        calculatorPad.add(createFunctionButton("⌫"), 3, 0);

        // Hàng 2: 1/x, x², √x, ÷
        calculatorPad.add(createFunctionButton("1/x"), 0, 1);
        calculatorPad.add(createFunctionButton("x²"), 1, 1);
        calculatorPad.add(createFunctionButton("√x"), 2, 1);
        calculatorPad.add(createOperatorButton("÷"), 3, 1);

        // Hàng 3: 7, 8, 9, ×
        calculatorPad.add(createNumberButton("7"), 0, 2);
        calculatorPad.add(createNumberButton("8"), 1, 2);
        calculatorPad.add(createNumberButton("9"), 2, 2);
        calculatorPad.add(createOperatorButton("×"), 3, 2);

        // Hàng 4: 4, 5, 6, -
        calculatorPad.add(createNumberButton("4"), 0, 3);
        calculatorPad.add(createNumberButton("5"), 1, 3);
        calculatorPad.add(createNumberButton("6"), 2, 3);
        calculatorPad.add(createOperatorButton("-"), 3, 3);

        // Hàng 5: 1, 2, 3, +
        calculatorPad.add(createNumberButton("1"), 0, 4);
        calculatorPad.add(createNumberButton("2"), 1, 4);
        calculatorPad.add(createNumberButton("3"), 2, 4);
        calculatorPad.add(createOperatorButton("+"), 3, 4);

        // Hàng 6: ±, 0, ., =
        calculatorPad.add(createFunctionButton("±"), 0, 5);
        calculatorPad.add(createNumberButton("0"), 1, 5);
        calculatorPad.add(createNumberButton("."), 2, 5);
        Button btnEquals = createOperatorButton("=");
        btnEquals.setStyle("-fx-background-color: #0078d4; -fx-text-fill: white; -fx-background-radius: 0; -fx-border-color: #0078d4; -fx-border-width: 1;");
        btnEquals.setOnMouseEntered(e -> btnEquals.setStyle("-fx-background-color: #106ebe; -fx-text-fill: white; -fx-background-radius: 0; -fx-border-color: #106ebe; -fx-border-width: 1;"));
        btnEquals.setOnMouseExited(e -> btnEquals.setStyle("-fx-background-color: #0078d4; -fx-text-fill: white; -fx-background-radius: 0; -fx-border-color: #0078d4; -fx-border-width: 1;"));
        btnEquals.setOnAction(e -> handleEquals());
        calculatorPad.add(btnEquals, 3, 5);
    }
    
    /**
     * Tạo bàn phím chế độ Khoa học
     */
    private void createScientificPad() {
        calculatorPad.getChildren().clear();
        
        // Hàng 1: sin, cos, tan, CE, C, ⌫
        calculatorPad.add(createAdvancedButton("sin"), 0, 0);
        calculatorPad.add(createAdvancedButton("cos"), 1, 0);
        calculatorPad.add(createAdvancedButton("tan"), 2, 0);
        calculatorPad.add(createFunctionButton("CE"), 3, 0);
        calculatorPad.add(createFunctionButton("C"), 4, 0);
        calculatorPad.add(createFunctionButton("⌫"), 5, 0);
        
        // Hàng 2: asin, acos, atan, log, log10, ÷
        calculatorPad.add(createAdvancedButton("asin"), 0, 1);
        calculatorPad.add(createAdvancedButton("acos"), 1, 1);
        calculatorPad.add(createAdvancedButton("atan"), 2, 1);
        calculatorPad.add(createAdvancedButton("log"), 3, 1);
        calculatorPad.add(createAdvancedButton("log10"), 4, 1);
        calculatorPad.add(createOperatorButton("÷"), 5, 1);
        
        // Hàng 3: sinh, cosh, tanh, exp, x^y, ×
        calculatorPad.add(createAdvancedButton("sinh"), 0, 2);
        calculatorPad.add(createAdvancedButton("cosh"), 1, 2);
        calculatorPad.add(createAdvancedButton("tanh"), 2, 2);
        calculatorPad.add(createAdvancedButton("exp"), 3, 2);
        calculatorPad.add(createAdvancedButton("x^y"), 4, 2);
        calculatorPad.add(createOperatorButton("×"), 5, 2);
        
        // Hàng 4: √x, ³√x, nthroot, x², 1/x, -
        calculatorPad.add(createAdvancedButton("√x"), 0, 3);
        calculatorPad.add(createAdvancedButton("³√x"), 1, 3);
        calculatorPad.add(createAdvancedButton("nthroot"), 2, 3);
        calculatorPad.add(createAdvancedButton("x²"), 3, 3);
        calculatorPad.add(createFunctionButton("1/x"), 4, 3);
        calculatorPad.add(createOperatorButton("-"), 5, 3);
        
        // Hàng 5: 7, 8, 9, +, ±, %
        calculatorPad.add(createNumberButton("7"), 0, 4);
        calculatorPad.add(createNumberButton("8"), 1, 4);
        calculatorPad.add(createNumberButton("9"), 2, 4);
        calculatorPad.add(createOperatorButton("+"), 3, 4);
        calculatorPad.add(createFunctionButton("±"), 4, 4);
        calculatorPad.add(createFunctionButton("%"), 5, 4);
        
        // Hàng 6: 4, 5, 6, ., =, ln
        calculatorPad.add(createNumberButton("4"), 0, 5);
        calculatorPad.add(createNumberButton("5"), 1, 5);
        calculatorPad.add(createNumberButton("6"), 2, 5);
        calculatorPad.add(createNumberButton("."), 3, 5);
        Button btnEquals = createOperatorButton("=");
        btnEquals.setStyle("-fx-background-color: #0078d4; -fx-text-fill: white; -fx-background-radius: 0; -fx-border-color: #0078d4; -fx-border-width: 1;");
        btnEquals.setOnMouseEntered(e -> btnEquals.setStyle("-fx-background-color: #106ebe; -fx-text-fill: white; -fx-background-radius: 0; -fx-border-color: #106ebe; -fx-border-width: 1;"));
        btnEquals.setOnMouseExited(e -> btnEquals.setStyle("-fx-background-color: #0078d4; -fx-text-fill: white; -fx-background-radius: 0; -fx-border-color: #0078d4; -fx-border-width: 1;"));
        btnEquals.setOnAction(e -> handleEquals());
        calculatorPad.add(btnEquals, 4, 5);
        calculatorPad.add(createAdvancedButton("ln"), 5, 5);
        
        // Hàng 7: 1, 2, 3, 0
        calculatorPad.add(createNumberButton("1"), 0, 6);
        calculatorPad.add(createNumberButton("2"), 1, 6);
        calculatorPad.add(createNumberButton("3"), 2, 6);
        calculatorPad.add(createNumberButton("0"), 3, 6);
    }
    
    /**
     * Tạo bàn phím chế độ Lập trình viên
     */
    private void createProgrammerPad() {
        calculatorPad.getChildren().clear();
        
        // Hàng 1: BIN, OCT, DEC, HEX, CE, C, ⌫
        calculatorPad.add(createBaseButton("BIN", 2), 0, 0);
        calculatorPad.add(createBaseButton("OCT", 8), 1, 0);
        calculatorPad.add(createBaseButton("DEC", 10), 2, 0);
        calculatorPad.add(createBaseButton("HEX", 16), 3, 0);
        calculatorPad.add(createFunctionButton("CE"), 4, 0);
        calculatorPad.add(createFunctionButton("C"), 5, 0);
        calculatorPad.add(createFunctionButton("⌫"), 6, 0);
        
        // Hàng 2: A, B, C, D, E, F, ÷
        calculatorPad.add(createHexButton("A"), 0, 1);
        calculatorPad.add(createHexButton("B"), 1, 1);
        calculatorPad.add(createHexButton("C"), 2, 1);
        calculatorPad.add(createHexButton("D"), 3, 1);
        calculatorPad.add(createHexButton("E"), 4, 1);
        calculatorPad.add(createHexButton("F"), 5, 1);
        calculatorPad.add(createOperatorButton("÷"), 6, 1);
        
        // Hàng 3: 7, 8, 9, AND, OR, XOR, ×
        calculatorPad.add(createNumberButton("7"), 0, 2);
        calculatorPad.add(createNumberButton("8"), 1, 2);
        calculatorPad.add(createNumberButton("9"), 2, 2);
        calculatorPad.add(createFunctionButton("AND"), 3, 2);
        calculatorPad.add(createFunctionButton("OR"), 4, 2);
        calculatorPad.add(createFunctionButton("XOR"), 5, 2);
        calculatorPad.add(createOperatorButton("×"), 6, 2);
        
        // Hàng 4: 4, 5, 6, NOT, LSH, RSH, -
        calculatorPad.add(createNumberButton("4"), 0, 3);
        calculatorPad.add(createNumberButton("5"), 1, 3);
        calculatorPad.add(createNumberButton("6"), 2, 3);
        calculatorPad.add(createFunctionButton("NOT"), 3, 3);
        calculatorPad.add(createFunctionButton("LSH"), 4, 3);
        calculatorPad.add(createFunctionButton("RSH"), 5, 3);
        calculatorPad.add(createOperatorButton("-"), 6, 3);
        
        // Hàng 5: 1, 2, 3, %, ±, +, =
        calculatorPad.add(createNumberButton("1"), 0, 4);
        calculatorPad.add(createNumberButton("2"), 1, 4);
        calculatorPad.add(createNumberButton("3"), 2, 4);
        calculatorPad.add(createFunctionButton("%"), 3, 4);
        calculatorPad.add(createFunctionButton("±"), 4, 4);
        calculatorPad.add(createOperatorButton("+"), 5, 4);
        Button btnEquals = createOperatorButton("=");
        btnEquals.setStyle("-fx-background-color: #0078d4; -fx-text-fill: white; -fx-background-radius: 0; -fx-border-color: #0078d4; -fx-border-width: 1;");
        btnEquals.setOnMouseEntered(e -> btnEquals.setStyle("-fx-background-color: #106ebe; -fx-text-fill: white; -fx-background-radius: 0; -fx-border-color: #106ebe; -fx-border-width: 1;"));
        btnEquals.setOnMouseExited(e -> btnEquals.setStyle("-fx-background-color: #0078d4; -fx-text-fill: white; -fx-background-radius: 0; -fx-border-color: #0078d4; -fx-border-width: 1;"));
        btnEquals.setOnAction(e -> handleEquals());
        calculatorPad.add(btnEquals, 6, 4);
        
        // Hàng 6: 0, .
        calculatorPad.add(createNumberButton("0"), 0, 5);
        calculatorPad.add(createNumberButton("."), 1, 5);
    }

    /**
     * Tạo nút số
     */
    private Button createNumberButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(buttonWidth, buttonHeight);
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        button.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-background-radius: 0; -fx-border-color: #e0e0e0; -fx-border-width: 1;");
        
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: black; -fx-background-radius: 0; -fx-border-color: #e0e0e0; -fx-border-width: 1;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-background-radius: 0; -fx-border-color: #e0e0e0; -fx-border-width: 1;"));
        button.setOnAction(e -> handleNumberInput(text));
        return button;
    }

    /**
     * Tạo nút phép toán
     */
    private Button createOperatorButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(buttonWidth, buttonHeight);
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        button.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: black; -fx-background-radius: 0; -fx-border-color: #e0e0e0; -fx-border-width: 1;");
        
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: black; -fx-background-radius: 0; -fx-border-color: #e0e0e0; -fx-border-width: 1;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: black; -fx-background-radius: 0; -fx-border-color: #e0e0e0; -fx-border-width: 1;"));
        button.setOnAction(e -> handleOperatorInput(text));
        return button;
    }

    /**
     * Tạo nút chức năng
     */
    private Button createFunctionButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(buttonWidth, buttonHeight);
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        button.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: black; -fx-background-radius: 0; -fx-border-color: #e0e0e0; -fx-border-width: 1;");
        
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: black; -fx-background-radius: 0; -fx-border-color: #e0e0e0; -fx-border-width: 1;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: black; -fx-background-radius: 0; -fx-border-color: #e0e0e0; -fx-border-width: 1;"));
        
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
            case "(":
            case ")":
                // Placeholder for future implementation
                break;
            case "AND":
                button.setOnAction(e -> handleBitwiseOperation("AND"));
                break;
            case "OR":
                button.setOnAction(e -> handleBitwiseOperation("OR"));
                break;
            case "XOR":
                button.setOnAction(e -> handleBitwiseOperation("XOR"));
                break;
            case "NOT":
                button.setOnAction(e -> handleBitwiseOperation("NOT"));
                break;
            case "LSH":
                button.setOnAction(e -> handleBitwiseOperation("LSH"));
                break;
            case "RSH":
                button.setOnAction(e -> handleBitwiseOperation("RSH"));
                break;
        }
        return button;
    }
    
    /**
     * Tạo nút chức năng nâng cao (Khoa học)
     */
    private Button createAdvancedButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(buttonWidth, buttonHeight);
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        button.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: black; -fx-background-radius: 0; -fx-border-color: #e0e0e0; -fx-border-width: 1;");
        
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: black; -fx-background-radius: 0; -fx-border-color: #e0e0e0; -fx-border-width: 1;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: black; -fx-background-radius: 0; -fx-border-color: #e0e0e0; -fx-border-width: 1;"));
        button.setOnAction(e -> handleAdvancedFunction(text));
        return button;
    }
    
    /**
     * Tạo nút hệ cơ số (Lập trình viên)
     */
    private Button createBaseButton(String text, int base) {
        Button button = new Button(text);
        button.setPrefSize(buttonWidth, buttonHeight);
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        button.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: black; -fx-background-radius: 0; -fx-border-color: #e0e0e0; -fx-border-width: 1;");
        
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: black; -fx-background-radius: 0; -fx-border-color: #e0e0e0; -fx-border-width: 1;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: black; -fx-background-radius: 0; -fx-border-color: #e0e0e0; -fx-border-width: 1;"));
        button.setOnAction(e -> handleBaseConversion(base));
        return button;
    }
    
    /**
     * Tạo nút hex (A-F)
     */
    private Button createHexButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(buttonWidth, buttonHeight);
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        button.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-background-radius: 0; -fx-border-color: #e0e0e0; -fx-border-width: 1;");
        
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: black; -fx-background-radius: 0; -fx-border-color: #e0e0e0; -fx-border-width: 1;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-background-radius: 0; -fx-border-color: #e0e0e0; -fx-border-width: 1;"));
        button.setOnAction(e -> handleHexInput(text));
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
            performAdvancedCalculation(value, 0, "sqrt");
        } catch (NumberFormatException e) {
            showError("Lỗi", "Số không hợp lệ");
        }
    }
    
    /**
     * Xử lý chức năng nâng cao (Khoa học)
     */
    private void handleAdvancedFunction(String function) {
        try {
            double value = Double.parseDouble(currentInput);
            
            // Xử lý các trường hợp đặc biệt trước
            if ("x²".equals(function)) {
                performCalculation(value, value, "*");
                return;
            }
            
            if ("x^y".equals(function)) {
                // Cần toán hạng thứ 2
                firstOperand = value;
                operator = "^";
                waitingForOperand = true;
                expression = formatNumber(value) + " ^ ";
                expressionLabel.setText(expression);
                return;
            }
            
            if ("nthroot".equals(function)) {
                // Cần toán hạng thứ 2 (bậc căn)
                firstOperand = value;
                operator = "nthroot";
                waitingForOperand = true;
                expression = formatNumber(value) + " nthroot ";
                expressionLabel.setText(expression);
                return;
            }
            
            // Xử lý các hàm một toán hạng
            String operator = switch (function) {
                case "sin" -> "sin";
                case "cos" -> "cos";
                case "tan" -> "tan";
                case "asin" -> "asin";
                case "acos" -> "acos";
                case "atan" -> "atan";
                case "sinh" -> "sinh";
                case "cosh" -> "cosh";
                case "tanh" -> "tanh";
                case "log" -> "log";
                case "ln" -> "ln";
                case "log10" -> "log10";
                case "exp" -> "exp";
                case "√x" -> "sqrt";
                case "³√x" -> "cbrt";
                default -> null;
            };
            
            if (operator != null) {
                performAdvancedCalculation(value, 0, operator);
            }
        } catch (NumberFormatException e) {
            showError("Lỗi", "Số không hợp lệ");
        }
    }
    
    /**
     * Xử lý bitwise operations (Lập trình viên)
     */
    private void handleBitwiseOperation(String op) {
        try {
            double value = Double.parseDouble(currentInput);
            
            if ("NOT".equals(op)) {
                // NOT là unary operation
                performAdvancedCalculation(value, 0, "NOT");
            } else {
                // AND, OR, XOR, LSH, RSH cần toán hạng thứ 2
                firstOperand = value;
                operator = op;
                waitingForOperand = true;
                String displayOp = switch (op) {
                    case "AND" -> " & ";
                    case "OR" -> " | ";
                    case "XOR" -> " ^ ";
                    case "LSH" -> " << ";
                    case "RSH" -> " >> ";
                    default -> " " + op + " ";
                };
                expression = formatNumber(value) + displayOp;
                expressionLabel.setText(expression);
            }
        } catch (NumberFormatException e) {
            showError("Lỗi", "Số không hợp lệ");
        }
    }
    
    /**
     * Xử lý chuyển đổi hệ cơ số (Lập trình viên)
     */
    private void handleBaseConversion(int toBase) {
        try {
            // Giả sử số hiện tại ở hệ thập phân, chuyển sang hệ cơ số khác
            long value = Long.parseLong(currentInput.split("\\.")[0]);
            String baseName = switch (toBase) {
                case 2 -> "BIN";
                case 8 -> "OCT";
                case 10 -> "DEC";
                case 16 -> "HEX";
                default -> String.valueOf(toBase);
            };
            String result = Long.toString(value, toBase).toUpperCase();
            currentInput = result;
            expression = "DEC(" + value + ") = " + baseName + "(" + result + ")";
            expressionLabel.setText(expression);
            updateDisplay();
            addToHistory(expression);
        } catch (NumberFormatException e) {
            showError("Lỗi", "Số không hợp lệ");
        }
    }
    
    /**
     * Xử lý nhập hex (A-F)
     */
    private void handleHexInput(String hex) {
        if (waitingForOperand) {
            currentInput = hex;
            waitingForOperand = false;
        } else {
            if (currentInput.equals("0")) {
                currentInput = hex;
            } else {
                currentInput += hex;
            }
        }
        updateDisplay();
    }
    
    /**
     * Thực hiện tính toán nâng cao
     */
    private void performAdvancedCalculation(double op1, double op2, String op) {
        updateStatus("Đang tính toán...", Color.BLUE);
        
        new Thread(() -> {
            try {
                CalculatorClient.CalculationResult result = calculatorClient.calculateAdvanced(op1, op2, op);
                
                Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        currentInput = formatResult(result.getResult());
                        String funcName = getFunctionDisplayName(op);
                        // Xử lý hiển thị cho các loại hàm khác nhau
                        if (op.equals("AND") || op.equals("OR") || op.equals("XOR") || 
                            op.equals("LSH") || op.equals("RSH") || op.equals("nthroot")) {
                            expression = formatNumber(op1) + " " + funcName + " " + formatNumber(op2) + " =";
                        } else if (op.equals("NOT")) {
                            expression = funcName + "(" + formatNumber(op1) + ") =";
                        } else {
                            expression = funcName + "(" + formatNumber(op1) + ") =";
                        }
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
     * Lấy tên hàm để hiển thị
     */
    private String getFunctionDisplayName(String operator) {
        return switch (operator) {
            case "sin" -> "sin";
            case "cos" -> "cos";
            case "tan" -> "tan";
            case "asin" -> "arcsin";
            case "acos" -> "arccos";
            case "atan" -> "arctan";
            case "sinh" -> "sinh";
            case "cosh" -> "cosh";
            case "tanh" -> "tanh";
            case "log", "ln" -> "ln";
            case "log10" -> "log₁₀";
            case "exp" -> "e^x";
            case "sqrt" -> "√";
            case "cbrt" -> "³√";
            case "nthroot" -> "ⁿ√";
            case "AND" -> "&";
            case "OR" -> "|";
            case "XOR" -> "^";
            case "NOT" -> "~";
            case "LSH" -> "<<";
            case "RSH" -> ">>";
            default -> operator;
        };
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
        if (calculationHistory.size() > maxHistoryEntries) {
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
            
            // Kiểm tra nếu là bitwise operation hoặc advanced operation
            boolean isAdvanced = operator.equals("AND") || operator.equals("OR") || 
                                operator.equals("XOR") || operator.equals("LSH") || 
                                operator.equals("RSH") || operator.equals("nthroot") ||
                                operator.equals("^") || operator.equals("pow");
            
            if (isAdvanced) {
                performAdvancedCalculation(firstOperand, secondOperand, operator);
            } else {
                performCalculation(firstOperand, secondOperand, operator);
            }
            
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

    /**
     * Thiết lập phím tắt bàn phím (nhập số, phép toán, điều khiển)
     */
    private void setupKeyboardShortcuts(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_TYPED, this::handleKeyTyped);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
    }

    private void handleKeyTyped(KeyEvent event) {
        String ch = event.getCharacter();
        if (ch == null || ch.isBlank()) {
            return;
        }

        switch (ch) {
            case "+" -> handleOperatorInput("+");
            case "-" -> handleOperatorInput("-");
            case "*" -> handleOperatorInput("×");
            case "/" -> handleOperatorInput("÷");
            case "=" -> handleEquals();
            case "." -> handleNumberInput(".");
            default -> {
                if (ch.matches("[0-9]")) {
                    handleNumberInput(ch);
                } else {
                    return;
                }
            }
        }
        event.consume();
    }

    private void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();

        switch (code) {
            case ENTER, EQUALS -> handleEquals();
            case BACK_SPACE -> handleBackspace();
            case DELETE -> handleClearEntry();
            case ESCAPE -> clearAll();
            case ADD -> handleOperatorInput("+");
            case SUBTRACT -> handleOperatorInput("-");
            case MULTIPLY -> handleOperatorInput("×");
            case DIVIDE -> handleOperatorInput("÷");
            case DECIMAL -> handleNumberInput(".");
            default -> {
                return;
            }
        }
        event.consume();
    }

    public static void main(String[] args) {
        launch(args);
    }
}