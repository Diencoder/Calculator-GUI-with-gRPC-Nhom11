package com.calculator.calculatorguiwithgrpc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application configuration manager.
 * Loads configuration from application.properties file.
 * 
 * @author Calculator Team
 * @version 1.0
 */
public class AppConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static final String CONFIG_FILE = "/application.properties";
    private static AppConfig instance;
    private final Properties properties;
    
    private AppConfig() {
        this.properties = new Properties();
        loadProperties();
    }
    
    /**
     * Get singleton instance of AppConfig
     */
    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }
    
    /**
     * Load properties from application.properties file
     */
    private void loadProperties() {
        try (InputStream inputStream = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                logger.warn("Configuration file {} not found, using default values", CONFIG_FILE);
                return;
            }
            properties.load(inputStream);
            logger.info("Configuration loaded successfully from {}", CONFIG_FILE);
        } catch (IOException e) {
            logger.error("Error loading configuration file: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get property value as String
     */
    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Get property value as int
     */
    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for key '{}': {}, using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Get property value as long
     */
    public long getLong(String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid long value for key '{}': {}, using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Get property value as double
     */
    public double getDouble(String key, double defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid double value for key '{}': {}, using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Get property value as boolean
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }
    
    // ============================================
    // Server Configuration Getters
    // ============================================
    
    public String getServerHost() {
        return getString("calculator.server.host", "localhost");
    }
    
    public int getServerPort() {
        return getInt("calculator.server.port", 9090);
    }
    
    public int getServerShutdownTimeoutSeconds() {
        return getInt("calculator.server.shutdown.timeout.seconds", 30);
    }
    
    // ============================================
    // Client Configuration Getters
    // ============================================
    
    public String getClientHost() {
        return getString("calculator.client.host", "localhost");
    }
    
    public int getClientPort() {
        return getInt("calculator.client.port", 9090);
    }
    
    public long getClientRequestTimeoutSeconds() {
        return getLong("calculator.client.request.timeout.seconds", 10);
    }
    
    public long getClientConnectionTimeoutSeconds() {
        return getLong("calculator.client.connection.timeout.seconds", 5);
    }
    
    public int getClientMaxRetryAttempts() {
        return getInt("calculator.client.max.retry.attempts", 3);
    }
    
    public long getClientRetryDelayMillis() {
        return getLong("calculator.client.retry.delay.millis", 500);
    }
    
    public long getClientKeepAliveTimeSeconds() {
        return getLong("calculator.client.keepalive.time.seconds", 30);
    }
    
    public long getClientKeepAliveTimeoutSeconds() {
        return getLong("calculator.client.keepalive.timeout.seconds", 5);
    }
    
    // ============================================
    // GUI Configuration Getters
    // ============================================
    
    public int getGuiHistoryMaxEntries() {
        return getInt("calculator.gui.history.max.entries", 100);
    }
    
    public int getGuiWindowWidth() {
        return getInt("calculator.gui.window.width", 1200);
    }
    
    public int getGuiWindowHeight() {
        return getInt("calculator.gui.window.height", 750);
    }
    
    public int getGuiDisplayFontSize() {
        return getInt("calculator.gui.display.font.size", 48);
    }
    
    // ============================================
    // Validation Configuration Getters
    // ============================================
    
    public double getValidationMaxSafeValue() {
        return getDouble("calculator.validation.max.safe.value", 1.0E15);
    }
    
    public double getValidationMinSafeValue() {
        return getDouble("calculator.validation.min.safe.value", -1.0E15);
    }
    
    public double getValidationMaxExponent() {
        return getDouble("calculator.validation.max.exponent", 1000);
    }
    
    // ============================================
    // Logging Configuration Getters
    // ============================================
    
    public String getLoggingHistoryFile() {
        return getString("calculator.logging.history.file", "logs/calculation-history.log");
    }
    
    public String getLoggingLevel() {
        return getString("calculator.logging.level", "INFO");
    }
}


