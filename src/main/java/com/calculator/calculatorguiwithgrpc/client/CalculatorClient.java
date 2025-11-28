package com.calculator.calculatorguiwithgrpc.client;

import com.calculator.calculatorguiwithgrpc.config.AppConfig;
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
    private final ManagedChannel channel;
    private final CalculatorServiceGrpc.CalculatorServiceBlockingStub blockingStub;
    private final String clientId;
    private final CalculatorClientConfig config;
    private final CalculatorInputValidator inputValidator;
    private final ValidationUtils validationUtils;
    private volatile boolean isShutdown = false;

    /**
     * Constructor với host và port mặc định
     */
    public CalculatorClient() {
        this(CalculatorClientConfig.builder().build());
    }

    /**
     * Constructor với host và port tùy chỉnh
     */
    public CalculatorClient(String host, int port) {
        this(CalculatorClientConfig.builder().host(host).port(port).build());
    }

    /**
     * Constructor với cấu hình tùy chỉnh.
     */
    public CalculatorClient(CalculatorClientConfig config) {
        this.clientId = UUID.randomUUID().toString().substring(0, 8);
        this.config = config;
        this.inputValidator = new CalculatorInputValidator();
        this.validationUtils = new ValidationUtils();
        logger.info("[Client-{}] Đang khởi tạo Calculator Client - Host: {}, Port: {}", clientId,
                config.getHost(), config.getPort());

        try {
            AppConfig appConfig = AppConfig.getInstance();
            this.channel = ManagedChannelBuilder.forAddress(config.getHost(), config.getPort())
                    .usePlaintext()
                    .keepAliveTime(appConfig.getClientKeepAliveTimeSeconds(), TimeUnit.SECONDS)
                    .keepAliveTimeout(appConfig.getClientKeepAliveTimeoutSeconds(), TimeUnit.SECONDS)
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
        // Kiểm tra client đã shutdown chưa
        if (isShutdown) {
            logger.warn("[Client-{}] Client đã bị shutdown, không thể thực hiện tính toán", clientId);
            return new CalculationResult(false, 0.0, "Client đã bị shutdown.");
        }

        // Kiểm tra channel state
        if (channel == null || channel.isShutdown() || channel.isTerminated()) {
            logger.error("[Client-{}] Channel không khả dụng", clientId);
            return new CalculationResult(false, 0.0, "Kết nối đến server không khả dụng.");
        }

        logger.debug("[Client-{}] Gửi yêu cầu tính toán: {} {} {}", clientId, operand1, operator, operand2);

        // Validation chi tiết với thông báo lỗi cụ thể
        // Kiểm tra xem có phải advanced operator không
        boolean isAdvanced = validationUtils.isValidAdvancedOperator(operator) && 
                            !validationUtils.isValidOperator(operator);
        
        CalculatorInputValidator.ValidationResult validationResult =
                inputValidator.validateInputs(operand1, operand2, operator, isAdvanced);
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

        // Gửi request với retry mechanism
        return sendCalculationRequestWithRetry(request);
    }

    /**
     * API thân thiện cho GUI: phép toán cơ bản.
     */
    public CalculationResult calculate(double operand1, double operand2, String operator) {
        return performCalculation(operand1, operand2, operator);
    }

    /**
     * API thân thiện cho GUI: phép toán nâng cao (tạm thời dùng chung service).
     */
    public CalculationResult calculateAdvanced(double operand1, double operand2, String operator) {
        return performCalculation(operand1, operand2, operator);
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
     * Gửi yêu cầu tính toán với retry mechanism
     * Xử lý các lỗi tạm thời bằng cách retry
     */
    private CalculationResult sendCalculationRequestWithRetry(CalculationRequest request) {
        String requestId = request.getRequestId();
        int attempt = 0;
        int maxAttempts = config.getMaxRetryAttempts();

        while (attempt < maxAttempts) {
            attempt++;
            logger.debug("[Client-{}] Gửi request ID: {} (Lần thử: {}/{})", 
                    clientId, requestId, attempt, maxAttempts);

            CalculationResult result = sendCalculationRequest(request);
            
            // Nếu thành công hoặc lỗi không thể retry, trả về ngay
            if (result.isSuccess() || !shouldRetry(result.getErrorMessage())) {
                if (attempt > 1) {
                    logger.info("[Client-{}] Request ID: {} thành công sau {} lần thử", 
                            clientId, requestId, attempt);
                }
                return result;
            }

            // Nếu cần retry và chưa đạt max attempts
            if (attempt < maxAttempts) {
                logger.warn("[Client-{}] Request ID: {} thất bại, đang retry sau {}ms...", 
                        clientId, requestId, config.getRetryDelayMillis());
                try {
                    Thread.sleep(config.getRetryDelayMillis() * attempt); // Exponential backoff
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("[Client-{}] Retry bị gián đoạn", clientId);
                    return new CalculationResult(false, 0.0, "Request bị gián đoạn.");
                }
            }
        }

        logger.error("[Client-{}] Request ID: {} thất bại sau {} lần thử", 
                clientId, requestId, maxAttempts);
        return new CalculationResult(false, 0.0, 
                "Không thể kết nối đến server sau " + maxAttempts + " lần thử.");
    }

    /**
     * Kiểm tra xem có nên retry request không dựa trên error message
     */
    private boolean shouldRetry(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }
        // Retry cho các lỗi tạm thời
        return errorMessage.contains("không thể kết nối") ||
               errorMessage.contains("timeout") ||
               errorMessage.contains("không phản hồi") ||
               errorMessage.contains("UNAVAILABLE");
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
                    .withDeadlineAfter(config.getRequestTimeoutSeconds(), TimeUnit.SECONDS)
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
        if (isShutdown || channel == null || channel.isShutdown() || channel.isTerminated()) {
            logger.warn("[Client-{}] Client hoặc channel không khả dụng để kiểm tra health", clientId);
            return false;
        }

        logger.info("[Client-{}] Đang kiểm tra tình trạng máy chủ...", clientId);

        try {
            HealthCheckRequest request = HealthCheckRequest.newBuilder()
                    .setService("calculator")
                    .build();

            HealthCheckResponse response = blockingStub
                    .withDeadlineAfter(config.getConnectionTimeoutSeconds(), TimeUnit.SECONDS)
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
        if (isShutdown) {
            logger.warn("[Client-{}] Client đã được shutdown trước đó", clientId);
            return;
        }

        isShutdown = true;
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
     * Kiểm tra xem client đã bị shutdown chưa
     */
    public boolean isShutdown() {
        return isShutdown;
    }

    /**
     * Kiểm tra xem channel có đang kết nối không
     */
    public boolean isConnected() {
        return channel != null && !channel.isShutdown() && !channel.isTerminated() && !isShutdown;
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