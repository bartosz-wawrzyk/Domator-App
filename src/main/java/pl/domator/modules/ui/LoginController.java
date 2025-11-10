package pl.domator.modules.ui;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pl.domator.config.DBConnectionController;
import pl.domator.core.DatabaseUpdater;
import pl.domator.core.AutoLoginManager;
import pl.domator.core.LoggerUtils;
import pl.domator.security.PasswordUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    private int userId;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckBox;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;
    @FXML private StackPane loadingPane;

    @FXML
    public void initialize() {
        loadingPane.setVisible(false);

        String[] creds = AutoLoginManager.loadCredentials();
        if (creds != null) {
            usernameField.setText(creds[0]);
            passwordField.setText(creds[1]);
            rememberMeCheckBox.setSelected(true);
        }
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Login i hasło nie mogą być puste.");
            return;
        }

        loadingPane.setVisible(true);

        Task<Void> loginTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (Connection conn = DBConnectionController.getConnection()) {
                    if (conn == null || conn.isClosed()) {
                        throw new Exception("Brak połączenia z bazą.");
                    }

                    PreparedStatement ps = conn.prepareStatement("SELECT * FROM dmt.users WHERE username=?");
                    ps.setString(1, username);
                    ResultSet rsUser = ps.executeQuery();

                    if (!rsUser.next() ||
                            !rsUser.getString("password_hash").equals(PasswordUtils.hashPassword(password))) {
                        throw new Exception("Nieprawidłowe dane logowania.");
                    }
                    userId = rsUser.getInt("id");
                }
                return null;
            }
        };

        loginTask.setOnSucceeded(e -> {
            if (rememberMeCheckBox.isSelected()) {
                AutoLoginManager.saveCredentials(username, password);
            } else {
                AutoLoginManager.clearCredentials();
            }

            if (username.equals("admin") && password.equals("admin")) {
                return;
            }

            checkDatabaseVersionAndProceed();
        });

        loginTask.setOnFailed(e -> {
            loadingPane.setVisible(false);
            statusLabel.setText(loginTask.getException().getMessage());
        });

        new Thread(loginTask).start();
    }

    private void checkDatabaseVersionAndProceed() {
        Task<Void> versionTask = new Task<>() {
            @Override
            protected Void call() {
                try (Connection conn = DBConnectionController.getConnection()) {

                    int currentVersion = DatabaseUpdater.getCurrentVersion(conn);
                    int latestVersion = DatabaseUpdater.getLatestVersion();

                    if (currentVersion < latestVersion) {
                        runOnFxThread(() -> {
                            statusLabel.setText("Aktualizacja bazy danych...");

                            Task<Void> updateTask = new Task<>() {
                                @Override
                                protected Void call() {
                                    DatabaseUpdater.updateIfNeeded();
                                    return null;
                                }
                            };

                            updateTask.setOnSucceeded(ev -> {
                                new Thread(() -> {
                                    try {
                                        Thread.sleep(3000);
                                    } catch (InterruptedException ex) {
                                        LoggerUtils.logError(ex);

                                    }
                                    runOnFxThread(() -> {
                                        statusLabel.setText("");
                                        loadingPane.setVisible(false);
                                        openDashboard();
                                    });
                                }).start();
                            });

                            updateTask.setOnFailed(ev -> {
                                loadingPane.setVisible(false);
                                statusLabel.setText("Błąd podczas aktualizacji bazy.");
                            });

                            new Thread(updateTask).start();
                        });

                    } else {
                        runOnFxThread(() -> {
                            loadingPane.setVisible(false);
                            openDashboard();
                        });
                    }

                } catch (Exception ex) {
                    LoggerUtils.logError(ex);
                    runOnFxThread(() -> {
                        loadingPane.setVisible(false);
                        statusLabel.setText("Błąd podczas sprawdzania wersji bazy.");
                    });
                }
                return null;
            }
        };

        new Thread(versionTask).start();
    }

    private void runOnFxThread(Runnable r) {
        if (javafx.application.Platform.isFxApplicationThread()) {
            r.run();
        } else {
            javafx.application.Platform.runLater(r);
        }
    }

    @FXML
    public void openDBConfig() {
        try {
            Stage cfgStage = new Stage();
            cfgStage.initModality(Modality.APPLICATION_MODAL);
            cfgStage.setTitle("Konfiguracja bazy danych");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pl/domator/fxml/db_config.fxml"));
            cfgStage.setScene(new Scene(loader.load()));
            cfgStage.show();
        } catch (Exception e) {
            LoggerUtils.logError(e);
            statusLabel.setText("Błąd otwarcia konfiguracji bazy.");
        }
    }


    @FXML
    public void openRegister() {
        try {
            Stage regStage = new Stage();
            regStage.initModality(Modality.APPLICATION_MODAL);
            regStage.setTitle("Rejestracja użytkownika");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pl/domator/fxml/register.fxml"));
            regStage.setScene(new Scene(loader.load()));
            regStage.show();
        } catch (Exception e) {
            LoggerUtils.logError(e);
            statusLabel.setText("Błąd otwarcia okna rejestracji.");
        }
    }

    private void openDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pl/domator/fxml/dashboard.fxml"));
            Scene scene = new Scene(loader.load());

            DashboardController dashboardController = loader.getController();
            dashboardController.setUserId(userId);

            Stage dashboardStage = new Stage();
            dashboardStage.setScene(scene);
            dashboardStage.setTitle("Dashboard");
            dashboardStage.show();

            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            currentStage.close();

        } catch (Exception e) {
            LoggerUtils.logError(e);
            statusLabel.setText("Błąd otwarcia dashboard.");
        }
    }
}