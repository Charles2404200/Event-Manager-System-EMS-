package org.ems.ui.stage;

import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FxUtils {

    public static boolean exportToCSV(String filePath, List<String[]> data, String[] headers) {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            StringBuilder csv = new StringBuilder();

            for (String header : headers) {
                csv.append("\"").append(header).append("\",");
            }
            csv.deleteCharAt(csv.length() - 1);
            csv.append("\n");

            for (String[] row : data) {
                for (String cell : row) {
                    csv.append("\"").append(cell != null ? cell : "").append("\",");
                }
                csv.deleteCharAt(csv.length() - 1);
                csv.append("\n");
            }

            fos.write(csv.toString().getBytes());
            System.out.println("CSV exported to: " + filePath);
            return true;
        } catch (Exception e) {
            System.err.println("Error exporting CSV: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static String generateFileName(String prefix, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return prefix + "_" + timestamp + "." + extension;
    }
}
