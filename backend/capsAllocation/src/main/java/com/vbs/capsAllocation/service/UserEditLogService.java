package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.dto.UserEditLogDTO;
import com.vbs.capsAllocation.model.UserEditLog;
import com.vbs.capsAllocation.repository.UserEditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserEditLogService {

    @Autowired
    private UserEditLogRepository userEditLogRepository;

    /**
     * Log a change to a user field
     * 
     * @param userLdap The LDAP of the user being edited
     * @param fieldName The name of the field that was changed
     * @param oldValue The previous value
     * @param newValue The new value
     * @param changedBy The LDAP of the user making the change
     * @return The saved log entry
     */
    public UserEditLog logChange(String userLdap, String fieldName, String oldValue, String newValue, String changedBy) {
        // Don't log if values are the same
        if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
            return null;
        }
        
        UserEditLog log = new UserEditLog();
        log.setUserLdap(userLdap);
        log.setFieldName(fieldName);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setChangedBy(changedBy);
        
        return userEditLogRepository.save(log);
    }
    
    /**
     * Get all edit logs for a specific user
     * 
     * @param userLdap The LDAP of the user
     * @return List of edit logs
     */
    public List<UserEditLogDTO> getLogsByUserLdap(String userLdap) {
        List<UserEditLog> logs = userEditLogRepository.findByUserLdapOrderByChangedAtDesc(userLdap);
        return logs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Convert entity to DTO
     */
    private UserEditLogDTO convertToDTO(UserEditLog log) {
        UserEditLogDTO dto = new UserEditLogDTO();
        dto.setId(log.getId());
        dto.setUserLdap(log.getUserLdap());
        dto.setFieldName(log.getFieldName());
        dto.setOldValue(log.getOldValue());
        dto.setNewValue(log.getNewValue());
        dto.setChangedBy(log.getChangedBy());
        dto.setChangedAt(log.getChangedAt());
        return dto;
    }
}
