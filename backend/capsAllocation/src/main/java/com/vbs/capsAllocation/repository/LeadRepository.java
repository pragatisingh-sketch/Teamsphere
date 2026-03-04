package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.LeadsRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<LeadsRequest, Long> {

    List<LeadsRequest> findByLdapAndRequestType(String ldap, String requestType);

    List<LeadsRequest> findByRequestedBy(String loggedInuser);

    List<LeadsRequest> findByLdap(String ldap);

    // Native SQL query to completely exclude LOB fields
    @Query(value = "SELECT id, status, requested_by, request_type, ldap, requested_at, is_sign_up, employee_data_key FROM leads_request", nativeQuery = true)
    List<Object[]> findAllExcludingLobsNative();
}