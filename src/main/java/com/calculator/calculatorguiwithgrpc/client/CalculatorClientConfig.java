package com.calculator.calculatorguiwithgrpc.client;

/**
 * Immutable configuration for {@link CalculatorClient}.
 */
public class CalculatorClientConfig {

    private final String host;
    private final int port;
    private final long requestTimeoutSeconds;
    private final long connectionTimeoutSeconds;
    private final int maxRetryAttempts;
    private final long retryDelayMillis;

    private CalculatorClientConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.requestTimeoutSeconds = builder.requestTimeoutSeconds;
        this.connectionTimeoutSeconds = builder.connectionTimeoutSeconds;
        this.maxRetryAttempts = builder.maxRetryAttempts;
        this.retryDelayMillis = builder.retryDelayMillis;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public long getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public long getConnectionTimeoutSeconds() {
        return connectionTimeoutSeconds;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public long getRetryDelayMillis() {
        return retryDelayMillis;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String host = "localhost";
        private int port = 9090;
        private long requestTimeoutSeconds = 10;
        private long connectionTimeoutSeconds = 5;
        private int maxRetryAttempts = 3;
        private long retryDelayMillis = 500;

        private Builder() {
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder requestTimeoutSeconds(long requestTimeoutSeconds) {
            this.requestTimeoutSeconds = requestTimeoutSeconds;
            return this;
        }

        public Builder connectionTimeoutSeconds(long connectionTimeoutSeconds) {
            this.connectionTimeoutSeconds = connectionTimeoutSeconds;
            return this;
        }

        public Builder maxRetryAttempts(int maxRetryAttempts) {
            this.maxRetryAttempts = maxRetryAttempts;
            return this;
        }

        public Builder retryDelayMillis(long retryDelayMillis) {
            this.retryDelayMillis = retryDelayMillis;
            return this;
        }

        public CalculatorClientConfig build() {
            return new CalculatorClientConfig(this);
        }
    }
}

