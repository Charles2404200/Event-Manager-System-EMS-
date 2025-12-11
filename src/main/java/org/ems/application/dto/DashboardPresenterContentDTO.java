package org.ems.application.dto;

/**
 * DTO for presenter dashboard content
 * @author <your group number>
 */
public class DashboardPresenterContentDTO {
    private final long assignedSessionsCount;

    public DashboardPresenterContentDTO(long assignedSessionsCount) {
        this.assignedSessionsCount = assignedSessionsCount;
    }

    public long getAssignedSessionsCount() { return assignedSessionsCount; }

    @Override
    public String toString() {
        return "DashboardPresenterContentDTO{" +
                "assignedSessionsCount=" + assignedSessionsCount +
                '}';
    }
}

