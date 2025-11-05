package pl.domator.core;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerUtils {

    private static final String LOG_FILE = "bledy.txt";

    public static void logError(Exception e) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            pw.println("[" + timestamp + "] Wystąpił błąd:");
            e.printStackTrace(pw);
            pw.println();

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public static void logMessage(String message) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            pw.println("[" + timestamp + "] " + message);
            pw.println();

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
