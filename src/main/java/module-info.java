module com.calculator.calculatorguiwithgrpc {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    opens com.calculator.calculatorguiwithgrpc to javafx.fxml;
    exports com.calculator.calculatorguiwithgrpc;
}