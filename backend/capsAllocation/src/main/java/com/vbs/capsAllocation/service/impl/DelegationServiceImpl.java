package com.vbs.capsAllocation.service.impl;

import com.vbs.capsAllocation.dto.DelegationRequestDTO;
import com.vbs.capsAllocation.model.Delegation;
import com.vbs.capsAllocation.model.Role;
import com.vbs.capsAllocation.model.User;
import com.vbs.capsAllocation.repository.DelegationRepository;
import com.vbs.capsAllocation.repository.UserRepository;
import com.vbs.capsAllocation.service.DelegationService;
import com.vbs.capsAllocation.util.LoggerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DelegationServiceImpl implements DelegationService {

    @Autowired
    private DelegationRepository delegationRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public String delegateRole(DelegationRequestDTO request) {
        LoggerUtil.logMethodEntry(DelegationServiceImpl.class, "delegateRole", request);

        try {
            User delegator = userRepository.findByUsername(request.getDelegatorLdap())
                    .orElseThrow(() -> new RuntimeException("Delegator not found"));
            User delegatee = userRepository.findByUsername(request.getDelegateeLdap())
                    .orElseThrow(() -> new RuntimeException("Delegatee not found"));

            // Check if delegator has a role that can be delegated
            if (delegator.getRole() == Role.USER) {
                throw new RuntimeException("User role cannot be delegated. Only Lead, Manager, or Admin can delegate.");
            }

            // Check if there is already an active delegation for this delegator
            Optional<Delegation> existingDelegation = delegationRepository.findByDelegatorLdapAndStatus(
                    delegator.getUsername(), Delegation.DelegationStatus.ACTIVE);
            if (existingDelegation.isPresent()) {
                throw new RuntimeException("You already have an active delegation.");
            }

            Delegation delegation = new Delegation();
            delegation.setDelegatorLdap(delegator.getUsername());
            delegation.setDelegateeLdap(delegatee.getUsername());
            delegation.setOriginalRole(delegator.getRole());
            delegation.setDelegateeOriginalRole(delegatee.getRole());
            delegation.setDelegatedRole(delegator.getRole());
            delegation.setStartDate(request.getStartDate());
            delegation.setEndDate(request.getEndDate());

            // Determine status based on start date
            LocalDateTime now = LocalDateTime.now();
            if (request.getStartDate().isAfter(now)) {
                delegation.setStatus(Delegation.DelegationStatus.SCHEDULED);
            } else {
                delegation.setStatus(Delegation.DelegationStatus.ACTIVE);
                // Apply role changes immediately
                applyDelegation(delegator, delegatee, delegator.getRole());
            }

            delegationRepository.save(delegation);
            LoggerUtil.logMethodExit(DelegationServiceImpl.class, "delegateRole", "Success");
            return "Delegation "
                    + (delegation.getStatus() == Delegation.DelegationStatus.ACTIVE ? "activated" : "scheduled")
                    + " successfully.";

        } catch (Exception e) {
            LoggerUtil.logError("Error in delegateRole: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private void applyDelegation(User delegator, User delegatee, Role roleToDelegate) {
        // Delegator becomes USER
        delegator.setRole(Role.USER);
        userRepository.save(delegator);

        // Delegatee gets the role
        delegatee.setRole(roleToDelegate);
        userRepository.save(delegatee);
    }

    @Override
    @Transactional
    public String revertDelegation(String userLdap) {
        LoggerUtil.logMethodEntry(DelegationServiceImpl.class, "revertDelegation", userLdap);

        try {
            // Try to find delegation where the user is the delegator
            Optional<Delegation> delegationOpt = delegationRepository.findByDelegatorLdapAndStatus(
                    userLdap, Delegation.DelegationStatus.ACTIVE);

            // If not found, try to find delegation where the user is the delegatee
            if (delegationOpt.isEmpty()) {
                delegationOpt = delegationRepository.findByDelegateeLdapAndStatus(
                        userLdap, Delegation.DelegationStatus.ACTIVE);
            }

            // If still not found, throw exception
            Delegation delegation = delegationOpt
                    .orElseThrow(() -> new RuntimeException("No active delegation found to revert."));

            User delegator = userRepository.findByUsername(delegation.getDelegatorLdap())
                    .orElseThrow(() -> new RuntimeException("Delegator not found"));
            User delegatee = userRepository.findByUsername(delegation.getDelegateeLdap())
                    .orElseThrow(() -> new RuntimeException("Delegatee not found"));

            // Restore roles
            delegator.setRole(delegation.getOriginalRole());
            userRepository.save(delegator);

            delegatee.setRole(delegation.getDelegateeOriginalRole());
            userRepository.save(delegatee);

            delegation.setStatus(Delegation.DelegationStatus.CANCELLED);
            delegation.setEndDate(LocalDateTime.now()); // Update end date to now
            delegationRepository.save(delegation);

            LoggerUtil.logMethodExit(DelegationServiceImpl.class, "revertDelegation", "Success");
            return "Delegation reverted successfully.";

        } catch (Exception e) {
            LoggerUtil.logError("Error in revertDelegation: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public List<Delegation> getDelegationHistory(String ldap) {
        // Get all delegations where the user is either the delegator OR the delegatee
        List<Delegation> delegatorHistory = delegationRepository.findByDelegatorLdap(ldap);
        List<Delegation> delegateeHistory = delegationRepository.findByDelegateeLdap(ldap);

        // Combine both lists
        Set<Delegation> allDelegations = new HashSet<>();
        allDelegations.addAll(delegatorHistory);
        allDelegations.addAll(delegateeHistory);

        // Convert to list and sort by start date (most recent first)
        return allDelegations.stream()
                .sorted((d1, d2) -> d2.getStartDate().compareTo(d1.getStartDate()))
                .collect(Collectors.toList());
    }

    @Override
    @Scheduled(fixedRate = 60000) // Check every minute
    @Transactional
    public void checkScheduledDelegations() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Activate scheduled delegations
        List<Delegation> scheduled = delegationRepository.findByStatus(Delegation.DelegationStatus.SCHEDULED);
        for (Delegation d : scheduled) {
            if (d.getStartDate().isBefore(now) || d.getStartDate().isEqual(now)) {
                try {
                    User delegator = userRepository.findByUsername(d.getDelegatorLdap()).orElse(null);
                    User delegatee = userRepository.findByUsername(d.getDelegateeLdap()).orElse(null);

                    if (delegator != null && delegatee != null) {
                        applyDelegation(delegator, delegatee, d.getDelegatedRole());
                        d.setStatus(Delegation.DelegationStatus.ACTIVE);
                        delegationRepository.save(d);
                        LoggerUtil.logInfo(DelegationServiceImpl.class, "Activated scheduled delegation: {} -> {}",
                                d.getDelegatorLdap(), d.getDelegateeLdap());
                    }
                } catch (Exception e) {
                    LoggerUtil.logError("Error activating scheduled delegation {}: {}", d.getId(), e.getMessage());
                }
            }
        }

        // 2. Complete expired delegations
        List<Delegation> active = delegationRepository.findByStatus(Delegation.DelegationStatus.ACTIVE);
        for (Delegation d : active) {
            if (d.getEndDate() != null && (d.getEndDate().isBefore(now) || d.getEndDate().isEqual(now))) {
                try {
                    User delegator = userRepository.findByUsername(d.getDelegatorLdap()).orElse(null);
                    User delegatee = userRepository.findByUsername(d.getDelegateeLdap()).orElse(null);

                    if (delegator != null && delegatee != null) {
                        // Restore roles
                        delegator.setRole(d.getOriginalRole());
                        userRepository.save(delegator);

                        delegatee.setRole(d.getDelegateeOriginalRole());
                        userRepository.save(delegatee);

                        d.setStatus(Delegation.DelegationStatus.COMPLETED);
                        delegationRepository.save(d);
                        LoggerUtil.logInfo(DelegationServiceImpl.class, "Completed expired delegation: {} -> {}",
                                d.getDelegatorLdap(), d.getDelegateeLdap());
                    }
                } catch (Exception e) {
                    LoggerUtil.logError("Error completing expired delegation {}: {}", d.getId(), e.getMessage());
                }
            }
        }
    }

    @Override
    public List<String> getDelegatorsForDelegatee(String delegateeLdap) {
        return delegationRepository.findByDelegateeLdap(delegateeLdap).stream()
                .filter(d -> d.getStatus() == Delegation.DelegationStatus.ACTIVE)
                .map(Delegation::getDelegatorLdap)
                .toList();
    }
}
