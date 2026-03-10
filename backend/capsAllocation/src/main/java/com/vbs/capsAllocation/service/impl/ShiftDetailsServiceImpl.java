package com.vbs.capsAllocation.service.impl;

import com.vbs.capsAllocation.model.ShiftDetails;
import com.vbs.capsAllocation.repository.ShiftDetailsRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service

public class ShiftDetailsServiceImpl {
    private final ShiftDetailsRepository shiftDetailsRepository;

    public ShiftDetailsServiceImpl(ShiftDetailsRepository repo) { this.shiftDetailsRepository = repo; }

    @Cacheable(value = "shiftDetails", key = "#code")
    public ShiftDetails getShift(String code) {
        return shiftDetailsRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Shift not found for code: " + code));
    }
}