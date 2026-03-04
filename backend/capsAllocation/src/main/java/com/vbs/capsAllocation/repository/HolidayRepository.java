package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    
    /**
     * Find holiday by date and active status
     */
    Optional<Holiday> findByHolidayDateAndIsActive(LocalDate holidayDate, Boolean isActive);
    
    /**
     * Find all active holidays
     */
    List<Holiday> findByIsActiveOrderByHolidayDateAsc(Boolean isActive);
    
    /**
     * Find holidays within a date range
     */
    @Query("SELECT h FROM Holiday h WHERE h.holidayDate BETWEEN :startDate AND :endDate AND h.isActive = :isActive ORDER BY h.holidayDate ASC")
    List<Holiday> findHolidaysBetweenDates(@Param("startDate") LocalDate startDate, 
                                          @Param("endDate") LocalDate endDate, 
                                          @Param("isActive") Boolean isActive);
    
    /**
     * Find holidays by type and active status
     */
    List<Holiday> findByHolidayTypeAndIsActiveOrderByHolidayDateAsc(String holidayType, Boolean isActive);
    
    /**
     * Check if a specific date is a holiday
     */
    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END FROM Holiday h WHERE h.holidayDate = :date AND h.isActive = true")
    boolean isHoliday(@Param("date") LocalDate date);
    
    /**
     * Find holidays for current year
     */
    @Query("SELECT h FROM Holiday h WHERE YEAR(h.holidayDate) = :year AND h.isActive = :isActive ORDER BY h.holidayDate ASC")
    List<Holiday> findHolidaysByYear(@Param("year") int year, @Param("isActive") Boolean isActive);
    
    /**
     * Delete all holidays by type (used for CSV replacement)
     */
    void deleteByHolidayType(String holidayType);
}
