package com.vbs.capsAllocation.config;

import com.vbs.capsAllocation.model.DropdownConfiguration;
import com.vbs.capsAllocation.repository.DropdownConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration class to migrate existing hardcoded dropdown values to database
 * This will run on application startup and populate the dropdown_configurations table
 * with the existing hardcoded values from the frontend
 */
@Configuration
@RequiredArgsConstructor
public class DropdownDataMigrationConfig {

    private final DropdownConfigurationRepository dropdownConfigurationRepository;

    @Bean
    @Transactional
    public CommandLineRunner migrateDropdownData() {
        return args -> {
            // Only run migration if table is empty
            if (dropdownConfigurationRepository.count() == 0) {
                
                migrateProjects();
                migratePsePrograms();
                migrateProcesses();
                migrateLevels();
                migrateLanguages();
                migrateLocations();
                migrateVendors();
                
            } else {
                System.out.println("Dropdown configurations already exist, skipping migration.");
            }
        };
    }

    private void migrateProjects() {
        List<String> projects = Arrays.asList(
                "AIO UX Writers",
                "CG Data",
                "CG Ratings",
                "CQ&P Hume",
                "CQ&P Hume VOVO",
                "CQMT",
                "E&V",
                "GenAi GOVO",
                "GenAi VOVO",
                "UFR Hume",
                "UFR VF",
                "VDM",
                "VO&E"
        );


        for (int i = 0; i < projects.size(); i++) {
            String project = projects.get(i);
            DropdownConfiguration config = new DropdownConfiguration(
                "PROJECT", project, project, true, i + 1, "system"
            );
            dropdownConfigurationRepository.save(config);
        }
    }

    private void migratePsePrograms() {
        List<String> programs = Arrays.asList(
                "Content Growth",
                "Content Quality & Protections",
                "Experimentation & Velocity",
                "GenAi Feedback and Factuality",
                "User Feedback & Reporting",
                "Vendor Operations & Excellance",
                "Vertical Development & Maintenance"
        );


        for (int i = 0; i < programs.size(); i++) {
            String program = programs.get(i);
            DropdownConfiguration config = new DropdownConfiguration(
                "PSE_PROGRAM", program, program, true, i + 1, "system"
            );
            dropdownConfigurationRepository.save(config);
        }
    }

    private void migrateProcesses() {
        List<String> processes = Arrays.asList(
                "HUME",
                "KG_VF",
                "MAGI Feedback",
                "SEI"
        );


        for (int i = 0; i < processes.size(); i++) {
            String process = processes.get(i);
            DropdownConfiguration config = new DropdownConfiguration(
                "PROCESS", process, process, true, i + 1, "system"
            );
            dropdownConfigurationRepository.save(config);
        }
    }

    private void migrateLevels() {
        List<String> levels = Arrays.asList(
                "Analyst EN I",
                "Analyst EN II",
                "Analyst EN III",
                "Analyst i18N I",
                "Analyst i18n I",
                "Analyst i18n II",
                "Analyst i18n III",
                "Lead I",
                "Lead II",
                "Lead III",
                "Program Manager I",
                "Program Manager II",
                "Tech I",
                "Tech II",
                "Tech III"
        );



        for (int i = 0; i < levels.size(); i++) {
            String level = levels.get(i);
            DropdownConfiguration config = new DropdownConfiguration(
                "LEVEL", level, level, true, i + 1, "system"
            );
            dropdownConfigurationRepository.save(config);
        }
    }

    private void migrateLanguages() {
        List<String> languages = Arrays.asList(
                "Bengali",
                "English",
                "Filipino",
                "Gujarati",
                "Hindi",
                "Indonesian",
                "Malayalam",
                "Marathi",
                "Punjabi",
                "Tamil",
                "Telugu",
                "Urdu"
        );



        for (int i = 0; i < languages.size(); i++) {
            String language = languages.get(i);
            DropdownConfiguration config = new DropdownConfiguration(
                "LANGUAGE", language, language, true, i + 1, "system"
            );
            dropdownConfigurationRepository.save(config);
        }
    }

    private void migrateLocations() {
        List<String> locations = Arrays.asList(
            "GOVO, Gurugram, India",
            "VOVO, Gurugram, India",
            "VOVO Haldwani, India"
        );

        for (int i = 0; i < locations.size(); i++) {
            String location = locations.get(i);
            DropdownConfiguration config = new DropdownConfiguration(
                "LOCATION", location, location, true, i + 1, "system"
            );
            dropdownConfigurationRepository.save(config);
        }
    }

    private void migrateVendors() {
        List<String> vendors = Arrays.asList("VBS");

        for (int i = 0; i < vendors.size(); i++) {
            String vendor = vendors.get(i);
            DropdownConfiguration config = new DropdownConfiguration(
                "VENDOR", vendor, vendor, true, i + 1, "system"
            );
            dropdownConfigurationRepository.save(config);
        }
    }
}
