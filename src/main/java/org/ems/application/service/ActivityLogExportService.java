package org.ems.application.service;

import org.ems.application.dto.ActivityLogDisplayDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for exporting activity logs
 * Implements Single Responsibility Principle - only handles export logic
 * @author <your group number>
 */
public class ActivityLogExportService {

    /**
     * Convert activity logs to CSV format
     * @param logs List of logs to export
     * @return 2D array where first row is headers, rest are data rows
     */
    public String[][] convertToCSVData(List<ActivityLogDisplayDTO> logs) {
        long start = System.currentTimeMillis();
        System.out.println("📤 [ActivityLogExportService] Converting logs to CSV...");

        if (logs == null || logs.isEmpty()) {
            System.out.println("  ℹ No logs to export");
            return new String[1][5];
        }

        // First row is headers
        String[][] data = new String[logs.size() + 1][5];
        data[0] = new String[]{"Timestamp", "User ID", "Action", "Resource", "Description"};

        // Convert logs to data rows
        for (int i = 0; i < logs.size(); i++) {
            ActivityLogDisplayDTO log = logs.get(i);
            data[i + 1] = new String[]{
                    log.getTimestamp(),
                    log.getUserId(),
                    log.getAction(),
                    log.getResource(),
                    log.getDescription()
            };
        }

        System.out.println("  ✓ Converted " + logs.size() + " logs to CSV in " +
                         (System.currentTimeMillis() - start) + "ms");
        return data;
    }

    /**
     * Get CSV headers
     * @return Header row
     */
    public String[] getCSVHeaders() {
        return new String[]{"Timestamp", "User ID", "Action", "Resource", "Description"};
    }

    /**
     * Generate filename for export
     * @return Filename with timestamp
     */
    public String generateFilename() {
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        return "activity_logs_" + timestamp + ".csv";
    }
}

