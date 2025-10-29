package com.calculator.calculatorguiwithgrpc.client;

import com.calculator.calculatorguiwithgrpc.proto.CalculatorProtos.*;
import com.calculator.calculatorguiwithgrpc.proto.CalculatorServiceGrpc;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * gRPC Client cho Dịch vụ Máy tính
 * Xử lý giao tiếp với Máy chủ Máy tính
 *
 * @author Team 11
 * @version 1.0
 */
public class CalculatorClient {

    private static final Logger logger = LoggerFactory.getLogger(CalculatorClient.class);
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9090;

    private final ManagedChannel channel;
    private final CalculatorServiceGrpc.CalculatorServiceBlockingStub blockingStub;

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
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = CalculatorServiceGrpc.newBlockingStub(channel);
        logger.info("Calculator Client đã được khởi tạo - Host: {}, Port: {}", host, port);
    }

    /**
     * Thực hiện phép toán cơ bản hoặc nâng cao
     */
    public CalculationResult performCalculation(double operand1, double operand2, String operator) {
        logger.info("Gửi yêu cầu tính toán: {} {} {}", operand1, operator, operand2);

        // Kiểm tra tính hợp lệ của đầu vào
        if (!isValidInput(operand1, operand2, operator)) {
            return new CalculationResult(false, 0.0, "Đầu vào không hợp lệ.");
        }

        // Tạo yêu cầu và gửiisServerHealthy
        String requestId = UUID.randomUUID().toString();
        CalculationRequest request = createCalculationRequest(operand1, operand2, operator, requestId);

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
        return operator.equals("+") || operator.equals("-") || operator.equals("*") || operator.equals("/");
    }

    /**
     * Kiểm tra số hợp lệ
     */
    private boolean isValidNumber(double operand) {
        return operand != Double.NaN && operand != Double.POSITIVE_INFINITY && operand != Double.NEGATIVE_INFINITY;
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
     * Gửi yêu cầu tính toán và trả kết quả
     */
    private CalculationResult sendCalculationRequest(CalculationRequest request) {
        try {
            CalculationResponse response = blockingStub.calculate(request);

            if (response.getSuccess()) {
                logger.info("Tính toán thành công: Kết quả = {}", response.getResult());
                return new CalculationResult(true, response.getResult(), null);
            } else {
                logger.error("Tính toán thất bại: {}", response.getErrorMessage());
                return new CalculationResult(false, 0.0, response.getErrorMessage());
            }
        } catch (StatusRuntimeException e) {
            logger.error("Gọi gRPC thất bại", e);
            return new CalculationResult(false, 0.0, "Gọi gRPC thất bại: " + e.getStatus().getDescription());
        } catch (Exception e) {
            logger.error("Lỗi bất ngờ trong quá trình tính toán", e);
            return new CalculationResult(false, 0.0, "Lỗi bất ngờ: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra tình trạng của máy chủ
     */
    public boolean isServerHealthy() {
        try {
            HealthCheckRequest request = HealthCheckRequest.newBuilder()
                    .setService("calculator")
                    .build();

            HealthCheckResponse response = blockingStub.healthCheck(request);
            boolean isHealthy = response.getStatus() == HealthCheckResponse.ServingStatus.SERVING;

            logger.info("Kiểm tra tình trạng máy chủ: {}", isHealthy ? "KHỎE MẠNH" : "MẤT KẾT NỐI");
            return isHealthy;
        } catch (Exception e) {
            logger.error("Kiểm tra tình trạng thất bại", e);
            return false;
        }
    }

    /**
     * Tắt máy khách
     */
    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            logger.info("Calculator Client đã tắt thành công");
        } catch (InterruptedException e) {
            logger.error("Lỗi trong quá trình tắt máy khách", e);
            Thread.currentThread().interrupt();
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
