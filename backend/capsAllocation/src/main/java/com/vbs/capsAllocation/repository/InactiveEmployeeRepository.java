package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.InactiveEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InactiveEmployeeRepository extends JpaRepository<InactiveEmployee, Long> {
    Optional<InactiveEmployee> findByLdap(String ldap);
    List<InactiveEmployee> findByLead(String lead);

    // Native SQL queries to completely exclude LOB fields
    @Query(value = "SELECT id, first_name, last_name, ldap, team, new_level, lead, program_manager, vendor, email, status, process, level_before_change, level_after_change, backfill_ldap, language, tenure_till_date, level, parent, inactive_reason, pnse_program, start_date, role_change_effective_date, last_billing_date, billing_start_date, lwd_ml_start_date, shift, location, resignation_date, deleted_at, inactive FROM inactive_employees", nativeQuery = true)
    List<Object[]> findAllExcludingLobsNative();

    @Query(value = "SELECT id, first_name, last_name, ldap, team, new_level, lead, program_manager, vendor, email, status, process, level_before_change, level_after_change, backfill_ldap, language, tenure_till_date, level, parent, inactive_reason, pnse_program, start_date, role_change_effective_date, last_billing_date, billing_start_date, lwd_ml_start_date, shift, location, resignation_date, deleted_at, inactive FROM inactive_employees WHERE lead = :lead", nativeQuery = true)
    List<Object[]> findByLeadExcludingLobsNative(@Param("lead") String lead);

    @Query(value = "SELECT id, first_name, last_name, ldap, team, new_level, lead, program_manager, vendor, email, status, process, level_before_change, level_after_change, backfill_ldap, language, tenure_till_date, level, parent, inactive_reason, pnse_program, start_date, role_change_effective_date, last_billing_date, billing_start_date, lwd_ml_start_date, shift, location, resignation_date, deleted_at, inactive FROM inactive_employees WHERE id = :id", nativeQuery = true)
    InactiveEmployee findByIdExcludingLobsNative(@Param("id") Long id);
}