package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.dto.TimeSheetDTO;
import com.vbs.capsAllocation.model.TimeSheet;
import com.vbs.capsAllocation.repository.TimeSheetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class TimeSheetService {

    @Autowired
    private TimeSheetRepository timeSheetRepository;

    // Convert Entity to DTO
    private TimeSheetDTO convertToDTO(TimeSheet timeSheet) {
        TimeSheetDTO dto = new TimeSheetDTO();
        dto.setId(timeSheet.getId().toString());
        dto.setLdap(timeSheet.getLdap());
        dto.setMasked_orgid(timeSheet.getMasked_orgid());
        dto.setSubrole(timeSheet.getSubrole());
        dto.setRole(timeSheet.getRole());
        dto.setDate(timeSheet.getDate());
        dto.setProcess(timeSheet.getProcess());
        dto.setBillingCode(timeSheet.getBillingCode());
        dto.setActivity(timeSheet.getActivity());
        dto.setStatus(timeSheet.getStatus());
        dto.setLead_ldap(timeSheet.getLead_ldap());
        dto.setVendor(timeSheet.getVendor());
        dto.setMinutes(timeSheet.getMinutes());
        dto.setProject(timeSheet.getProject());
        dto.setTeam(timeSheet.getTeam());
        dto.setComment(timeSheet.getComment());
        return dto;
    }

    // Convert DTO to Entity
    private TimeSheet convertToEntity(TimeSheetDTO dto) {
        TimeSheet timeSheet = new TimeSheet();
        if (dto.getId() != null && !dto.getId().isEmpty()) {
            timeSheet.setId(Long.parseLong(dto.getId()));
        }
        timeSheet.setLdap(dto.getLdap());
        timeSheet.setMasked_orgid(dto.getMasked_orgid());
        timeSheet.setSubrole(dto.getSubrole());
        timeSheet.setRole(dto.getRole());
        timeSheet.setDate(dto.getDate());
        timeSheet.setProcess(dto.getProcess());
        timeSheet.setBillingCode(dto.getBillingCode());
        timeSheet.setActivity(dto.getActivity());
        timeSheet.setStatus(dto.getStatus());
        timeSheet.setLead_ldap(dto.getLead_ldap());
        timeSheet.setVendor(dto.getVendor());
        timeSheet.setMinutes(dto.getMinutes());
        timeSheet.setProject(dto.getProject());
        timeSheet.setTeam(dto.getTeam());
        timeSheet.setComment(dto.getComment());
        return timeSheet;
    }

    // Get all records
    public List<TimeSheetDTO> getAllRecords() {
        return timeSheetRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Get record by ID
    public TimeSheetDTO getRecordById(Long id) {
        return timeSheetRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    // Create new record
    public TimeSheetDTO createRecord(TimeSheetDTO timeSheetDTO) {
        TimeSheet timeSheet = convertToEntity(timeSheetDTO);
        TimeSheet savedTimeSheet = timeSheetRepository.save(timeSheet);
        return convertToDTO(savedTimeSheet);
    }

    // Update existing record
    public TimeSheetDTO updateRecord(Long id, TimeSheetDTO timeSheetDTO) {
        if (timeSheetRepository.existsById(id)) {
            TimeSheet timeSheet = convertToEntity(timeSheetDTO);
            timeSheet.setId(id);
            TimeSheet updatedTimeSheet = timeSheetRepository.save(timeSheet);
            return convertToDTO(updatedTimeSheet);
        }
        return null;
    }

    // Delete record
    public boolean deleteRecord(Long id) {
        if (timeSheetRepository.existsById(id)) {
            timeSheetRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Delete multiple records
    public boolean deleteMultipleRecords(List<Long> ids) {
        try {
            timeSheetRepository.deleteAllById(ids);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Import from CSV
    public List<TimeSheetDTO> importFromCSV(MultipartFile file) {
        List<TimeSheetDTO> importedRecords = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            // Skip header if exists
            boolean firstLine = true;
            
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue; // Skip header
                }
                
                String[] data = line.split(",");
                if (data.length >= 15) { // Ensure we have all required fields
                    TimeSheetDTO dto = new TimeSheetDTO();
                    // Skip ID as it will be auto-generated
                    dto.setLdap(data[1]);
                    dto.setMasked_orgid(data[2]);
                    dto.setSubrole(data[3]);
                    dto.setRole(data[4]);
                    dto.setDate(data[5]);
                    dto.setProcess(data[6]);
                    dto.setBillingCode(data[7]);
                    dto.setActivity(data[8]);
                    dto.setStatus(data[9]);
                    dto.setLead_ldap(data[10]);
                    dto.setVendor(data[11]);
                    dto.setMinutes(data[12]);
                    dto.setProject(data[13]);
                    dto.setTeam(data[14]);
                    if (data.length > 15) {
                        dto.setComment(data[15]);
                    }
                    
                    TimeSheet savedTimeSheet = timeSheetRepository.save(convertToEntity(dto));
                    importedRecords.add(convertToDTO(savedTimeSheet));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to import CSV: " + e.getMessage());
        }
        
        return importedRecords;
    }

    // Fetch from Bitrix (mock implementation)
    public List<TimeSheetDTO> fetchFromBitrix(String startDate, String endDate) {
        // In a real implementation, this would connect to Bitrix API
        // For now, return mock data
        List<TimeSheetDTO> mockData = new ArrayList<>();
        
        TimeSheetDTO record1 = new TimeSheetDTO();
        record1.setId("B001");
        record1.setLdap("user.bitrix");
        record1.setMasked_orgid("org456");
        record1.setSubrole("Developer");
        record1.setRole("Engineer");
        record1.setDate("2023-06-20");
        record1.setProcess("Development");
        record1.setBillingCode("BIT001");
        record1.setActivity("API Integration");
        record1.setStatus("Completed");
        record1.setLead_ldap("lead.bitrix");
        record1.setVendor("Bitrix24");
        record1.setMinutes("360");
        record1.setProject("Bitrix Integration");
        record1.setTeam("Integration Team");
        record1.setComment("Imported from Bitrix");
        mockData.add(record1);
        
        // Add more mock records as needed
        
        return mockData;
    }
}
