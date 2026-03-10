package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.TimeSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeSheetRepository extends JpaRepository<TimeSheet, Long> {
    // Custom queries can be added here if needed
    List<TimeSheet> findByLdap(String ldap);
    List<TimeSheet> findByDate(String date);
    List<TimeSheet> findByDateBetween(String startDate, String endDate);
    List<TimeSheet> findByTeam(String team);
    List<TimeSheet> findByProject(String project);
}
