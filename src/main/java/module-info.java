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

    opens pl.domator.App to javafx.fxml;
    opens pl.domator.Modules.UI to javafx.fxml;
    opens pl.domator.Config to javafx.fxml;
    opens pl.domator.Core to javafx.fxml;
    opens pl.domator.Security to javafx.fxml;
    opens pl.domator.Modules.Finances.Controller to javafx.fxml;
    opens pl.domator.Modules.Finances.Model to javafx.fxml;
    opens pl.domator.Modules.Vehicle.Controller to javafx.fxml;
    opens pl.domator.Modules.Vehicle.Model to javafx.fxml;
    opens pl.domator.Modules.MealPlanner.Controller to javafx.fxml;
    opens pl.domator.Modules.MealPlanner.Model to javafx.fxml;

    exports pl.domator.App;
    exports pl.domator.Modules.UI;
    exports pl.domator.Config;
    exports pl.domator.Core;
    exports pl.domator.Security;
    exports pl.domator.Modules.Finances.Controller;
    exports pl.domator.Modules.Finances.Model;
    exports pl.domator.Modules.Vehicle.Controller;
    exports pl.domator.Modules.Vehicle.Model;
    exports pl.domator.Modules.MealPlanner.Controller;
    exports pl.domator.Modules.MealPlanner.Model;
}