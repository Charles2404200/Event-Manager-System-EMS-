package org.ems.domain.repository;

import org.ems.domain.model.Attendee;
import java.util.List;
import java.util.UUID;

public interface AttendeeRepository {

    Attendee save(Attendee attendee);

    void delete(UUID id);

    Attendee findById(UUID id);

    List<Attendee> findAll();

    // mapping tables
    void registerEvent(UUID attendeeId, UUID eventId);

    void registerSession(UUID attendeeId, UUID sessionId);

    void unregisterEvent(UUID attendeeId, UUID eventId);

    void unregisterSession(UUID attendeeId, UUID sessionId);
}
