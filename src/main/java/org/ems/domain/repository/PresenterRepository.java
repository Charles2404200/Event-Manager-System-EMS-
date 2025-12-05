package org.ems.domain.repository;

import org.ems.domain.model.Presenter;

import java.util.List;
import java.util.UUID;

public interface PresenterRepository {

    Presenter save(Presenter presenter);

    void delete(UUID id);

    Presenter findById(UUID id);

    Presenter findByUsername(String username);

    List<Presenter> findAll();

    List<Presenter> findByEvent(UUID eventId);

    List<Presenter> findBySession(UUID sessionId);

    void assignToSession(UUID presenterId, UUID sessionId);

    void removeFromSession(UUID presenterId, UUID sessionId);

    void clearSessions(UUID presenterId);

    /**
     * Returns total number of presenters.
     */
    long count();
}
