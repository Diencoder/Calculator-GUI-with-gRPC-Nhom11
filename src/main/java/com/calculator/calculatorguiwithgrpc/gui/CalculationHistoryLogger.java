package com.calculator.calculatorguiwithgrpc.gui;

import com.calculator.calculatorguiwithgrpc.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Simple file-based logger for calculation history.
 */
public class CalculationHistoryLogger {

    private static final Logger logger = LoggerFactory.getLogger(CalculationHistoryLogger.class);
    private static final AppConfig appConfig = AppConfig.getInstance();
    private final Path logFile;

    public CalculationHistoryLogger() {
        this(Path.of(appConfig.getLoggingHistoryFile()));
    }

    public CalculationHistoryLogger(Path logFile) {
        this.logFile = logFile;
        ensureFile();
    }

    private void ensureFile() {
        try {
            if (logFile.getParent() != null) {
                Files.createDirectories(logFile.getParent());
            }
            if (!Files.exists(logFile)) {
                Files.createFile(logFile);
            }
        } catch (IOException e) {
            logger.error("Unable to initialize history log file {}", logFile, e);
        }
    }

    public void log(CalculationHistoryEntry entry) {
        try {
            Files.writeString(
                    logFile,
                    entry.formattedLineForFile() + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            logger.error("Failed to write calculation history entry", e);
        }
    }
}

