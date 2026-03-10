package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.ShiftDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShiftDetailsRepository extends JpaRepository<ShiftDetails, Long> {
    Optional<ShiftDetails> findByCode(String code);

}

