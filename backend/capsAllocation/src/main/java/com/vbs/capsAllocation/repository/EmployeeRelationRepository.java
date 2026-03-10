package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.EmployeeRelation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRelationRepository extends JpaRepository<EmployeeRelation, Long> {

    @Modifying
    @Query("DELETE FROM EmployeeRelation er WHERE er.employee.id = :employeeId")
    void deleteByEmployeeId(@Param("employeeId") Long employeeId);

    @Query("SELECT er FROM EmployeeRelation er WHERE er.employee.id IN :ids")
    List<EmployeeRelation> findByEmployeeIdIn(@Param("ids") List<Long> ids);

    @Query("SELECT er FROM EmployeeRelation er WHERE er.employee.id = :id")
    List<EmployeeRelation> findByEmployeeId(@Param("id") Long id);

    @Query("SELECT er FROM EmployeeRelation er JOIN er.relationType rt JOIN er.relatedEmployee re WHERE re.ldap = :leadLdap AND rt.name IN :names AND er.isActive = true")
    List<EmployeeRelation> findSecondaryByLeadLdapAndRelationTypeNames(@Param("leadLdap") String leadLdap, @Param("names") List<String> names);

    // 🔹 Secondary Leads
    @Query("SELECT er.employee.ldap FROM EmployeeRelation er " +
            "JOIN er.relationType rt " +
            "JOIN er.relatedEmployee re " +
            "WHERE re.ldap = :leadLdap AND rt.name = 'LEAD' AND er.isActive = true")
    List<String> findLdapsBySecondaryLead(@Param("leadLdap") String leadLdap);

    // 🔹 Secondary Managers
    @Query("SELECT er.employee.ldap FROM EmployeeRelation er " +
            "JOIN er.relationType rt " +
            "JOIN er.relatedEmployee re " +
            "WHERE re.ldap = :managerLdap AND rt.name = 'MANAGER' AND er.isActive = true")
    List<String> findLdapsBySecondaryManager(@Param("managerLdap") String managerLdap);
}