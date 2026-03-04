package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Query(value = "SELECT id, first_name, last_name, ldap, start_date, team, new_level, lead, program_manager, vendor, email, status, lwd_ml_start_date, process, resignation_date, role_change_effective_date, level_before_change, level_after_change, last_billing_date, backfill_ldap, billing_start_date, language, tenure_till_date, level, inactive_reason, NULL as profile_pic, parent, is_deleted, pnse_program, location, shift, inactive FROM Employee WHERE ldap = :ldap AND status = 'Active'", nativeQuery = true)
    Optional<Employee> findByLdap(@Param("ldap") String ldap);

    boolean existsByLdap(String ldap);

    @Query(value = "SELECT id, first_name, last_name, ldap, start_date, team, new_level, lead, program_manager, vendor, email, status, lwd_ml_start_date, process, resignation_date, role_change_effective_date, level_before_change, level_after_change, last_billing_date, backfill_ldap, billing_start_date, language, tenure_till_date, level, inactive_reason, NULL as profile_pic, parent, is_deleted, pnse_program, location, shift, inactive FROM Employee WHERE lead = :leadLdap AND status = 'Active'", nativeQuery = true)
    List<Employee> findByLead(@Param("leadLdap") String leadLdap);

    @Query(value = "SELECT id, first_name, last_name, ldap, start_date, team, new_level, lead, program_manager, vendor, email, status, lwd_ml_start_date, process, resignation_date, role_change_effective_date, level_before_change, level_after_change, last_billing_date, backfill_ldap, billing_start_date, language, tenure_till_date, level, inactive_reason, NULL as profile_pic, parent, is_deleted, pnse_program, location, shift, inactive FROM Employee WHERE program_manager = :programManagerLdap AND status = 'Active'", nativeQuery = true)
    List<Employee> findByProgramManager(@Param("programManagerLdap") String programManagerLdap);

    // Native SQL queries to completely exclude LOB fields
    @Query(value = "SELECT id, first_name, last_name, ldap, start_date, team, new_level, lead, program_manager, vendor, email, status, lwd_ml_start_date, process, resignation_date, role_change_effective_date, level_before_change, level_after_change, last_billing_date, backfill_ldap, billing_start_date, language, tenure_till_date, level, inactive_reason, NULL as profile_pic, parent, is_deleted, pnse_program, location, shift, inactive FROM Employee", nativeQuery = true)
    List<Object[]> findAllExcludingLobsNative();

    @Query(value = "SELECT id, first_name, last_name, ldap, start_date, team, new_level, lead, program_manager, vendor, email, status, lwd_ml_start_date, process, resignation_date, role_change_effective_date, level_before_change, level_after_change, last_billing_date, backfill_ldap, billing_start_date, language, tenure_till_date, level, inactive_reason, NULL as profile_pic, parent, is_deleted, pnse_program, location, shift, inactive FROM Employee WHERE lead = :leadLdap AND status = 'Active'", nativeQuery = true)
    List<Object[]> findByLeadExcludingLobsNative(@Param("leadLdap") String leadLdap);

    @Query(value = "SELECT id, first_name, last_name, ldap, start_date, team, new_level, lead, program_manager, vendor, email, status, lwd_ml_start_date, process, resignation_date, role_change_effective_date, level_before_change, level_after_change, last_billing_date, backfill_ldap, billing_start_date, language, tenure_till_date, level, inactive_reason, NULL as profile_pic, parent, is_deleted, pnse_program, location, shift, inactive FROM Employee WHERE id = :id AND status = 'Active'", nativeQuery = true)
    Employee findByIdExcludingLobsNative(@Param("id") Long id);

    @Query(value = "SELECT id, first_name, last_name, ldap, start_date, team, new_level, lead, program_manager, vendor, email, status, lwd_ml_start_date, process, resignation_date, role_change_effective_date, level_before_change, level_after_change, last_billing_date, backfill_ldap, billing_start_date, language, tenure_till_date, level, inactive_reason, NULL as profile_pic, parent, is_deleted, pnse_program, location, shift, inactive FROM Employee WHERE id IN :userIds AND status = 'Active'", nativeQuery = true)
    List<Object[]> findAllByIdExcludingLobsNative(@Param("userIds") List<Long> userIds);

    @Query("SELECT e.ldap FROM Employee e WHERE e.lead = :leadLdap AND e.status = 'Active'")
    List<String> findLdapsByLead(@Param("leadLdap") String leadLdap);

    @Query("SELECT e.ldap FROM Employee e WHERE e.programManager = :managerLdap AND e.status = 'Active'")
    List<String> findLdapsByManager(@Param("managerLdap") String managerLdap);

    @Query("SELECT e.ldap FROM Employee e WHERE e.status = 'Active'")
    List<String> findAllLdaps();

    List<Employee> findByPnseProgramAndLevelIgnoreCase(String pnseProgram, String level);

    List<Employee> findByLdapIn(List<String> ldaps);

    List<Employee> findByEmailIn(List<String> emails);

    Optional<Employee> findByEmail(String email);

    // Filter Options Queries
    @Query("SELECT DISTINCT e.team FROM Employee e WHERE e.team IS NOT NULL AND e.status = 'Active' ORDER BY e.team")
    List<String> findDistinctTeams();

    @Query("SELECT DISTINCT e.process FROM Employee e WHERE e.process IS NOT NULL AND e.status = 'Active' ORDER BY e.process")
    List<String> findDistinctProjects();

    @Query("SELECT DISTINCT e.pnseProgram FROM Employee e WHERE e.pnseProgram IS NOT NULL AND e.pnseProgram != 'NA' AND e.status = 'Active' ORDER BY e.pnseProgram")
    List<String> findDistinctPrograms();

    @Query("SELECT DISTINCT e.programManager FROM Employee e WHERE e.programManager IS NOT NULL AND e.status = 'Active' ORDER BY e.programManager")
    List<String> findDistinctManagers();

    @Query("SELECT DISTINCT e.lead FROM Employee e WHERE e.lead IS NOT NULL AND e.status = 'Active' ORDER BY e.lead")
    List<String> findDistinctLeads();

    // List-based queries for Delegation support
    @Query(value = "SELECT id, first_name, last_name, ldap, start_date, team, new_level, lead, program_manager, vendor, email, status, lwd_ml_start_date, process, resignation_date, role_change_effective_date, level_before_change, level_after_change, last_billing_date, backfill_ldap, billing_start_date, language, tenure_till_date, level, inactive_reason, NULL as profile_pic, parent, is_deleted, pnse_program, location, shift, inactive FROM Employee WHERE lead IN :leads AND status = 'Active'", nativeQuery = true)
    List<Employee> findByLeadIn(@Param("leads") List<String> leads);

    @Query(value = "SELECT id, first_name, last_name, ldap, start_date, team, new_level, lead, program_manager, vendor, email, status, lwd_ml_start_date, process, resignation_date, role_change_effective_date, level_before_change, level_after_change, last_billing_date, backfill_ldap, billing_start_date, language, tenure_till_date, level, inactive_reason, NULL as profile_pic, parent, is_deleted, pnse_program, location, shift, inactive FROM Employee WHERE program_manager IN :managers AND status = 'Active'", nativeQuery = true)
    List<Employee> findByProgramManagerIn(@Param("managers") List<String> managers);

    @Query("SELECT e.ldap FROM Employee e WHERE e.lead IN :leads AND e.status = 'Active'")
    List<String> findLdapsByLeadIn(@Param("leads") List<String> leads);

    @Query("SELECT e.ldap FROM Employee e WHERE e.programManager IN :managers AND e.status = 'Active'")
    List<String> findLdapsByManagerIn(@Param("managers") List<String> managers);

    // Status-based queries for Weekly Time Entry Defaulters Report
    @Query(value = "SELECT id, first_name, last_name, ldap, start_date, team, new_level, lead, program_manager, vendor, email, status, lwd_ml_start_date, process, resignation_date, role_change_effective_date, level_before_change, level_after_change, last_billing_date, backfill_ldap, billing_start_date, language, tenure_till_date, level, inactive_reason, NULL as profile_pic, parent, is_deleted, pnse_program, location, shift, inactive FROM Employee WHERE status = :status", nativeQuery = true)
    List<Employee> findByStatus(@Param("status") String status);

    @Query(value = "SELECT id, first_name, last_name, ldap, start_date, team, new_level, lead, program_manager, vendor, email, status, lwd_ml_start_date, process, resignation_date, role_change_effective_date, level_before_change, level_after_change, last_billing_date, backfill_ldap, billing_start_date, language, tenure_till_date, level, inactive_reason, NULL as profile_pic, parent, is_deleted, pnse_program, location, shift, inactive FROM Employee WHERE status = 'Active' AND lead = :leadLdap", nativeQuery = true)
    List<Employee> findActiveEmployeesForLead(@Param("leadLdap") String leadLdap);

    @Query(value = "SELECT id, first_name, last_name, ldap, start_date, team, new_level, lead, program_manager, vendor, email, status, lwd_ml_start_date, process, resignation_date, role_change_effective_date, level_before_change, level_after_change, last_billing_date, backfill_ldap, billing_start_date, language, tenure_till_date, level, inactive_reason, NULL as profile_pic, parent, is_deleted, pnse_program, location, shift, inactive FROM Employee WHERE status = 'Active' AND program_manager = :managerLdap", nativeQuery = true)
    List<Employee> findActiveEmployeesForManager(@Param("managerLdap") String managerLdap);
}