package org.ems.application.impl;

import org.ems.application.service.EventService;
import org.ems.application.service.ScheduleService;
import org.ems.domain.model.enums.EventType;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Event;
import org.ems.domain.model.Presenter;
import org.ems.domain.model.Session;
import org.ems.domain.repository.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class EventServiceImpl implements EventService {

    private final EventRepository eventRepo;
    private final SessionRepository sessionRepo;
    private final AttendeeRepository attendeeRepo;
    private final PresenterRepository presenterRepo;
    private final ScheduleService scheduleService;

    public EventServiceImpl(
            EventRepository eventRepo,
            SessionRepository sessionRepo,
            AttendeeRepository attendeeRepo,
            PresenterRepository presenterRepo,
            ScheduleService scheduleService
    ) {
        this.eventRepo = eventRepo;
        this.sessionRepo = sessionRepo;
        this.attendeeRepo = attendeeRepo;
        this.presenterRepo = presenterRepo;
        this.scheduleService = scheduleService;
    }

    // EVENT CRUD
    @Override
    public Event createEvent(Event e) { return eventRepo.save(e); }

    @Override
    public Event updateEvent(Event e) { return eventRepo.save(e); }

    @Override
    public Event getEvent(UUID id) { return eventRepo.findById(id); }

    @Override
    public boolean deleteEvent(UUID id) {
        eventRepo.delete(id);
        return true;
    }

    @Override
    public List<Event> getEvents() { return eventRepo.findAll(); }

    @Override
    public List<Event> getEventsByType(String type) {
        return eventRepo.findByType(EventType.valueOf(type));
    }

    // SESSION CRUD
    @Override
    public Session createSession(Session s) {
        // check presenter conflicts
        if (s.getPresenterIds() != null) {
            for (UUID pid : s.getPresenterIds()) {
                if (scheduleService.hasPresenterConflict(pid, s)) {
                    throw new IllegalStateException("Presenter conflict detected.");
                }
            }
        }
        return sessionRepo.save(s);
    }

    @Override
    public Session updateSession(Session s) { return sessionRepo.save(s); }

    @Override
    public Session getSession(UUID id) { return sessionRepo.findById(id); }

    @Override
    public boolean deleteSession(UUID id) {
        sessionRepo.delete(id);
        return true;
    }

    @Override
    public List<Session> getSessions() { return sessionRepo.findAll(); }

    @Override
    public List<Session> getSessionsByDate(LocalDate date) {
        return sessionRepo.findByDate(date);
    }

    // OPERATIONS
    @Override
    public boolean addPresenterToSession(UUID pid, UUID sid) {
        Session s = sessionRepo.findById(sid);
        if (scheduleService.hasPresenterConflict(pid, s)) return false;

        s.addPresenter(pid);
        sessionRepo.save(s);

        return true;
    }

    @Override
    public boolean registerAttendeeToSession(UUID aid, UUID sid) {
        Session s = sessionRepo.findById(sid);
        if (scheduleService.hasAttendeeConflict(aid, s)) return false;

        Attendee a = attendeeRepo.findById(aid);
        a.addSession(sid);
        attendeeRepo.save(a);

        return true;
    }
}
