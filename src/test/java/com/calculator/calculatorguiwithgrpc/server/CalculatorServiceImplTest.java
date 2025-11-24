package com.calculator.calculatorguiwithgrpc.server;

import com.calculator.calculatorguiwithgrpc.proto.CalculatorProtos.CalculationRequest;
import com.calculator.calculatorguiwithgrpc.proto.CalculatorProtos.CalculationResponse;
import com.calculator.calculatorguiwithgrpc.proto.CalculatorProtos.HealthCheckRequest;
import com.calculator.calculatorguiwithgrpc.proto.CalculatorProtos.HealthCheckResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorServiceImplTest {

    private CalculatorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CalculatorServiceImpl();
    }

    @Test
    void calculate_addition_shouldReturnSuccessResponse() {
        CalculationRequest request = CalculationRequest.newBuilder()
                .setOperand1(12)
                .setOperand2(8)
                .setOperator("+")
                .setRequestId("req-add")
                .build();

        TestObserver observer = new TestObserver();

        service.calculate(request, observer);

        assertTrue(observer.completed);
        assertEquals(1, observer.responses.size());
        CalculationResponse response = observer.responses.get(0);
        assertTrue(response.getSuccess());
        assertEquals(20, response.getResult(), 0.0001);
        assertEquals("req-add", response.getRequestId());
    }

    @Test
    void calculate_divisionByZero_shouldReturnErrorResponse() {
        CalculationRequest request = CalculationRequest.newBuilder()
                .setOperand1(12)
                .setOperand2(0)
                .setOperator("/")
                .setRequestId("req-div-zero")
                .build();

        TestObserver observer = new TestObserver();

        service.calculate(request, observer);

        assertTrue(observer.completed);
        assertEquals(1, observer.responses.size());
        CalculationResponse response = observer.responses.get(0);
        assertFalse(response.getSuccess());
        assertTrue(response.getErrorMessage().toLowerCase().contains("division"));
    }

    @Test
    void healthCheck_shouldReturnServingStatus() {
        HealthCheckRequest request = HealthCheckRequest.newBuilder()
                .setService("calculator")
                .build();

        HealthObserver observer = new HealthObserver();

        service.healthCheck(request, observer);

        assertTrue(observer.completed);
        assertEquals(1, observer.responses.size());
        HealthCheckResponse response = observer.responses.get(0);
        assertEquals(HealthCheckResponse.ServingStatus.SERVING, response.getStatus());
        assertEquals("Calculator service is running", response.getMessage());
    }

    private static class TestObserver implements StreamObserver<CalculationResponse> {
        private final List<CalculationResponse> responses = new ArrayList<>();
        private boolean completed = false;

        @Override
        public void onNext(CalculationResponse value) {
            responses.add(value);
        }

        @Override
        public void onError(Throwable t) {
            fail("Unexpected error: " + t.getMessage());
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }

    private static class HealthObserver implements StreamObserver<HealthCheckResponse> {
        private final List<HealthCheckResponse> responses = new ArrayList<>();
        private boolean completed = false;

        @Override
        public void onNext(HealthCheckResponse value) {
            responses.add(value);
        }

        @Override
        public void onError(Throwable t) {
            fail("Unexpected error: " + t.getMessage());
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }
}

