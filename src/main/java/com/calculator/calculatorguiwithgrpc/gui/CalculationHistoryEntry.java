package com.calculator.calculatorguiwithgrpc.gui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single calculation history entry.
 */
public record CalculationHistoryEntry(LocalDateTime timestamp, String expression, boolean success, String message) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public String toString() {
        String status = success ? message : "Error: " + message;
        return String.format("[%s] %s -> %s", FORMATTER.format(timestamp), expression, status);
    }

    public String formattedLineForFile() {
        return String.format("%s | %s | %s | %s",
                timestamp,
                success ? "SUCCESS" : "FAIL",
                expression,
                message);
    }
}

