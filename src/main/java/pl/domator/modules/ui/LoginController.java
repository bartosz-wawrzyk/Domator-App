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

        Stage loginStage = (Stage) loginButton.getScene().getWindow();

        Task<Void> versionTask = new Task<>() {
            @Override
            protected Void call() {
                try (Connection conn = DBConnectionController.getConnection()) {

                    int currentVersion = DatabaseUpdater.getCurrentVersion(conn);
                    int latestVersion = DatabaseUpdater.getLatestVersion();

                    if (currentVersion < latestVersion) {

                        runOnFxThread(() -> {
                            loginStage.close();

                            Stage updateStage = showUpdateDialog();

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
                                    } catch (InterruptedException ignored) {}

                                    runOnFxThread(() -> {
                                        updateStage.close();
                                        openDashboard();
                                    });

                                }).start();
                            });

                            updateTask.setOnFailed(ev -> {
                                runOnFxThread(() -> {
                                    updateStage.close();
                                    LoggerUtils.logError((Exception) updateTask.getException());
                                });
                            });

                            new Thread(updateTask).start();
                        });

                    } else {
                        runOnFxThread(() -> {
                            loginStage.close();
                            openDashboard();
                        });
                    }

                } catch (Exception ex) {
                    LoggerUtils.logError(ex);
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
            cfgStage.setResizable(false);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pl/domator/fxml/DBConfig.fxml"));
            cfgStage.setScene(new Scene(loader.load()));
            cfgStage.show();
        } catch (Exception e) {
            LoggerUtils.logError(e);
        }
    }


    @FXML
    public void openRegister() {
        try {
            Stage regStage = new Stage();
            regStage.initModality(Modality.APPLICATION_MODAL);
            regStage.setTitle("Rejestracja użytkownika");
            regStage.setResizable(false);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pl/domator/fxml/Register.fxml"));
            regStage.setScene(new Scene(loader.load()));
            regStage.show();
        } catch (Exception e) {
            LoggerUtils.logError(e);
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
        }
    }

    private Stage showUpdateDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pl/domator/fxml/UpdateDialog.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.setTitle("Aktualizacja");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setScene(scene);
            stage.show();

            return stage;

        } catch (Exception e) {
            LoggerUtils.logError(e);
            return null;
        }
    }

}