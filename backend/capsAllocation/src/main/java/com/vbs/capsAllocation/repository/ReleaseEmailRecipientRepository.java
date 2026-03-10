package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.ReleaseEmailRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReleaseEmailRecipientRepository extends JpaRepository<ReleaseEmailRecipient, Long> {

    // Find all active recipients
    List<ReleaseEmailRecipient> findByIsActiveTrueOrderByNameAsc();

    // Find by email
    Optional<ReleaseEmailRecipient> findByEmail(String email);

    // Check if email exists
    boolean existsByEmail(String email);
}
