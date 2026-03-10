package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.dto.CreateHolidayDTO;
import com.vbs.capsAllocation.dto.HolidayDTO;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for managing holidays.
 */
public interface HolidayService {

    /**
     * Create a new holiday.
     */
    HolidayDTO createHoliday(CreateHolidayDTO createHolidayDTO, String createdBy);

    /**
     * Get all active holidays.
     */
    List<HolidayDTO> getAllActiveHolidays();

    /**
     * Get holidays by year.
     */
    List<HolidayDTO> getHolidaysByYear(int year);

    /**
     * Get holidays within a date range.
     */
    List<HolidayDTO> getHolidaysBetweenDates(LocalDate startDate, LocalDate endDate);

    /**
     * Check if a specific date is a holiday.
     */
    boolean isHoliday(LocalDate date);

    /**
     * Get holiday by date.
     */
    HolidayDTO getHolidayByDate(LocalDate date);

    /**
     * Update holiday by ID.
     */
    HolidayDTO updateHoliday(Long holidayId, CreateHolidayDTO updateHolidayDTO, String updatedBy);

    /**
     * Delete holiday by ID.
     */
    void deleteHoliday(Long holidayId);

    /**
     * Upload holidays from CSV file.
     */
    List<HolidayDTO> uploadHolidaysFromCSV(MultipartFile file, String uploadedBy);

    /**
     * Get holidays by type.
     */
    List<HolidayDTO> getHolidaysByType(String holidayType);
}
