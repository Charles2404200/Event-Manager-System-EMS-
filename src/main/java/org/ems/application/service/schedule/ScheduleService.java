package org.ems.application.service.schedule;

import org.ems.domain.model.Session;

import java.util.UUID;

public interface ScheduleService {
    boolean hasPresenterConflict(UUID presenterId, Session newSession);
    boolean hasAttendeeConflict(UUID attendeeId, Session newSession);
}
