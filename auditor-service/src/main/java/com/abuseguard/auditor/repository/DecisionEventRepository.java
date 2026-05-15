package com.abuseguard.auditor.repository;

import com.abuseguard.auditor.entity.DecisionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DecisionEventRepository extends JpaRepository<DecisionEventEntity, Long> {
    boolean existsByEventId(UUID eventId);
}