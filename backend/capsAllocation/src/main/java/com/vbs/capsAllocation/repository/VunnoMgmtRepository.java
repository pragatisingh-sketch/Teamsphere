package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.Vunno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VunnoMgmtRepository extends JpaRepository<Vunno, String> {

    @Query("SELECT v FROM Vunno v WHERE v.ldap = :ldap")
    List<Vunno> detailsByLdap(@Param("ldap") String ldap);

}
