package org.ems.application.service.activity;

import org.ems.application.dto.activity.ActivityLogDisplayDTO;

import java.util.List;

/**
 * Service for exporting activity logs
 * Implements Single Responsibility Principle - only handles export logic
 * @author <your group number>
 */
public interface ActivityLogExportService {

    /**
     * Convert activity logs to CSV format
     * @param logs List of logs to export
     * @return 2D array where first row is headers, rest are data rows
     */
    String[][] convertToCSVData(List<ActivityLogDisplayDTO> logs);

    /**
     * Get CSV headers
     * @return Header row
     */
    String[] getCSVHeaders();

    /**
     * Generate filename for export
     * @return Filename with timestamp
     */
    String generateFilename();
}

