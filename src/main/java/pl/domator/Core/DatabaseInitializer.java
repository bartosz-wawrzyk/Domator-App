package pl.domator.Core;

import pl.domator.Config.DBConnectionController;
import java.sql.*;

public class DatabaseInitializer {

    public static void initializeDatabase() {
        try (Connection conn = DBConnectionController.getConnection()) {

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT schema_name FROM information_schema.schemata WHERE schema_name='dmt'");
            if (!rs.next()) {
                stmt.executeUpdate("CREATE SCHEMA dmt");
            }

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS dmt.users (
                    id SERIAL PRIMARY KEY,
                    username VARCHAR(50) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS dmt.db_version (
                    id SERIAL PRIMARY KEY,
                    db_version INT NOT NULL,
                    app_version VARCHAR(20),
                    last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    name VARCHAR(50)
                );
            """);

            rs = stmt.executeQuery("SELECT COUNT(*) FROM dmt.db_version");
            rs.next();
            if (rs.getInt(1) == 0) {
                stmt.executeUpdate("""
                    INSERT INTO dmt.db_version (db_version, app_version, name)
                    VALUES (0, '1.0.0', 'Domator');
                """);
            }

        } catch (SQLException e) {
            LoggerUtils.logError(e);
        }
    }
}