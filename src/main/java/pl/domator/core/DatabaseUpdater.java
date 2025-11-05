package pl.domator.core;

import pl.domator.config.DBConnectionController;
import java.sql.*;

public class DatabaseUpdater {

    public static final int LATEST_VERSION = 1;

    public static void updateIfNeeded() {
        try (Connection conn = DBConnectionController.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT db_version FROM dmt.db_version ORDER BY id DESC LIMIT 1");
            rs.next();
            int currentVersion = rs.getInt("db_version");

            if (currentVersion < 1) {
               /* stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS dmt.loans (
                    id SERIAL PRIMARY KEY,
                    user_id INT NOT NULL REFERENCES dmt.users(id) ON DELETE CASCADE,
                    bank_name VARCHAR(100) NOT NULL,
                    loan_purpose VARCHAR(255),
                    total_amount NUMERIC(15,2) NOT NULL,
                    total_installments INT NOT NULL,
                    installment_amount NUMERIC(15,2) NOT NULL,
                    due_date DATE,
                    custom_due_date DATE,
                    overpayment_same_account BOOLEAN DEFAULT TRUE,
                    is_paid BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

                stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS dmt.loan_payments (
                    id SERIAL PRIMARY KEY,
                    loan_id INT NOT NULL REFERENCES dmt.loans(id) ON DELETE CASCADE,
                    payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    amount NUMERIC(15,2) NOT NULL,
                    notes VARCHAR(255)
                );
            """);

                stmt.executeUpdate("""
                    UPDATE dmt.db_version
                    SET db_version = 1,
                        app_version = '1.1.0',
                        last_update = NOW()
                    WHERE id = (SELECT id FROM dmt.db_version ORDER BY id DESC LIMIT 1)
                """);*/
            }
        } catch (SQLException e) {
            LoggerUtils.logError(e);
        }
    }

    public static int getLatestVersion() {
        return LATEST_VERSION;
    }

    public static int getCurrentVersion(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT db_version FROM dmt.db_version ORDER BY id DESC LIMIT 1");
            if (rs.next()) {
                return rs.getInt("db_version");
            }
        } catch (SQLException e) {
            LoggerUtils.logError(e);
        }
        return 0;
    }
}