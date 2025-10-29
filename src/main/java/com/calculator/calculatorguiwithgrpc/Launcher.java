package com.calculator.calculatorguiwithgrpc;

import com.calculator.calculatorguiwithgrpc.gui.CalculatorGUI;
import javafx.application.Application;

/**
 * Launcher class for JavaFX application
 * This is required to avoid JavaFX runtime components missing error
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(CalculatorGUI.class, args);
    }
}
