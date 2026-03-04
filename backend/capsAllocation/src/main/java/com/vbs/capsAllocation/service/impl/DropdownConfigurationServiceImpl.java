package com.vbs.capsAllocation.service.impl;

import com.vbs.capsAllocation.dto.CreateDropdownConfigurationDTO;
import com.vbs.capsAllocation.dto.DropdownConfigurationDTO;
import com.vbs.capsAllocation.dto.UpdateDropdownConfigurationDTO;
import com.vbs.capsAllocation.model.DropdownConfiguration;
import com.vbs.capsAllocation.repository.DropdownConfigurationRepository;
import com.vbs.capsAllocation.service.DropdownConfigurationService;
import com.vbs.capsAllocation.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for managing dropdown configurations
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DropdownConfigurationServiceImpl implements DropdownConfigurationService {

    private final DropdownConfigurationRepository dropdownConfigurationRepository;

    @Override
    public DropdownConfigurationDTO createDropdownOption(CreateDropdownConfigurationDTO createDTO, String createdBy) {
        LoggerUtil.logMethodEntry(DropdownConfigurationServiceImpl.class, "createDropdownOption");

        // Check if option already exists
        if (dropdownConfigurationRepository.existsByDropdownTypeAndOptionValue(
                createDTO.getDropdownType(), createDTO.getOptionValue())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                "Dropdown option already exists for type: " + createDTO.getDropdownType() + 
                " with value: " + createDTO.getOptionValue());
        }

        // If no sort order provided, set it to max + 1
        Integer sortOrder = createDTO.getSortOrder();
        if (sortOrder == null || sortOrder == 0) {
            Integer maxSortOrder = dropdownConfigurationRepository.findMaxSortOrderByDropdownType(createDTO.getDropdownType());
            sortOrder = maxSortOrder + 1;
        }

        DropdownConfiguration entity = new DropdownConfiguration(
            createDTO.getDropdownType(),
            createDTO.getOptionValue(),
            createDTO.getDisplayName(),
            createDTO.getIsActive(),
            sortOrder,
            createdBy
        );

        DropdownConfiguration saved = dropdownConfigurationRepository.save(entity);
        return convertToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DropdownConfigurationDTO> getActiveDropdownOptions(String dropdownType) {
        LoggerUtil.logMethodEntry(DropdownConfigurationServiceImpl.class, "getActiveDropdownOptions");

        List<DropdownConfiguration> configurations = dropdownConfigurationRepository
            .findByDropdownTypeAndIsActiveTrueOrderBySortOrderAscOptionValueAsc(dropdownType);
        
        return configurations.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DropdownConfigurationDTO> getAllDropdownOptions(String dropdownType) {
        LoggerUtil.logMethodEntry(DropdownConfigurationServiceImpl.class, "getAllDropdownOptions");

        List<DropdownConfiguration> configurations = dropdownConfigurationRepository
            .findByDropdownTypeOrderBySortOrderAscOptionValueAsc(dropdownType);
        
        return configurations.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public DropdownConfigurationDTO updateDropdownOption(Long id, UpdateDropdownConfigurationDTO updateDTO) {
        LoggerUtil.logMethodEntry(DropdownConfigurationServiceImpl.class, "updateDropdownOption");

        DropdownConfiguration entity = dropdownConfigurationRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Dropdown configuration not found with id: " + id));

        // Check if the new option value conflicts with existing ones (excluding current entity)
        if (!entity.getOptionValue().equals(updateDTO.getOptionValue()) &&
            dropdownConfigurationRepository.existsByDropdownTypeAndOptionValueAndIdNot(
                entity.getDropdownType(), updateDTO.getOptionValue(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Dropdown option already exists for type: " + entity.getDropdownType() +
                " with value: " + updateDTO.getOptionValue());
        }

        entity.setOptionValue(updateDTO.getOptionValue());
        entity.setDisplayName(updateDTO.getDisplayName());
        entity.setIsActive(updateDTO.getIsActive());
        entity.setSortOrder(updateDTO.getSortOrder());

        DropdownConfiguration updated = dropdownConfigurationRepository.save(entity);
        return convertToDTO(updated);
    }

    @Override
    public void deleteDropdownOption(Long id) {
        LoggerUtil.logMethodEntry(DropdownConfigurationServiceImpl.class, "deleteDropdownOption");

        if (!dropdownConfigurationRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Dropdown configuration not found with id: " + id);
        }

        dropdownConfigurationRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public DropdownConfigurationDTO getDropdownOptionById(Long id) {
        LoggerUtil.logMethodEntry(DropdownConfigurationServiceImpl.class, "getDropdownOptionById");

        DropdownConfiguration entity = dropdownConfigurationRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Dropdown configuration not found with id: " + id));

        return convertToDTO(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllDropdownTypes() {
        LoggerUtil.logMethodEntry(DropdownConfigurationServiceImpl.class, "getAllDropdownTypes");
        return dropdownConfigurationRepository.findDistinctDropdownTypes();
    }

    @Override
    public List<DropdownConfigurationDTO> reorderDropdownOptions(String dropdownType, List<Long> orderedIds) {
        LoggerUtil.logMethodEntry(DropdownConfigurationServiceImpl.class, "reorderDropdownOptions");

        List<DropdownConfiguration> configurations = dropdownConfigurationRepository.findByIdIn(orderedIds);
        
        // Validate that all IDs belong to the same dropdown type
        boolean allSameType = configurations.stream()
            .allMatch(config -> config.getDropdownType().equals(dropdownType));
        
        if (!allSameType) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "All dropdown options must belong to the same type: " + dropdownType);
        }

        // Update sort orders based on the provided order
        for (int i = 0; i < orderedIds.size(); i++) {
            Long id = orderedIds.get(i);
            DropdownConfiguration config = configurations.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "Dropdown configuration not found with id: " + id));
            
            config.setSortOrder(i + 1);
        }

        List<DropdownConfiguration> updated = dropdownConfigurationRepository.saveAll(configurations);
        return updated.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public boolean canManageDropdowns(String userRole) {
        return "ADMIN_OPS_MANAGER".equals(userRole);
    }

    /**
     * Convert DropdownConfiguration entity to DTO
     */
    private DropdownConfigurationDTO convertToDTO(DropdownConfiguration entity) {
        return new DropdownConfigurationDTO(
            entity.getId(),
            entity.getDropdownType(),
            entity.getOptionValue(),
            entity.getDisplayName(),
            entity.getIsActive(),
            entity.getSortOrder(),
            entity.getCreatedBy(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
