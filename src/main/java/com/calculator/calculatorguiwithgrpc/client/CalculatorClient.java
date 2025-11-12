package com.calculator.calculatorguiwithgrpc.client;

import com.calculator.calculatorguiwithgrpc.proto.CalculatorProtos.*;
import com.calculator.calculatorguiwithgrpc.proto.CalculatorServiceGrpc;
import com.calculator.calculatorguiwithgrpc.utils.ValidationUtils;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * gRPC Client cho Dịch vụ Máy tính
 * Xử lý giao tiếp với Máy chủ Máy tính qua gRPC
 * Bao gồm xử lý lỗi kết nối mạng và logging chi tiết
 *
 * @author Văn Điền - gRPC Client Developer
 * @version 1.0
 */
public class CalculatorClient {

    private static final Logger logger = LoggerFactory.getLogger(CalculatorClient.class);
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9090;
    private static final long REQUEST_TIMEOUT_SECONDS = 10;
    private static final long CONNECTION_TIMEOUT_SECONDS = 5;
    
    // Giới hạn cho validation
    private static final double MAX_SAFE_VALUE = 1e15;  // Giá trị tối đa an toàn
    private static final double MIN_SAFE_VALUE = -1e15; // Giá trị tối thiểu an toàn
    private static final double MAX_EXPONENT = 1000;    // Số mũ tối đa cho phép

    private final ManagedChannel channel;
    private final CalculatorServiceGrpc.CalculatorServiceBlockingStub blockingStub;
    private final String clientId;
    private final ValidationUtils validationUtils;

    /**
     * Constructor với host và port mặc định
     */
    public CalculatorClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    /**
     * Constructor với host và port tùy chỉnh
     */
    public CalculatorClient(String host, int port) {
        this.clientId = UUID.randomUUID().toString().substring(0, 8);
        this.validationUtils = new ValidationUtils();
        logger.info("[Client-{}] Đang khởi tạo Calculator Client - Host: {}, Port: {}", clientId, host, port);
        
        try {
            this.channel = ManagedChannelBuilder.forAddress(host, port)
                    .usePlaintext()
                    .keepAliveTime(30, TimeUnit.SECONDS)
                    .keepAliveTimeout(5, TimeUnit.SECONDS)
                    .keepAliveWithoutCalls(true)
                    .build();
            
            // Tạo base stub (deadline sẽ được thêm cho từng request)
            this.blockingStub = CalculatorServiceGrpc.newBlockingStub(channel);
            
            logger.info("[Client-{}] Calculator Client đã được khởi tạo thành công", clientId);
        } catch (Exception e) {
            logger.error("[Client-{}] Lỗi khi khởi tạo kết nối đến server: {}", clientId, e.getMessage(), e);
            throw new RuntimeException("Không thể khởi tạo kết nối đến server: " + e.getMessage(), e);
        }
    }

    /**
     * Thực hiện phép toán cơ bản hoặc nâng cao
     * Gửi CalculationRequest (OperationRequest) và nhận CalculationResponse (OperationResult)
     */
    public CalculationResult performCalculation(double operand1, double operand2, String operator) {
        logger.info("[Client-{}] Gửi yêu cầu tính toán: {} {} {}", clientId, operand1, operator, operand2);

        // Validation chi tiết với thông báo lỗi cụ thể
        ValidationResult validationResult = validateInput(operand1, operand2, operator);
        if (!validationResult.isValid()) {
            String errorMessage = validationResult.getErrorMessage();
            logger.warn("[Client-{}] Validation thất bại: {} {} {} - Lỗi: {}", 
                    clientId, operand1, operator, operand2, errorMessage);
            return new CalculationResult(false, 0.0, errorMessage);
        }

        // Tạo yêu cầu (OperationRequest)
        String requestId = UUID.randomUUID().toString();
        CalculationRequest request = createCalculationRequest(operand1, operand2, operator, requestId);
        
        logger.debug("[Client-{}] Request ID: {}, Request: operand1={}, operand2={}, operator={}", 
                clientId, requestId, operand1, operand2, operator);

        // Gửi request và nhận response (OperationResult)
        return sendCalculationRequest(request);
    }

    /**
     * Validation chi tiết đầu vào với thông báo lỗi cụ thể
     */
    private ValidationResult validateInput(double operand1, double operand2, String operator) {
        StringBuilder errors = new StringBuilder();
        
        // 1. Kiểm tra toán tử
        if (operator == null || operator.trim().isEmpty()) {
            errors.append("Toán tử không được để trống. ");
            logger.debug("[Client-{}] Toán tử null hoặc rỗng", clientId);
        } else if (!validationUtils.isValidOperator(operator.trim())) {
            errors.append("Toán tử không hợp lệ: '").append(operator).append("'. ");
            errors.append("Các toán tử hợp lệ: +, -, *, /, %, ^. ");
            logger.debug("[Client-{}] Toán tử không hợp lệ: {}", clientId, operator);
        }
        
        // 2. Kiểm tra số operand1
        String operand1Error = validateOperand(operand1, "Số thứ nhất");
        if (operand1Error != null) {
            errors.append(operand1Error).append(" ");
        }
        
        // 3. Kiểm tra số operand2
        String operand2Error = validateOperand(operand2, "Số thứ hai");
        if (operand2Error != null) {
            errors.append(operand2Error).append(" ");
        }
        
        // 4. Kiểm tra các trường hợp đặc biệt theo toán tử
        if (operator != null && validationUtils.isValidOperator(operator.trim())) {
            String operatorError = validateOperatorSpecificCases(operand1, operand2, operator.trim());
            if (operatorError != null) {
                errors.append(operatorError).append(" ");
            }
        }
        
        // 5. Kiểm tra overflow tiềm năng
        String overflowError = checkPotentialOverflow(operand1, operand2, operator);
        if (overflowError != null) {
            errors.append(overflowError).append(" ");
        }
        
        boolean isValid = errors.length() == 0;
        String errorMessage = isValid ? null : errors.toString().trim();
        
        if (isValid) {
            logger.debug("[Client-{}] Validation thành công cho: {} {} {}", clientId, operand1, operator, operand2);
        }
        
        return new ValidationResult(isValid, errorMessage);
    }
    
    /**
     * Kiểm tra tính hợp lệ của một số
     */
    private String validateOperand(double operand, String operandName) {
        if (Double.isNaN(operand)) {
            logger.debug("[Client-{}] {} là NaN", clientId, operandName);
            return operandName + " không phải là số hợp lệ (NaN).";
        }
        
        if (Double.isInfinite(operand)) {
            logger.debug("[Client-{}] {} là Infinity", clientId, operandName);
            return operandName + " không được là vô cực (Infinity).";
        }
        
        if (operand > MAX_SAFE_VALUE) {
            logger.debug("[Client-{}] {} quá lớn: {}", clientId, operandName, operand);
            return operandName + " quá lớn (>" + MAX_SAFE_VALUE + ").";
        }
        
        if (operand < MIN_SAFE_VALUE) {
            logger.debug("[Client-{}] {} quá nhỏ: {}", clientId, operandName, operand);
            return operandName + " quá nhỏ (<" + MIN_SAFE_VALUE + ").";
        }
        
        return null; // Hợp lệ
    }
    
    /**
     * Kiểm tra các trường hợp đặc biệt theo toán tử
     */
    private String validateOperatorSpecificCases(double operand1, double operand2, String operator) {
        switch (operator) {
            case "/":
                if (operand2 == 0.0) {
                    logger.debug("[Client-{}] Phát hiện chia cho 0", clientId);
                    return "Không thể chia cho 0.";
                }
                break;
                
            case "%":
                if (operand2 == 0.0) {
                    logger.debug("[Client-{}] Phát hiện modulo cho 0", clientId);
                    return "Không thể lấy phần dư khi chia cho 0.";
                }
                break;
                
            case "^":
                // Kiểm tra số mũ quá lớn
                if (Math.abs(operand2) > MAX_EXPONENT) {
                    logger.debug("[Client-{}] Số mũ quá lớn: {}", clientId, operand2);
                    return "Số mũ quá lớn (|" + operand2 + "| > " + MAX_EXPONENT + ").";
                }
                // Kiểm tra cơ số âm với số mũ không nguyên
                if (operand1 < 0 && operand2 != (long) operand2) {
                    logger.debug("[Client-{}] Cơ số âm với số mũ không nguyên: {} ^ {}", clientId, operand1, operand2);
                    return "Không thể tính lũy thừa với cơ số âm và số mũ không nguyên.";
                }
                break;
        }
        
        return null; // Hợp lệ
    }
    
    /**
     * Kiểm tra khả năng overflow
     */
    private String checkPotentialOverflow(double operand1, double operand2, String operator) {
        if (operator == null || !validationUtils.isValidOperator(operator.trim())) {
            return null;
        }
        
        try {
            switch (operator.trim()) {
                case "*":
                    // Kiểm tra nhân có thể gây overflow
                    if (Math.abs(operand1) > 1 && Math.abs(operand2) > Double.MAX_VALUE / Math.abs(operand1)) {
                        logger.debug("[Client-{}] Phát hiện overflow tiềm năng khi nhân: {} * {}", clientId, operand1, operand2);
                        return "Kết quả phép nhân có thể quá lớn.";
                    }
                    break;
                    
                case "^":
                    // Kiểm tra lũy thừa có thể gây overflow
                    if (Math.abs(operand1) > 1 && operand2 > Math.log(Double.MAX_VALUE) / Math.log(Math.abs(operand1))) {
                        logger.debug("[Client-{}] Phát hiện overflow tiềm năng khi lũy thừa: {} ^ {}", clientId, operand1, operand2);
                        return "Kết quả lũy thừa có thể quá lớn.";
                    }
                    break;
            }
        } catch (Exception e) {
            logger.debug("[Client-{}] Lỗi khi kiểm tra overflow: {}", clientId, e.getMessage());
            // Bỏ qua lỗi kiểm tra overflow, không chặn request
        }
        
        return null;
    }
    
    /**
     * Lớp kết quả validation (tương tự ValidationUtils.ValidationResult)
     */
    private static class ValidationResult {
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

    /**
     * Tạo yêu cầu tính toán mới
     */
    private CalculationRequest createCalculationRequest(double operand1, double operand2, String operator, String requestId) {
        return CalculationRequest.newBuilder()
                .setOperand1(operand1)
                .setOperand2(operand2)
                .setOperator(operator)
                .setRequestId(requestId)
                .build();
    }

    /**
     * Gửi yêu cầu tính toán (OperationRequest) và nhận kết quả (OperationResult)
     * Xử lý các lỗi kết nối mạng chi tiết
     */
    private CalculationResult sendCalculationRequest(CalculationRequest request) {
        String requestId = request.getRequestId();
        logger.debug("[Client-{}] Đang gửi request ID: {}", clientId, requestId);
        
        try {
            // Gửi request và nhận response với timeout
            CalculationResponse response = blockingStub
                    .withDeadlineAfter(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .calculate(request);
            
            logger.debug("[Client-{}] Nhận được response cho request ID: {}, Success: {}, Result: {}", 
                    clientId, requestId, response.getSuccess(), response.getResult());

            if (response.getSuccess()) {
                logger.info("[Client-{}] Tính toán thành công - Request ID: {}, Kết quả: {}", 
                        clientId, requestId, response.getResult());
                return new CalculationResult(true, response.getResult(), null);
            } else {
                logger.error("[Client-{}] Tính toán thất bại - Request ID: {}, Lỗi: {}", 
                        clientId, requestId, response.getErrorMessage());
                return new CalculationResult(false, 0.0, response.getErrorMessage());
            }
            
        } catch (StatusRuntimeException e) {
            // Xử lý các lỗi gRPC cụ thể
            Status status = e.getStatus();
            String errorMessage = handleGrpcError(status, e);
            logger.error("[Client-{}] Lỗi gRPC - Request ID: {}, Status: {}, Mô tả: {}", 
                    clientId, requestId, status.getCode(), status.getDescription(), e);
            return new CalculationResult(false, 0.0, errorMessage);
            
        } catch (Exception e) {
            // Xử lý các lỗi kết nối mạng khác
            String errorMessage = handleNetworkError(e);
            logger.error("[Client-{}] Lỗi kết nối mạng - Request ID: {}, Lỗi: {}", 
                    clientId, requestId, e.getMessage(), e);
            return new CalculationResult(false, 0.0, errorMessage);
        }
    }
    
    /**
     * Xử lý các lỗi gRPC cụ thể
     */
    private String handleGrpcError(Status status, StatusRuntimeException e) {
        return switch (status.getCode()) {
            case UNAVAILABLE -> {
                logger.error("[Client-{}] Server không khả dụng - Có thể server chưa chạy hoặc mất kết nối", clientId);
                yield "Không thể kết nối đến server. Vui lòng kiểm tra server đã chạy chưa.";
            }
            case DEADLINE_EXCEEDED -> {
                logger.error("[Client-{}] Request timeout - Server không phản hồi trong thời gian cho phép", clientId);
                yield "Request timeout - Server không phản hồi. Vui lòng thử lại.";
            }
            case INTERNAL -> {
                logger.error("[Client-{}] Lỗi nội bộ server", clientId);
                yield "Lỗi nội bộ server: " + status.getDescription();
            }
            case INVALID_ARGUMENT -> {
                logger.error("[Client-{}] Tham số không hợp lệ", clientId);
                yield "Tham số không hợp lệ: " + status.getDescription();
            }
            case UNIMPLEMENTED -> {
                logger.error("[Client-{}] Phương thức chưa được triển khai", clientId);
                yield "Phương thức chưa được triển khai trên server.";
            }
            default -> {
                logger.error("[Client-{}] Lỗi gRPC: {}", clientId, status.getCode());
                yield "Lỗi gRPC: " + status.getDescription();
            }
        };
    }
    
    /**
     * Xử lý các lỗi kết nối mạng
     */
    private String handleNetworkError(Exception e) {
        Throwable cause = e.getCause();
        
        if (cause instanceof ConnectException) {
            logger.error("[Client-{}] Không thể kết nối đến server - Connection refused", clientId);
            return "Không thể kết nối đến server. Vui lòng kiểm tra server đã chạy và đúng địa chỉ/port.";
        }
        
        if (cause instanceof SocketTimeoutException || e instanceof java.util.concurrent.TimeoutException) {
            logger.error("[Client-{}] Kết nối timeout", clientId);
            return "Kết nối timeout. Server không phản hồi trong thời gian cho phép.";
        }
        
        if (cause instanceof java.net.UnknownHostException) {
            logger.error("[Client-{}] Không tìm thấy host", clientId);
            return "Không tìm thấy địa chỉ server. Vui lòng kiểm tra lại hostname.";
        }
        
        if (cause instanceof java.net.NoRouteToHostException) {
            logger.error("[Client-{}] Không có đường dẫn đến host", clientId);
            return "Không thể kết nối đến server. Kiểm tra kết nối mạng.";
        }
        
        logger.error("[Client-{}] Lỗi mạng không xác định: {}", clientId, e.getClass().getSimpleName());
        return "Lỗi kết nối mạng: " + e.getMessage();
    }

    /**
     * Kiểm tra tình trạng của máy chủ
     */
    public boolean isServerHealthy() {
        logger.info("[Client-{}] Đang kiểm tra tình trạng máy chủ...", clientId);
        
        try {
            HealthCheckRequest request = HealthCheckRequest.newBuilder()
                    .setService("calculator")
                    .build();

            HealthCheckResponse response = blockingStub
                    .withDeadlineAfter(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .healthCheck(request);
            
            boolean isHealthy = response.getStatus() == HealthCheckResponse.ServingStatus.SERVING;

            if (isHealthy) {
                logger.info("[Client-{}] Máy chủ KHỎE MẠNH - Status: {}, Message: {}", 
                        clientId, response.getStatus(), response.getMessage());
            } else {
                logger.warn("[Client-{}] Máy chủ KHÔNG KHỎE MẠNH - Status: {}, Message: {}", 
                        clientId, response.getStatus(), response.getMessage());
            }
            
            return isHealthy;
            
        } catch (StatusRuntimeException e) {
            Status status = e.getStatus();
            logger.error("[Client-{}] Kiểm tra tình trạng thất bại - Status: {}, Mô tả: {}", 
                    clientId, status.getCode(), status.getDescription(), e);
            return false;
        } catch (Exception e) {
            logger.error("[Client-{}] Kiểm tra tình trạng thất bại - Lỗi: {}", 
                    clientId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Tắt máy khách và đóng kết nối
     */
    public void shutdown() {
        logger.info("[Client-{}] Đang tắt Calculator Client...", clientId);
        
        try {
            if (channel != null && !channel.isShutdown()) {
                channel.shutdown();
                boolean terminated = channel.awaitTermination(5, TimeUnit.SECONDS);
                
                if (!terminated) {
                    logger.warn("[Client-{}] Channel chưa tắt hoàn toàn, đang force shutdown...", clientId);
                    channel.shutdownNow();
                    channel.awaitTermination(2, TimeUnit.SECONDS);
                }
                
                logger.info("[Client-{}] Calculator Client đã tắt thành công", clientId);
            }
        } catch (InterruptedException e) {
            logger.error("[Client-{}] Lỗi trong quá trình tắt máy khách", clientId, e);
            Thread.currentThread().interrupt();
            if (channel != null) {
                channel.shutdownNow();
            }
        } catch (Exception e) {
            logger.error("[Client-{}] Lỗi không mong đợi khi tắt client", clientId, e);
        }
    }

    /**
     * Lớp kết quả cho các phép toán tính toán
     */
    public static class CalculationResult {
        private final boolean success;
        private final double result;
        private final String errorMessage;

        public CalculationResult(boolean success, double result, String errorMessage) {
            this.success = success;
            this.result = result;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public double getResult() {
            return result;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String toString() {
            return success ? "CalculationResult{success=true, result=" + result + "}" :
                    "CalculationResult{success=false, error='" + errorMessage + "'}";
        }
    }
}
