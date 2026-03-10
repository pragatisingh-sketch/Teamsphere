package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.Delegation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DelegationRepository extends JpaRepository<Delegation, Long> {
    List<Delegation> findByDelegatorLdap(String delegatorLdap);

    List<Delegation> findByDelegateeLdap(String delegateeLdap);

    Optional<Delegation> findByDelegatorLdapAndStatus(String delegatorLdap, Delegation.DelegationStatus status);

    Optional<Delegation> findByDelegateeLdapAndStatus(String delegateeLdap, Delegation.DelegationStatus status);

    List<Delegation> findByStatus(Delegation.DelegationStatus status);
}
