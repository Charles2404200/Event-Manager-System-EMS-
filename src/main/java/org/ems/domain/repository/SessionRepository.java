package org.ems.domain.repository;

import org.ems.domain.model.Session;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SessionRepository {

    Session save(Session session);

    void delete(UUID id);

    Session findById(UUID id);

    List<Session> findAll();

    List<Session> findByEvent(UUID eventId);

    List<Session> findByDate(LocalDate date);

    // Presenter assignment
    void assignPresenter(UUID sessionId, UUID presenterId);

    void clearPresenters(UUID sessionId);
}
