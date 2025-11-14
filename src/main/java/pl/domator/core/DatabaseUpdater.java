package pl.domator.core;

import pl.domator.config.DBConnectionController;
import java.sql.*;

public class DatabaseUpdater {

    public static final int LATEST_VERSION = 2;

    public static void updateIfNeeded() {
        try (Connection conn = DBConnectionController.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT db_version FROM dmt.db_version ORDER BY id DESC LIMIT 1");
            rs.next();
            int currentVersion = rs.getInt("db_version");

            if (currentVersion < 1) {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS dmt.loans (
                        id SERIAL PRIMARY KEY,
                        user_id INT NOT NULL REFERENCES dmt.users(id) ON DELETE CASCADE,
                        bank_name VARCHAR(100) NOT NULL,
                        loan_subject VARCHAR(255) NOT NULL,
                        total_amount NUMERIC(15,2) NOT NULL,
                        paid_amount NUMERIC(15,2) DEFAULT 0,
                        remaining_amount NUMERIC(15,2) DEFAULT 0,
                        monthly_payment NUMERIC(15,2) NOT NULL,
                        overpayment_sum NUMERIC(15,2) DEFAULT 0,
                        due_date DATE,
                        overpayment_same_account BOOLEAN DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                """);

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS dmt.loan_payments (
                        id SERIAL PRIMARY KEY,
                        loan_id INT NOT NULL REFERENCES dmt.loans(id) ON DELETE CASCADE,
                        payment_date DATE NOT NULL DEFAULT CURRENT_DATE,
                        amount NUMERIC(15,2) NOT NULL,
                        payment_type VARCHAR(20) CHECK (payment_type IN ('rata', 'nadpłata')) NOT NULL DEFAULT 'rata',
                        note VARCHAR(255)
                    );
                """);

                stmt.executeUpdate("""
                    UPDATE dmt.db_version
                    SET db_version = 1,
                        app_version = '1.1.0',
                        last_update = NOW()
                    WHERE id = (SELECT id FROM dmt.db_version ORDER BY id DESC LIMIT 1)
                """);
            }

            if (currentVersion < 2) {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS dmt.vehicles (
                        id SERIAL PRIMARY KEY,
                        user_id INT NOT NULL REFERENCES dmt.users(id) ON DELETE CASCADE,
                        brand VARCHAR(100) NOT NULL,
                        model VARCHAR(100) NOT NULL,
                        production_year INT,
                        registration_number VARCHAR(20) UNIQUE,
                        vin VARCHAR(50) UNIQUE,
                        fuel_type VARCHAR(30),
                        current_mileage INT DEFAULT 0,
                        notes TEXT
                    );
                """);

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS dmt.vehicle_technical_inspections (
                        id SERIAL PRIMARY KEY,
                        vehicle_id INT NOT NULL REFERENCES dmt.vehicles(id) ON DELETE CASCADE,
                        inspection_date DATE NOT NULL,
                        valid_until DATE NOT NULL,
                        mileage_at_inspection INT,
                        cost DECIMAL(10,2),
                        notes TEXT
                    );
                """);

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS dmt.vehicle_insurance_policies (
                        id SERIAL PRIMARY KEY,
                        vehicle_id INT NOT NULL REFERENCES dmt.vehicles(id) ON DELETE CASCADE,
                        policy_type VARCHAR(10) NOT NULL CHECK (policy_type IN ('OC', 'AC', 'OC+AC')),
                        insurer_name VARCHAR(100) NOT NULL,
                        policy_number VARCHAR(50),
                        start_date DATE NOT NULL,
                        end_date DATE NOT NULL,
                        cost DECIMAL(10,2),
                        notes TEXT
                    );
                """);

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS dmt.vehicle_service_history (
                        id SERIAL PRIMARY KEY,
                        vehicle_id INT NOT NULL REFERENCES dmt.vehicles(id) ON DELETE CASCADE,
                        service_date DATE NOT NULL,
                        mileage_at_service INT,
                        cost DECIMAL(10,2),
                        description TEXT NOT NULL
                    );
                """);

                stmt.executeUpdate("""
                    UPDATE dmt.db_version
                    SET db_version = 2,
                        app_version = '1.2.0',
                        last_update = NOW()
                    WHERE id = (SELECT id FROM dmt.db_version ORDER BY id DESC LIMIT 1)
                """);
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