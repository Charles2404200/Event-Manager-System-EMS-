package org.ems.domain.model;

import org.ems.domain.model.enums.Role;
import org.ems.domain.model.enums.PresenterType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Presenter extends Person {

    private PresenterType presenterType;

    private List<UUID> sessionIds = new ArrayList<>();
    private List<UUID> eventIds = new ArrayList<>();
    private String bio;
    private List<String> materialPaths = new ArrayList<>();

    public Presenter() {
        super();
        setRole(Role.PRESENTER);
    }

    public Presenter(String fullName, LocalDate dob,
                     String email, String phone,
                     String username, String passwordHash,
                     PresenterType presenterType, String bio) {

        super(fullName, dob, email, phone, username, passwordHash, Role.PRESENTER);
        this.presenterType = presenterType;
        this.bio = bio;
    }

    public Presenter(String fullName, LocalDate dob,
                     String email, String phone,
                     String username, String passwordHash,
                     String presenterTypeString) {

        super(fullName, dob, email, phone, username, passwordHash, Role.PRESENTER);
        try {
            this.presenterType = PresenterType.valueOf(presenterTypeString);
        } catch (IllegalArgumentException e) {
            this.presenterType = PresenterType.KEYNOTE_SPEAKER;
        }
    }

    // Convenient constructor for signup
    public Presenter(String fullName, LocalDate dob,
                     String email, String phone,
                     String username, String passwordHash) {

        super(fullName, dob, email, phone, username, passwordHash, Role.PRESENTER);
        this.presenterType = PresenterType.KEYNOTE_SPEAKER;
    }

    public PresenterType getPresenterType() { return presenterType; }

    public void setPresenterType(PresenterType presenterType) {
        this.presenterType = presenterType;
    }

    public List<UUID> getSessionIds() { return sessionIds; }
    public void addSession(UUID sessionId) { sessionIds.add(sessionId); }

    public List<UUID> getEventIds() { return eventIds; }
    public void addEvent(UUID eventId) { eventIds.add(eventId); }

    public void uploadMaterial(String path) {
        materialPaths.add(path);
    }

    public List<String> getMaterialPaths() {
        return materialPaths;
    }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
