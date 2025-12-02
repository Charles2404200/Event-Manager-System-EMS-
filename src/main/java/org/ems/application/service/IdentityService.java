package org.ems.application.service;

/**
 * @author <your group number>
 */

import java.util.UUID;
import java.util.List;
import org.ems.domain.model.Person;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Presenter;
public interface IdentityService {

    // Attendee
    Attendee createAttendee(Attendee a);
    Attendee updateAttendee(Attendee a);
    Attendee getAttendee(UUID id);
    boolean deleteAttendee(UUID id);
    // Authentication
    Person login(String email, String password);

    Presenter createPresenter(Presenter p);
    Presenter updatePresenter(Presenter p);
    Presenter getPresenter(UUID id);
    boolean deletePresenter(UUID id);
    List<Presenter> searchPresenters(String name);
    List<Presenter> getAllPresenters();
    List<Attendee> getAllAttendees();
    Person getUserById(UUID id);

}
