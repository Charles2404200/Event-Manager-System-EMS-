package org.ems.domain.repository;

import org.ems.domain.model.Event;
import org.ems.domain.model.enums.EventType;

import java.util.List;
import java.util.UUID;

public interface EventRepository {
    Event save(Event event);
    void delete(UUID id);
    Event findById(UUID id);
    List<Event> findAll();
    List<Event> findByType(EventType type);

    /**
     * Returns total number of events.
     */
    long count();

    /**
     * Returns a page of events using offset/limit.
     */
    List<Event> findPage(int offset, int limit);
}
