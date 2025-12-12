package org.ems.application.service.schedule;

import org.ems.domain.model.Session;
import org.ems.domain.repository.SessionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ScheduleServiceImpl implements ScheduleService {

    private final SessionRepository sessionRepo;

    public ScheduleServiceImpl(SessionRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    private boolean overlaps(LocalDateTime s1, LocalDateTime e1,
                             LocalDateTime s2, LocalDateTime e2) {
        return s1.isBefore(e2) && s2.isBefore(e1);
    }

    @Override
    public boolean hasPresenterConflict(UUID pid, Session newS) {
        List<Session> all = sessionRepo.findAll();
        for (Session s : all) {
            if (s.getPresenterIds().contains(pid)) {
                if (overlaps(s.getStart(), s.getEnd(), newS.getStart(), newS.getEnd())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasAttendeeConflict(UUID aid, Session newS) {
        return false;
    }
}
