package com.vbs.capsAllocation.service.impl;

import com.vbs.capsAllocation.model.Employee;
import com.vbs.capsAllocation.model.EmployeeRelation;
import com.vbs.capsAllocation.model.RelationType;
import com.vbs.capsAllocation.repository.EmployeeRelationRepository;
import com.vbs.capsAllocation.repository.EmployeeRepository;
import com.vbs.capsAllocation.repository.RelationTypeRepository;
import com.vbs.capsAllocation.service.EmployeeRelationService;
import com.vbs.capsAllocation.service.EmployeeService;
import com.vbs.capsAllocation.util.LoggerUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmployeeRelationServiceImpl implements EmployeeRelationService {

    @Autowired
    private EmployeeRelationRepository employeeRelationRepository;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private RelationTypeRepository relationTypeRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public List<EmployeeRelation> getAllEmployeeRelations() {
        return employeeRelationRepository.findAll();
    }

    @Override
    public Optional<EmployeeRelation> getEmployeeRelationById(Long id) {
        return employeeRelationRepository.findById(id);
    }

    @Override
    public EmployeeRelation createEmployeeRelation(EmployeeRelation employeeRelation) {
        return employeeRelationRepository.save(employeeRelation);
    }

    @Override
    public EmployeeRelation updateEmployeeRelation(Long id, EmployeeRelation employeeRelationDetails) {
        Optional<EmployeeRelation> optionalEmployeeRelation = employeeRelationRepository.findById(id);
        if (optionalEmployeeRelation.isPresent()) {
            EmployeeRelation employeeRelation = optionalEmployeeRelation.get();
            employeeRelation.setEmployee(employeeRelationDetails.getEmployee());
            employeeRelation.setRelationType(employeeRelationDetails.getRelationType());
            employeeRelation.setRelationValue(employeeRelationDetails.getRelationValue());
            employeeRelation.setEffectiveDate(employeeRelationDetails.getEffectiveDate());
            employeeRelation.setEndDate(employeeRelationDetails.getEndDate());
            employeeRelation.setIsActive(employeeRelationDetails.getIsActive());
            return employeeRelationRepository.save(employeeRelation);
        }
        throw new RuntimeException("EmployeeRelation not found with id " + id);
    }

    @Override
    public void deleteEmployeeRelation(Long id) {
        if (employeeRelationRepository.existsById(id)) {
            employeeRelationRepository.deleteById(id);
        } else {
            throw new RuntimeException("EmployeeRelation not found with id " + id);
        }
    }

    @Override
    @Transactional
    public String uploadEmployeeRelationCsv(MultipartFile file) {
        Map<String, List<Map<String, Object>>> employeeRelations = new HashMap<>();
        int processedCount = 0;
        int updatedCount = 0;
        int secondaryRelationsCount = 0;
        int skippedEmployees = 0;
        List<String> skippedEmployeeList = new ArrayList<>();

        // Cache RelationTypes
        Map<String, RelationType> relationTypeMap = relationTypeRepository.findAll()
                .stream()
                .collect(Collectors.toMap(rt -> rt.getName().toUpperCase(), rt -> rt));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            LoggerUtil.logDebug("Starting CSV upload for employee relations");

            String line;
            int lineNumber = 0;
            String[] headers = null;

            // First pass: parse CSV
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;

                try {
                    String[] values = line.split(",", -1); // preserve empty
                    if (lineNumber == 1) {
                        headers = values;
                        continue;
                    }
                    if (headers == null) continue;

                    String ldap = "";
                    String primaryLead = "";
                    String primaryManager = "";
                    List<String> secondaryLeads = new ArrayList<>();
                    List<String> secondaryManagers = new ArrayList<>();
                    String project = "";
                    String psAndEProgram = "";
                    String level = "";

                    for (int i = 0; i < headers.length; i++) {
                        String header = headers[i].trim().toLowerCase();
                        String value = i < values.length ? cleanValue(values[i]) : "";

                        switch (header) {
                            case "ldap":
                                ldap = value;
                                break;
                            case "primary lead":
                                primaryLead = value;
                                break;
                            case "primary manager":
                                primaryManager = value;
                                break;
                            case "project":
                                project = value;
                                break;
                            case "ps&e program":
                            case "ps&e program ":
                                psAndEProgram = value;
                                break;
                            case "level":
                                level = value;
                                break;
                            default:
                                if (header.contains("secondary lead")) secondaryLeads.add(value);
                                else if (header.contains("secondary manager")) secondaryManagers.add(value);
                                break;
                        }
                    }

                    // Always record even empty values
                    List<Map<String, Object>> relationData = new ArrayList<>();
                    relationData.add(Map.of("primaryLead", primaryLead));
                    relationData.add(Map.of("primaryManager", primaryManager));
                    relationData.add(Map.of("secondaryLeads", secondaryLeads));
                    relationData.add(Map.of("secondaryManagers", secondaryManagers));
                    relationData.add(Map.of("project", project));
                    relationData.add(Map.of("psAndEProgram", psAndEProgram));
                    relationData.add(Map.of("level", level));

                    employeeRelations.put(ldap, relationData);
                    processedCount++;
                } catch (Exception e) {
                    LoggerUtil.logError("Error parsing CSV line " + lineNumber + ": " + e.getMessage(), e);
                    // Continue processing other lines - don't mark transaction for rollback
                    // Just skip this line and continue with the next one
                    continue;
                }
            }

            LoggerUtil.logDebug("CSV parsing completed. Total employees processed: " + processedCount);

            // Second pass: DB updates
            for (Map.Entry<String, List<Map<String, Object>>> entry : employeeRelations.entrySet()) {
                String ldap = entry.getKey();
                List<Map<String, Object>> relationData = entry.getValue();

                if (ldap.isEmpty()) continue;
                System.out.println("Ldap: "+ldap);
                Employee employee = null;
                try {
                    employee = employeeService.getEmployeeByLdap(ldap);
                } catch (RuntimeException e) {
                    skippedEmployees++;
                    skippedEmployeeList.add(ldap);
                    LoggerUtil.logDebug("Skipping employee lookup error for LDAP " + ldap + ": " + e.getMessage());
                    continue;
                }

                if (employee == null) {
                    skippedEmployees++;
                    skippedEmployeeList.add(ldap);
                    LoggerUtil.logDebug("Skipping employee, not found: " + ldap);
                    continue;
                }

                boolean employeeUpdated = false;

                // Primary Lead
                String primaryLead = (String) relationData.get(0).get("primaryLead");
                if (!primaryLead.isEmpty()) {
                    try {
                        Employee leadEmp = employeeService.getEmployeeByLdap(primaryLead);
                        if (leadEmp != null) {
                            employee.setLead(leadEmp.getLdap());
                            employeeUpdated = true;
                        } else {
                            LoggerUtil.logDebug("Primary Lead not found for LDAP " + ldap + " -> " + primaryLead);
                        }
                    } catch (Exception e) {
                        LoggerUtil.logDebug("Error looking up Primary Lead for " + ldap + " -> " + primaryLead + ": " + e.getMessage());
                        // Continue processing other fields - don't mark transaction for rollback
                    }
                }

                // Primary Manager
                String primaryManager = (String) relationData.get(1).get("primaryManager");
                if (!primaryManager.isEmpty()) {
                    try {
                        Employee mgrEmp = employeeService.getEmployeeByLdap(primaryManager);
                        if (mgrEmp != null) {
                            employee.setProgramManager(mgrEmp.getLdap());
                            employeeUpdated = true;
                        } else {
                            LoggerUtil.logDebug("Primary Manager not found for LDAP " + ldap + " -> " + primaryManager);
                        }
                    } catch (Exception e) {
                        LoggerUtil.logDebug("Error looking up Primary Manager for " + ldap + " -> " + primaryManager + ": " + e.getMessage());
                        // Continue processing other fields - don't mark transaction for rollback
                    }
                }

                // Project
                String project = (String) relationData.get(4).get("project");
                if (!project.isEmpty()) {
                    employee.setTeam(project);
                    employeeUpdated = true;
                }

                // PS&E Program
                String psAndEProgram = (String) relationData.get(5).get("psAndEProgram");
                if (!psAndEProgram.isEmpty()) {
                    employee.setPnseProgram(psAndEProgram);
                    employeeUpdated = true;
                }

                // Level
                String level = (String) relationData.get(6).get("level");
                if (!level.isEmpty()) {
                    employee.setLevel(level);
                    employeeUpdated = true;
                }

                if (employeeUpdated) {
                    try {
                        employeeRepository.save(employee);
                        updatedCount++;
                    } catch (Exception e) {
                        LoggerUtil.logError("Error updating employee " + ldap + ": " + e.getMessage(), e);
                        // Continue processing other employees - don't mark transaction for rollback
                    }
                }

                // Handle secondary relations
                List<String> secondaryLeads = (List<String>) relationData.get(2).get("secondaryLeads");
                List<String> secondaryManagers = (List<String>) relationData.get(3).get("secondaryManagers");

                try {
                    employeeRelationRepository.deleteByEmployeeId(employee.getId());
                } catch (Exception e) {
                    LoggerUtil.logError("Error deleting existing relations for employee " + ldap + ": " + e.getMessage(), e);
                    // Continue processing - don't mark transaction for rollback
                }

                // Secondary Leads
                RelationType leadType = relationTypeMap.get("LEAD");
                if (leadType == null) {
                    LoggerUtil.logDebug("RelationType 'LEAD' not found for employee " + ldap);
                    // Continue processing other relations - don't mark transaction for rollback
                } else {
                    for (String leadLdap : secondaryLeads) {
                        if (!leadLdap.isEmpty()) {
                            try {
                                Employee leadEmp = employeeService.getEmployeeByLdap(leadLdap);
                                if (leadEmp != null) {
                                    saveRelation(employee, leadEmp, leadType, leadLdap);
                                    secondaryRelationsCount++;
                                } else {
                                    LoggerUtil.logDebug("Secondary Lead not found for " + ldap + " -> " + leadLdap);
                                }
                            } catch (Exception e) {
                                LoggerUtil.logDebug("Error looking up secondary Lead for " + ldap + " -> " + leadLdap + ": " + e.getMessage());
                                // Continue processing other secondary leads - don't mark transaction for rollback
                            }
                        }
                    }
                }

                // Secondary Managers
                RelationType managerType = relationTypeMap.get("MANAGER");
                if (managerType == null) {
                    LoggerUtil.logDebug("RelationType 'MANAGER' not found for employee " + ldap);
                    // Continue processing other relations - don't mark transaction for rollback
                } else {
                    for (String mgrLdap : secondaryManagers) {
                        if (!mgrLdap.isEmpty()) {
                            try {
                                Employee mgrEmp = employeeService.getEmployeeByLdap(mgrLdap);
                                if (mgrEmp != null) {
                                    saveRelation(employee, mgrEmp, managerType, mgrLdap);
                                    secondaryRelationsCount++;
                                } else {
                                    LoggerUtil.logDebug("Secondary Manager not found for " + ldap + " -> " + mgrLdap);
                                }
                            } catch (Exception e) {
                                LoggerUtil.logDebug("Error looking up secondary Manager for " + ldap + " -> " + mgrLdap + ": " + e.getMessage());
                                // Continue processing other secondary managers - don't mark transaction for rollback
                            }
                        }
                    }
                }
            }

            LoggerUtil.logDebug("CSV upload completed: processed=" + processedCount +
                    ", updated=" + updatedCount +
                    ", secondaryRelations=" + secondaryRelationsCount +
                    ", skippedEmployees=" + skippedEmployees);

            // Print complete list of skipped employees
            if (!skippedEmployeeList.isEmpty()) {
                LoggerUtil.logDebug("Complete list of skipped employees (not found): " + skippedEmployeeList);
            }

            return "CSV uploaded successfully. " + processedCount + " employees processed, " +
                    updatedCount + " employees updated, " +
                    secondaryRelationsCount + " secondary relations added, " +
                    skippedEmployees + " employees skipped (not found).";

        } catch (IOException e) {
            LoggerUtil.logError("Error reading CSV file: " + e.getMessage(), e);
            throw new RuntimeException("Failed to read CSV file: " + e.getMessage());
        }
    }

    /** Helper to clean CSV values */
    private String cleanValue(String value) {
        if (value == null) return "";
        value = value.trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
            value = value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    /** Helper to save a secondary relation */
    private void saveRelation(Employee employee, Employee related, RelationType type, String relationValue) {
        try {
            EmployeeRelation relation = new EmployeeRelation();
            relation.setEmployee(employee);
            relation.setRelatedEmployee(related);
            relation.setRelationType(type);
            relation.setRelationValue(relationValue);
            relation.setEffectiveDate(String.valueOf(new Date()));
            employeeRelationRepository.save(relation);
        } catch (Exception e) {
            LoggerUtil.logError("Error saving secondary relation for employee " + employee.getLdap() +
                    " with relation " + relationValue + ": " + e.getMessage(), e);
            // Continue processing other relations - don't mark transaction for rollback
        }
    }


}
