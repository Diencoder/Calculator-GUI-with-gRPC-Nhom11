package com.calculator.calculatorguiwithgrpc.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enhanced gRPC Server for Calculator Service
 * Handles calculation requests from clients with improved configuration,
 * error handling, and monitoring capabilities.
 * 
 * This server implementation demonstrates key concepts in network programming:
 * 
 * 1. SERVER SOCKET & PORT BINDING:
 *    - Server binds to a specific port (default 9090) to listen for incoming connections
 *    - Port is configurable via system properties or environment variables
 *    - Port validation ensures it's within valid range (1024-65535)
 * 
 * 2. CONNECTION HANDLING:
 *    - Tracks active and total connections
 *    - Monitors connection lifecycle (establishment, maintenance, termination)
 *    - Handles connection timeouts and errors
 * 
 * 3. REQUEST/RESPONSE PROCESSING:
 *    - Thread pool manages concurrent request handling
 *    - Each request is processed in a separate thread
 *    - Request counting and statistics tracking
 * 
 * 4. NETWORK STATISTICS:
 *    - Bytes sent/received tracking
 *    - Connection metrics (active, total, errors, timeouts)
 *    - Request/response timing
 * 
 * 5. GRACEFUL SHUTDOWN:
 *    - Allows in-flight requests to complete
 *    - Closes new connections gracefully
 *    - Timeout-based forced shutdown if needed
 * 
 * 6. ERROR HANDLING:
 *    - Network error detection and recovery
 *    - Connection timeout handling
 *    - Proper resource cleanup
 * 
 * Features:
 * - Configurable port via system properties or environment variables
 * - Graceful shutdown with configurable timeout
 * - Server status tracking and request counting
 * - Network statistics (connections, bytes, errors)
 * - Enhanced error handling and logging
 * - Custom thread pool configuration
 * 
 * @author Calculator Team
 * @version 3.0 - Enhanced for Network Programming Education
 */
public class CalculatorServer {
    
    private static final Logger logger = LoggerFactory.getLogger(CalculatorServer.class);
    
    // Configuration constants
    private static final String PORT_PROPERTY = "calculator.server.port";
    private static final String PORT_ENV_VAR = "CALCULATOR_SERVER_PORT";
    private static final int DEFAULT_PORT = 9090;
    private static final int MIN_PORT = 1024;
    private static final int MAX_PORT = 65535;
    
    private static final String SHUTDOWN_TIMEOUT_PROPERTY = "calculator.server.shutdown.timeout";
    private static final long DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 30;
    private static final long MIN_SHUTDOWN_TIMEOUT_SECONDS = 5;
    private static final long MAX_SHUTDOWN_TIMEOUT_SECONDS = 300;
    
    // Server instance
    private Server server;
    private final int port;
    private final long shutdownTimeoutSeconds;
    
    // Server state tracking
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicLong requestCount = new AtomicLong(0);
    private final long startTime = System.currentTimeMillis();
    
    // Network statistics tracking (for learning network programming)
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong bytesReceived = new AtomicLong(0);
    private final AtomicLong bytesSent = new AtomicLong(0);
    private final AtomicLong networkErrors = new AtomicLong(0);
    private final AtomicLong connectionTimeouts = new AtomicLong(0);
    
    /**
     * Constructor with default configuration
     */
    public CalculatorServer() {
        this(getConfiguredPort(), getConfiguredShutdownTimeout());
    }
    
    /**
     * Constructor with custom port
     * 
     * @param port The port number to bind the server to
     */
    public CalculatorServer(int port) {
        this(port, DEFAULT_SHUTDOWN_TIMEOUT_SECONDS);
    }
    
    /**
     * Constructor with full configuration
     * 
     * @param port The port number to bind the server to
     * @param shutdownTimeoutSeconds Timeout for graceful shutdown
     */
    public CalculatorServer(int port, long shutdownTimeoutSeconds) {
        this.port = validatePort(port);
        this.shutdownTimeoutSeconds = validateTimeout(shutdownTimeoutSeconds);
        logger.info("Calculator Server initialized - Port: {}, Shutdown Timeout: {}s", 
                   this.port, this.shutdownTimeoutSeconds);
    }
    
    /**
     * Start the gRPC server
     * 
     * @throws IOException if the server fails to start
     * @throws IllegalStateException if the server is already running
     */
    public void start() throws IOException {
        if (isRunning.get()) {
            throw new IllegalStateException("Server is already running");
        }
        
        logger.info("Starting Calculator gRPC Server on port {}...", port);
        
        try {
            // Build and start the server with enhanced configuration
            // NETWORK PROGRAMMING CONCEPT: Server Socket Creation
            // - ServerBuilder.forPort() creates a server socket bound to the specified port
            // - This is similar to ServerSocket in Java NIO, but using gRPC abstraction
            server = ServerBuilder.forPort(port)
                .addService(new CalculatorServiceImpl())
                .addService(new AdvancedCalculatorService())
                    .intercept(new LogHandler(this)) // Pass server instance for network stats
                    // NETWORK PROGRAMMING CONCEPT: Thread Pool for Concurrent Connections
                    // - Each incoming connection/request is handled by a separate thread
                    // - CachedThreadPool creates threads on demand and reuses them
                    // - This allows the server to handle multiple clients simultaneously
                    .executor(Executors.newCachedThreadPool(r -> {
                        Thread t = new Thread(r, "calculator-server-worker");
                        t.setDaemon(false);
                        return t;
                    }))
                    // NETWORK PROGRAMMING CONCEPT: Message Size Limits
                    // - Prevents DoS attacks via oversized messages
                    // - Protects server memory from exhaustion
                    .maxInboundMessageSize(4 * 1024 * 1024) // 4MB max message size
                    .maxInboundMetadataSize(8 * 1024) // 8KB max metadata size
                .build()
                .start();
        
            // NETWORK PROGRAMMING CONCEPT: Server is now listening on the port
            // - Server.accept() equivalent (handled by gRPC internally)
            // - Server is ready to accept incoming TCP connections
            
            isRunning.set(true);
            
            logger.info("✓ Calculator gRPC Server started successfully on port {}", port);
            logger.info("  Server is ready to accept connections");
            logServerInfo();
            
            // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown signal received, initiating graceful shutdown...");
            try {
                CalculatorServer.this.stop();
            } catch (InterruptedException e) {
                logger.error("Error during server shutdown", e);
                Thread.currentThread().interrupt();
            }
            }, "calculator-server-shutdown-hook"));
            
        } catch (IOException e) {
            isRunning.set(false);
            logger.error("Failed to start Calculator gRPC Server on port {}", port, e);
            throw new IOException("Failed to start server on port " + port + ": " + e.getMessage(), e);
        } catch (Exception e) {
            isRunning.set(false);
            logger.error("Unexpected error while starting server", e);
            throw new RuntimeException("Unexpected error while starting server: " + e.getMessage(), e);
        }
    }
    
    /**
     * Stop the gRPC server gracefully
     * 
     * @throws InterruptedException if the shutdown is interrupted
     */
    public void stop() throws InterruptedException {
        if (!isRunning.get() || server == null) {
            logger.warn("Server is not running, nothing to stop");
            return;
        }
        
        logger.info("Stopping Calculator gRPC Server...");
        isRunning.set(false);
        
        try {
            // Initiate graceful shutdown
            server.shutdown();
            
            // Wait for graceful shutdown
            logger.info("Waiting up to {} seconds for graceful shutdown...", shutdownTimeoutSeconds);
            boolean terminated = server.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS);
            
            if (!terminated) {
                logger.warn("Server did not terminate gracefully within {} seconds, forcing shutdown...", 
                           shutdownTimeoutSeconds);
                server.shutdownNow();
                
                // Wait for forced shutdown
                terminated = server.awaitTermination(5, TimeUnit.SECONDS);
                
                if (!terminated) {
                    logger.error("Server did not terminate even after forced shutdown");
                } else {
                    logger.info("Server terminated after forced shutdown");
                }
            } else {
                logger.info("Server terminated gracefully");
            }
            
            logServerStatistics();
            
        } catch (InterruptedException e) {
            logger.error("Shutdown interrupted, forcing immediate shutdown", e);
            if (server != null) {
                server.shutdownNow();
            }
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            logger.error("Error during server shutdown", e);
        if (server != null) {
                server.shutdownNow();
            }
        }
    }
    
    /**
     * Block until the server is terminated
     * 
     * @throws InterruptedException if interrupted while waiting
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server == null) {
            logger.warn("Server is not initialized, cannot block until shutdown");
            return;
        }
        
        logger.info("Server is running. Press Ctrl+C to stop...");
            server.awaitTermination();
    }
    
    /**
     * Check if the server is currently running
     * 
     * @return true if the server is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning.get() && server != null && !server.isShutdown();
    }
    
    /**
     * Get the port the server is bound to
     * 
     * @return the port number
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Get the number of requests processed
     * 
     * @return the request count
     */
    public long getRequestCount() {
        return requestCount.get();
    }
    
    /**
     * Increment the request counter
     */
    public void incrementRequestCount() {
        requestCount.incrementAndGet();
    }
    
    /**
     * Get server uptime in milliseconds
     * 
     * @return uptime in milliseconds
     */
    public long getUptime() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * Get server statistics as a formatted string
     * Includes network programming metrics for educational purposes
     * 
     * @return server statistics with network metrics
     */
    public String getServerStatistics() {
        if (!isRunning()) {
            return "Server is not running";
        }
        
        long uptimeSeconds = getUptime() / 1000;
        long hours = uptimeSeconds / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        long seconds = uptimeSeconds % 60;
        
        // Calculate network throughput
        double bytesPerSecond = uptimeSeconds > 0 
            ? (double) (bytesReceived.get() + bytesSent.get()) / uptimeSeconds 
            : 0.0;
        
        return String.format(
            "=== Server Statistics ===\n" +
            "Status: Running\n" +
            "Port: %d (listening for connections)\n" +
            "Uptime: %d hours, %d minutes, %d seconds\n\n" +
            "=== Request Statistics ===\n" +
            "Total Requests: %d\n" +
            "Requests/Second: %.2f\n\n" +
            "=== Network Statistics (Network Programming Metrics) ===\n" +
            "Active Connections: %d\n" +
            "Total Connections: %d\n" +
            "Bytes Received: %d (%.2f KB)\n" +
            "Bytes Sent: %d (%.2f KB)\n" +
            "Total Bytes Transferred: %d (%.2f KB)\n" +
            "Network Throughput: %.2f bytes/sec\n" +
            "Network Errors: %d\n" +
            "Connection Timeouts: %d",
            port,
            hours, minutes, seconds,
            requestCount.get(),
            uptimeSeconds > 0 ? (double) requestCount.get() / uptimeSeconds : 0.0,
            activeConnections.get(),
            totalConnections.get(),
            bytesReceived.get(), bytesReceived.get() / 1024.0,
            bytesSent.get(), bytesSent.get() / 1024.0,
            bytesReceived.get() + bytesSent.get(), 
            (bytesReceived.get() + bytesSent.get()) / 1024.0,
            bytesPerSecond,
            networkErrors.get(),
            connectionTimeouts.get()
        );
    }
    
    // ========== Network Statistics Methods (for learning network programming) ==========
    
    /**
     * NETWORK PROGRAMMING CONCEPT: Connection Tracking
     * Increment active connection count when a new client connects
     */
    public void onConnectionEstablished() {
        activeConnections.incrementAndGet();
        totalConnections.incrementAndGet();
        logger.debug("New connection established. Active: {}, Total: {}", 
                    activeConnections.get(), totalConnections.get());
    }
    
    /**
     * NETWORK PROGRAMMING CONCEPT: Connection Cleanup
     * Decrement active connection count when a client disconnects
     */
    public void onConnectionClosed() {
        long current = activeConnections.decrementAndGet();
        if (current < 0) {
            activeConnections.set(0); // Prevent negative values
        }
        logger.debug("Connection closed. Active: {}", current);
    }
    
    /**
     * NETWORK PROGRAMMING CONCEPT: Data Transfer Tracking
     * Track bytes received from clients (incoming data)
     */
    public void addBytesReceived(long bytes) {
        if (bytes > 0) {
            bytesReceived.addAndGet(bytes);
        }
    }
    
    /**
     * NETWORK PROGRAMMING CONCEPT: Data Transfer Tracking
     * Track bytes sent to clients (outgoing data)
     */
    public void addBytesSent(long bytes) {
        if (bytes > 0) {
            bytesSent.addAndGet(bytes);
        }
    }
    
    /**
     * NETWORK PROGRAMMING CONCEPT: Error Handling
     * Track network errors (connection failures, timeouts, etc.)
     */
    public void onNetworkError() {
        networkErrors.incrementAndGet();
        logger.warn("Network error occurred. Total errors: {}", networkErrors.get());
    }
    
    /**
     * NETWORK PROGRAMMING CONCEPT: Timeout Handling
     * Track connection timeouts (when client doesn't respond in time)
     */
    public void onConnectionTimeout() {
        connectionTimeouts.incrementAndGet();
        logger.warn("Connection timeout occurred. Total timeouts: {}", connectionTimeouts.get());
    }
    
    /**
     * Get active connection count
     * NETWORK PROGRAMMING CONCEPT: Connection Pool Monitoring
     */
    public long getActiveConnections() {
        return activeConnections.get();
    }
    
    /**
     * Get total connection count (lifetime)
     */
    public long getTotalConnections() {
        return totalConnections.get();
    }
    
    /**
     * Get total bytes received
     */
    public long getBytesReceived() {
        return bytesReceived.get();
    }
    
    /**
     * Get total bytes sent
     */
    public long getBytesSent() {
        return bytesSent.get();
    }
    
    /**
     * Get network error count
     */
    public long getNetworkErrors() {
        return networkErrors.get();
    }
    
    /**
     * Get connection timeout count
     */
    public long getConnectionTimeouts() {
        return connectionTimeouts.get();
    }
    
    /**
     * Log server information
     */
    private void logServerInfo() {
        logger.info("========================================");
        logger.info("Calculator gRPC Server Information");
        logger.info("  Port: {}", port);
        logger.info("  Java Version: {}", System.getProperty("java.version"));
        logger.info("  Java Vendor: {}", System.getProperty("java.vendor"));
        logger.info("  OS: {} {}", System.getProperty("os.name"), System.getProperty("os.version"));
        logger.info("========================================");
    }
    
    /**
     * Log server statistics including network programming metrics
     */
    private void logServerStatistics() {
        long uptimeSeconds = getUptime() / 1000;
        logger.info("========================================");
        logger.info("Server Statistics (Final)");
        logger.info("  Total Uptime: {} seconds", uptimeSeconds);
        logger.info("  Total Requests: {}", requestCount.get());
        if (uptimeSeconds > 0) {
            double avgRequestsPerSecond = (double) requestCount.get() / uptimeSeconds;
            logger.info("  Average Requests/Second: {}", String.format("%.2f", avgRequestsPerSecond));
        }
        logger.info("");
        logger.info("Network Programming Metrics:");
        logger.info("  Total Connections: {}", totalConnections.get());
        logger.info("  Bytes Received: {} ({} KB)", bytesReceived.get(), 
                   String.format("%.2f", bytesReceived.get() / 1024.0));
        logger.info("  Bytes Sent: {} ({} KB)", bytesSent.get(), 
                   String.format("%.2f", bytesSent.get() / 1024.0));
        logger.info("  Total Bytes Transferred: {} ({} KB)", 
                   bytesReceived.get() + bytesSent.get(),
                   String.format("%.2f", (bytesReceived.get() + bytesSent.get()) / 1024.0));
        if (uptimeSeconds > 0) {
            double throughput = (double) (bytesReceived.get() + bytesSent.get()) / uptimeSeconds;
            logger.info("  Network Throughput: {} bytes/sec", String.format("%.2f", throughput));
        }
        logger.info("  Network Errors: {}", networkErrors.get());
        logger.info("  Connection Timeouts: {}", connectionTimeouts.get());
        logger.info("========================================");
    }
    
    /**
     * Validate port number
     * 
     * @param port the port to validate
     * @return the validated port
     * @throws IllegalArgumentException if port is invalid
     */
    private static int validatePort(int port) {
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException(
                String.format("Port must be between %d and %d, got: %d", MIN_PORT, MAX_PORT, port));
        }
        return port;
    }
    
    /**
     * Validate timeout value
     * 
     * @param timeout the timeout to validate
     * @return the validated timeout
     * @throws IllegalArgumentException if timeout is invalid
     */
    private static long validateTimeout(long timeout) {
        if (timeout < MIN_SHUTDOWN_TIMEOUT_SECONDS || timeout > MAX_SHUTDOWN_TIMEOUT_SECONDS) {
            throw new IllegalArgumentException(
                String.format("Shutdown timeout must be between %d and %d seconds, got: %d", 
                            MIN_SHUTDOWN_TIMEOUT_SECONDS, MAX_SHUTDOWN_TIMEOUT_SECONDS, timeout));
        }
        return timeout;
    }
    
    /**
     * Get configured port from system property or environment variable
     * 
     * @return the configured port or default
     */
    private static int getConfiguredPort() {
        // Try system property first
        String portStr = System.getProperty(PORT_PROPERTY);
        if (portStr != null && !portStr.trim().isEmpty()) {
            try {
                return Integer.parseInt(portStr.trim());
            } catch (NumberFormatException e) {
                logger.warn("Invalid port in system property {}: {}, using default {}", 
                           PORT_PROPERTY, portStr, DEFAULT_PORT);
            }
        }
        
        // Try environment variable
        portStr = System.getenv(PORT_ENV_VAR);
        if (portStr != null && !portStr.trim().isEmpty()) {
            try {
                return Integer.parseInt(portStr.trim());
            } catch (NumberFormatException e) {
                logger.warn("Invalid port in environment variable {}: {}, using default {}", 
                           PORT_ENV_VAR, portStr, DEFAULT_PORT);
            }
        }
        
        return DEFAULT_PORT;
    }
    
    /**
     * Get configured shutdown timeout from system property
     * 
     * @return the configured timeout or default
     */
    private static long getConfiguredShutdownTimeout() {
        String timeoutStr = System.getProperty(SHUTDOWN_TIMEOUT_PROPERTY);
        if (timeoutStr != null && !timeoutStr.trim().isEmpty()) {
            try {
                return Long.parseLong(timeoutStr.trim());
            } catch (NumberFormatException e) {
                logger.warn("Invalid shutdown timeout in system property {}: {}, using default {}", 
                           SHUTDOWN_TIMEOUT_PROPERTY, timeoutStr, DEFAULT_SHUTDOWN_TIMEOUT_SECONDS);
            }
        }
        return DEFAULT_SHUTDOWN_TIMEOUT_SECONDS;
    }
    
    /**
     * Main method to start the server
     * 
     * Usage:
     *   java CalculatorServer
     *   java -Dcalculator.server.port=9091 CalculatorServer
     *   CALCULATOR_SERVER_PORT=9091 java CalculatorServer
     * 
     * @param args command line arguments (currently unused)
     */
    public static void main(String[] args) {
        try {
        CalculatorServer server = new CalculatorServer();
        server.start();
        server.blockUntilShutdown();
        } catch (IOException e) {
            logger.error("Failed to start server", e);
            System.exit(1);
        } catch (InterruptedException e) {
            logger.error("Server interrupted", e);
            Thread.currentThread().interrupt();
            System.exit(1);
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            System.exit(1);
        }
    }
}
