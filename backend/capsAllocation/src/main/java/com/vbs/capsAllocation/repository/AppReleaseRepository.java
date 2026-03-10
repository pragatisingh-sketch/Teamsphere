package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.AppRelease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppReleaseRepository extends JpaRepository<AppRelease, Long> {

    // Get the latest release by release date
    Optional<AppRelease> findTopByOrderByReleaseDateDesc();

    // Get the latest release by version (semantic versioning)
    Optional<AppRelease> findTopByOrderByVersionDesc();

    // Find releases ordered by release date descending
    List<AppRelease> findAllByOrderByReleaseDateDesc();

    // Check if a version already exists
    boolean existsByVersion(String version);

    // Find release by version
    Optional<AppRelease> findByVersion(String version);

    // Find releases that haven't had notifications sent
    List<AppRelease> findByNotificationSentFalseOrderByReleaseDateDesc();
}
