package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.VunnoAuditLog;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@Repository
public interface VunnoAuditRepository extends JpaRepository<VunnoAuditLog, String> {

    /**
     * Find all audit logs for a specific leave request, ordered by most recent
     * first
     */
    List<VunnoAuditLog> findByVunnoResponseIdOrderByChangedAtDesc(Long vunnoResponseId);
}
