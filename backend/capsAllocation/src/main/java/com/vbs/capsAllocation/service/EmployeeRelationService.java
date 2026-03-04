package com.vbs.capsAllocation.service;

import com.vbs.capsAllocation.model.EmployeeRelation;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;

public interface EmployeeRelationService {
    List<EmployeeRelation> getAllEmployeeRelations();
    Optional<EmployeeRelation> getEmployeeRelationById(Long id);
    EmployeeRelation createEmployeeRelation(EmployeeRelation employeeRelation);
    EmployeeRelation updateEmployeeRelation(Long id, EmployeeRelation employeeRelationDetails);
    void deleteEmployeeRelation(Long id);
    String uploadEmployeeRelationCsv(MultipartFile file);
}