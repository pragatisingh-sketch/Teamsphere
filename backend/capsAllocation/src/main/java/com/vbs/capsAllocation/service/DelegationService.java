package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.dto.DelegationRequestDTO;
import com.vbs.capsAllocation.model.Delegation;

import java.util.List;

public interface DelegationService {
    String delegateRole(DelegationRequestDTO request);

    String revertDelegation(String delegatorLdap);

    List<Delegation> getDelegationHistory(String ldap);

    void checkScheduledDelegations();

    List<String> getDelegatorsForDelegatee(String delegateeLdap);
}
