module pl.domator {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires java.sql;
    requires java.desktop;
    requires org.postgresql.jdbc;

    opens pl.domator.app to javafx.fxml;
    opens pl.domator.modules.ui to javafx.fxml;
    opens pl.domator.config to javafx.fxml;
    opens pl.domator.core to javafx.fxml;
    opens pl.domator.security to javafx.fxml;

    exports pl.domator.app;
    exports pl.domator.modules.ui;
    exports pl.domator.config;
    exports pl.domator.core;
    exports pl.domator.security;
}