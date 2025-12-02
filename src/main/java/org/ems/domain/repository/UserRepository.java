package org.ems.domain.repository;

import org.ems.domain.model.Person;

import java.util.List;
import java.util.UUID;

public interface UserRepository {

    Person findById(UUID id);

    Person findByUsername(String username);

    List<Person> findAll();

    Person save(Person user);

    void delete(UUID id);

    boolean existsByUsername(String username);
}
