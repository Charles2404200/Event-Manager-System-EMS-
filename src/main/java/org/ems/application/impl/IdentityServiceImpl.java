package org.ems.application.impl;

import org.ems.application.service.IdentityService;
import org.ems.config.DatabaseConfig;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Person;
import org.ems.domain.model.Presenter;
import org.ems.domain.repository.AttendeeRepository;
import org.ems.domain.repository.PresenterRepository;

import java.util.List;
import java.util.UUID;

/**
 * @author <your group number>
 */
public class IdentityServiceImpl implements IdentityService {

    private final AttendeeRepository attendeeRepo;
    private final PresenterRepository presenterRepo;

    public IdentityServiceImpl(
            AttendeeRepository attendeeRepo,
            PresenterRepository presenterRepo
    ) {
        this.attendeeRepo = attendeeRepo;
        this.presenterRepo = presenterRepo;
    }

    // ATTENDEE
    @Override
    public Attendee createAttendee(Attendee a) {
        return attendeeRepo.save(a);
    }

    @Override
    public Attendee updateAttendee(Attendee a) {
        return attendeeRepo.save(a);
    }

    @Override
    public Attendee getAttendee(UUID id) {
        return attendeeRepo.findById(id);
    }

    @Override
    public boolean deleteAttendee(UUID id) {
        attendeeRepo.delete(id);
        return true;
    }

    @Override
    public Person login(String email, String password) {
        // Try to match by email or username in attendees
        try {
            if (attendeeRepo != null) {
                List<Attendee> attendees = attendeeRepo.findAll();
                for (Attendee attendee : attendees) {
                    if (attendee.getEmail() != null && attendee.getEmail().equals(email)) {
                        if (attendee.getPasswordHash() != null && attendee.getPasswordHash().equals(password)) {
                            return attendee;
                        }
                    }
                    if (attendee.getUsername() != null && attendee.getUsername().equals(email)) {
                        if (attendee.getPasswordHash() != null && attendee.getPasswordHash().equals(password)) {
                            return attendee;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error querying attendees: " + e.getMessage());
        }

        // Try to match by email or username in presenters
        try {
            if (presenterRepo != null) {
                List<Presenter> presenters = presenterRepo.findAll();
                for (Presenter presenter : presenters) {
                    if (presenter.getEmail() != null && presenter.getEmail().equals(email)) {
                        if (presenter.getPasswordHash() != null && presenter.getPasswordHash().equals(password)) {
                            return presenter;
                        }
                    }
                    if (presenter.getUsername() != null && presenter.getUsername().equals(email)) {
                        if (presenter.getPasswordHash() != null && presenter.getPasswordHash().equals(password)) {
                            return presenter;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error querying presenters: " + e.getMessage());
        }

        // Fallback: Try to find admin or other system users directly from persons table
        try {
            String query = "SELECT * FROM persons WHERE (email = ? OR username = ?) AND password_hash = ?";
            java.sql.Connection conn = DatabaseConfig.getConnection();
            try (java.sql.PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, email);
                ps.setString(2, email);
                ps.setString(3, password);
                java.sql.ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    // Create a generic Person object for admin/system users
                    Person p = new Person() {};
                    p.setId((java.util.UUID) rs.getObject("id"));
                    p.setFullName(rs.getString("full_name"));
                    p.setDateOfBirth(rs.getObject("dob", java.time.LocalDate.class));
                    p.setEmail(rs.getString("email"));
                    p.setPhone(rs.getString("phone"));
                    p.setUsername(rs.getString("username"));
                    p.setPasswordHash(rs.getString("password_hash"));
                    p.setRole(org.ems.domain.model.enums.Role.valueOf(rs.getString("role")));
                    return p;
                }
            }
        } catch (Exception e) {
            System.err.println("Error querying persons table for admin: " + e.getMessage());
        }

        return null; // Login failed
    }

    // ...existing code...
    @Override
    public Presenter createPresenter(Presenter p) {
        return presenterRepo.save(p);
    }

    @Override
    public Presenter updatePresenter(Presenter p) {
        return presenterRepo.save(p);
    }

    @Override
    public Presenter getPresenter(UUID id) {
        return presenterRepo.findById(id);
    }

    @Override
    public boolean deletePresenter(UUID id) {
        presenterRepo.delete(id);
        return true;
    }

    @Override
    public List<Presenter> searchPresenters(String name) {
        return presenterRepo.findAll().stream()
                .filter(p -> p.getFullName().toLowerCase().contains(name.toLowerCase()))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Presenter> getAllPresenters() {
        return presenterRepo.findAll();
    }

    @Override
    public List<Attendee> getAllAttendees() {
        return attendeeRepo.findAll();
    }

    @Override
    public Person getUserById(UUID id) {
        // Try to get from presenters first
        Presenter presenter = presenterRepo.findById(id);
        if (presenter != null) {
            return presenter;
        }
        // Then try attendees
        Attendee attendee = attendeeRepo.findById(id);
        if (attendee != null) {
            return attendee;
        }
        return null;
    }
}
