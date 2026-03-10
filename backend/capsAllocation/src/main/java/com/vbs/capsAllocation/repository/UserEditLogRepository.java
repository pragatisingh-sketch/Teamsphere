package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.UserEditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserEditLogRepository extends JpaRepository<UserEditLog, Long> {
    
    List<UserEditLog> findByUserLdap(String userLdap);
    
    List<UserEditLog> findByUserLdapOrderByChangedAtDesc(String userLdap);
    
    List<UserEditLog> findByChangedBy(String changedBy);
}
