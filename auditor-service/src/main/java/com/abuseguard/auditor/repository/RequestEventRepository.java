package com.abuseguard.auditor.repository;

import com.abuseguard.auditor.entity.RequestEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RequestEventRepository extends JpaRepository<RequestEventEntity, Long> {
    boolean existsByEventId(UUID eventId);
}