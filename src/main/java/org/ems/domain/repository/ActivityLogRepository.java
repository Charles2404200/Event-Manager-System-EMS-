package org.ems.domain.repository;

import org.ems.domain.model.ActivityLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ActivityLogRepository {

    void save(ActivityLog log);

    ActivityLog findById(UUID id);

    List<ActivityLog> findAll();

    List<ActivityLog> findPage(int offset, int pageSize);

    List<ActivityLog> findByAction(String action);

    List<ActivityLog> findByUserId(String userId);

    List<ActivityLog> findByResource(String resource);

    List<ActivityLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    long count();

    void deleteById(UUID id);

    void deleteAll();

    void deleteByDateBefore(LocalDateTime date);
}

