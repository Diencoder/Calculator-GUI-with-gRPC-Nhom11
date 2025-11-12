package com.calculator.calculatorguiwithgrpc.client;

import com.calculator.calculatorguiwithgrpc.proto.CalculatorProtos.*;
import com.calculator.calculatorguiwithgrpc.proto.CalculatorServiceGrpc;
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

    private final ManagedChannel channel;
    private final CalculatorServiceGrpc.CalculatorServiceBlockingStub blockingStub;
    private final String clientId;

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

        // Kiểm tra tính hợp lệ của đầu vào
        if (!isValidInput(operand1, operand2, operator)) {
            logger.warn("[Client-{}] Đầu vào không hợp lệ: {} {} {}", clientId, operand1, operator, operand2);
            return new CalculationResult(false, 0.0, "Đầu vào không hợp lệ.");
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
     * Kiểm tra đầu vào có hợp lệ không (kiểm tra số và toán tử)
     */
    private boolean isValidInput(double operand1, double operand2, String operator) {
        return isValidOperator(operator) && isValidNumber(operand1) && isValidNumber(operand2);
    }

    /**
     * Kiểm tra toán tử hợp lệ
     */
    private boolean isValidOperator(String operator) {
        return operator != null && (operator.equals("+") || operator.equals("-") || 
                operator.equals("*") || operator.equals("/") || operator.equals("%"));
    }

    /**
     * Kiểm tra số hợp lệ
     */
    private boolean isValidNumber(double operand) {
        return !Double.isNaN(operand) && !Double.isInfinite(operand);
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
