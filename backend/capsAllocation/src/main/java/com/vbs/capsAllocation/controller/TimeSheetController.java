package com.vbs.capsAllocation.controller;

import com.vbs.capsAllocation.dto.TimeSheetDTO;
import com.vbs.capsAllocation.service.TimeSheetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for timesheet operations in the system
 * 
 * @author Piyush Mishra
 * @version 1.0
 */
@RestController
@RequestMapping("/api/timesheets")
@CrossOrigin(origins = "*")
public class TimeSheetController {

    @Autowired
    private TimeSheetService timeSheetService;

    // Get all records
    @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @GetMapping("/records")
    public ResponseEntity<List<TimeSheetDTO>> getAllRecords() {
        List<TimeSheetDTO> records = timeSheetService.getAllRecords();
        return ResponseEntity.ok(records);
    }

    // Get record by ID
    @GetMapping("/records/{id}")
    public ResponseEntity<TimeSheetDTO> getRecordById(@PathVariable Long id) {
        TimeSheetDTO record = timeSheetService.getRecordById(id);
        if (record != null) {
            return ResponseEntity.ok(record);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Create new record
    @PostMapping("/records")
    public ResponseEntity<TimeSheetDTO> createRecord(@RequestBody TimeSheetDTO timeSheetDTO) {
        TimeSheetDTO createdRecord = timeSheetService.createRecord(timeSheetDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRecord);
    }

    // Update record
    @PutMapping("/records/{id}")
    public ResponseEntity<TimeSheetDTO> updateRecord(@PathVariable Long id, @RequestBody TimeSheetDTO timeSheetDTO) {
        TimeSheetDTO updatedRecord = timeSheetService.updateRecord(id, timeSheetDTO);
        if (updatedRecord != null) {
            return ResponseEntity.ok(updatedRecord);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Delete record
    @DeleteMapping("/records/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        boolean deleted = timeSheetService.deleteRecord(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Delete multiple records
    @PostMapping("/records/delete-multiple")
    public ResponseEntity<Void> deleteMultipleRecords(@RequestBody Map<String, List<String>> requestBody) {
        List<Long> ids = requestBody.get("ids").stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());
        
        boolean deleted = timeSheetService.deleteMultipleRecords(ids);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('LEAD') or hasRole('MANAGER') or hasRole('ADMIN_OPS_MANAGER')")
    @PostMapping("/import-csv")
    public ResponseEntity<List<TimeSheetDTO>> importFromCSV(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            List<TimeSheetDTO> importedRecords = timeSheetService.importFromCSV(file);
            return ResponseEntity.ok(importedRecords);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Export to CSV
    @GetMapping("/export-csv")
    public ResponseEntity<Resource> exportToCSV() {
        List<TimeSheetDTO> records = timeSheetService.getAllRecords();
        
        // Create CSV content
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("ID,LDAP,Masked Org ID,Subrole,Role,Date,Process,Billing Code,Activity,Status,Lead LDAP,Vendor,Minutes,Project,Team,Comment\n");
        
        for (TimeSheetDTO record : records) {
            csvContent.append(record.getId()).append(",")
                    .append(record.getLdap()).append(",")
                    .append(record.getMasked_orgid()).append(",")
                    .append(record.getSubrole()).append(",")
                    .append(record.getRole()).append(",")
                    .append(record.getDate()).append(",")
                    .append(record.getProcess()).append(",")
                    .append(record.getBillingCode()).append(",")
                    .append(record.getActivity()).append(",")
                    .append(record.getStatus()).append(",")
                    .append(record.getLead_ldap()).append(",")
                    .append(record.getVendor()).append(",")
                    .append(record.getMinutes()).append(",")
                    .append(record.getProject()).append(",")
                    .append(record.getTeam()).append(",")
                    .append(record.getComment()).append("\n");
        }
        
        ByteArrayResource resource = new ByteArrayResource(csvContent.toString().getBytes());
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=timesheet_records.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    // Fetch from Bitrix
    @GetMapping("/fetch-bitrix")
    public ResponseEntity<List<TimeSheetDTO>> fetchFromBitrix(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        List<TimeSheetDTO> records = timeSheetService.fetchFromBitrix(startDate, endDate);
        return ResponseEntity.ok(records);
    }
}
