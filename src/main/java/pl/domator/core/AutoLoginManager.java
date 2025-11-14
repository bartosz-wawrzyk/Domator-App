package pl.domator.core;

import java.io.*;
import java.util.Properties;

public class AutoLoginManager {

    private static final String FILE_PATH = "autologin.properties";

    public static void saveCredentials(String username, String password) {
        try (OutputStream out = new FileOutputStream(FILE_PATH)) {
            Properties props = new Properties();
            props.setProperty("username", username);
            props.setProperty("password", password);
            props.store(out, "Autologin credentials");
        } catch (IOException e) {
            LoggerUtils.logError(e);
        }
    }

    public static String[] loadCredentials() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return null;
        try (InputStream in = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(in);
            String username = props.getProperty("username");
            String password = props.getProperty("password");
            if (username != null && password != null) {
                return new String[]{username, password};
            }
        } catch (IOException e) {
            LoggerUtils.logError(e);
        }
        return null;
    }

    public static void clearCredentials() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            file.delete();
        }
    }
}
