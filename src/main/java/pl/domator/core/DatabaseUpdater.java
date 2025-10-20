package pl.domator.core;

import pl.domator.config.DBConnectionController;
import java.sql.*;

public class DatabaseUpdater {

    public static final int LATEST_VERSION = 0;

    public static void updateIfNeeded() {
        try (Connection conn = DBConnectionController.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT db_version FROM dmt.db_version ORDER BY id DESC LIMIT 1");
            rs.next();
            int currentVersion = rs.getInt("db_version");
            /*
            if (currentVersion < 1) {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS dmt.settings (
                        id SERIAL PRIMARY KEY,
                        key VARCHAR(100) UNIQUE NOT NULL,
                        value TEXT
                    );
                """);

                stmt.executeUpdate("""
                    UPDATE dmt.db_version
                    SET db_version = 1,
                        app_version = '1.1.0',
                        last_update = NOW()
                    WHERE id = (SELECT id FROM dmt.db_version ORDER BY id DESC LIMIT 1)
                """);
                currentVersion = 1;
            }

            if (currentVersion < 2) {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS dmt.cars (
                        id SERIAL PRIMARY KEY,
                        brand VARCHAR(100) NOT NULL,
                        model VARCHAR(100) NOT NULL,
                        year INTEGER,
                        created_at TIMESTAMP DEFAULT NOW()
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
            */
        } catch (SQLException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return 0;
    }
}