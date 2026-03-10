package com.vbs.capsAllocation.service.impl;

import com.vbs.capsAllocation.dto.CreateHolidayDTO;
import com.vbs.capsAllocation.dto.HolidayDTO;
import com.vbs.capsAllocation.model.Holiday;
import com.vbs.capsAllocation.repository.HolidayRepository;
import com.vbs.capsAllocation.service.HolidayService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class HolidayServiceImpl implements HolidayService {

    @Autowired
    private HolidayRepository holidayRepository;

    @Override
    public HolidayDTO createHoliday(CreateHolidayDTO createHolidayDTO, String createdBy) {
        // Check if holiday already exists for this date
        if (holidayRepository.findByHolidayDateAndIsActive(createHolidayDTO.getHolidayDate(), true).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Holiday already exists for this date");
        }

        Holiday holiday = new Holiday();
        holiday.setHolidayDate(createHolidayDTO.getHolidayDate());
        holiday.setHolidayName(createHolidayDTO.getHolidayName());
        holiday.setDescription(createHolidayDTO.getDescription());
        holiday.setHolidayType(createHolidayDTO.getHolidayType() != null ? createHolidayDTO.getHolidayType() : "GOOGLE");
        holiday.setIsActive(createHolidayDTO.getIsActive() != null ? createHolidayDTO.getIsActive() : true);
        holiday.setUploadedBy(createdBy);

        Holiday savedHoliday = holidayRepository.save(holiday);
        return convertToDTO(savedHoliday);
    }

    @Override
    public List<HolidayDTO> getAllActiveHolidays() {
        return holidayRepository.findByIsActiveOrderByHolidayDateAsc(true).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<HolidayDTO> getHolidaysByYear(int year) {
        return holidayRepository.findHolidaysByYear(year, true).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<HolidayDTO> getHolidaysBetweenDates(LocalDate startDate, LocalDate endDate) {
        return holidayRepository.findHolidaysBetweenDates(startDate, endDate, true).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isHoliday(LocalDate date) {
        return holidayRepository.isHoliday(date);
    }

    @Override
    public HolidayDTO getHolidayByDate(LocalDate date) {
        Holiday holiday = holidayRepository.findByHolidayDateAndIsActive(date, true)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No holiday found for this date"));
        return convertToDTO(holiday);
    }

    @Override
    public HolidayDTO updateHoliday(Long holidayId, CreateHolidayDTO updateHolidayDTO, String updatedBy) {
        Holiday holiday = holidayRepository.findById(holidayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Holiday not found"));

        // Check if date is being changed and if another holiday exists for the new date
        if (!holiday.getHolidayDate().equals(updateHolidayDTO.getHolidayDate())) {
            if (holidayRepository.findByHolidayDateAndIsActive(updateHolidayDTO.getHolidayDate(), true).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Holiday already exists for the new date");
            }
        }

        holiday.setHolidayDate(updateHolidayDTO.getHolidayDate());
        holiday.setHolidayName(updateHolidayDTO.getHolidayName());
        holiday.setDescription(updateHolidayDTO.getDescription());
        holiday.setHolidayType(updateHolidayDTO.getHolidayType() != null ? updateHolidayDTO.getHolidayType() : "GOOGLE");
        holiday.setIsActive(updateHolidayDTO.getIsActive() != null ? updateHolidayDTO.getIsActive() : true);
        holiday.setUploadedBy(updatedBy);

        Holiday savedHoliday = holidayRepository.save(holiday);
        return convertToDTO(savedHoliday);
    }

    @Override
    public void deleteHoliday(Long holidayId) {
        if (!holidayRepository.existsById(holidayId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Holiday not found");
        }
        holidayRepository.deleteById(holidayId);
    }

    @Override
    public List<HolidayDTO> getHolidaysByType(String holidayType) {
        return holidayRepository.findByHolidayTypeAndIsActiveOrderByHolidayDateAsc(holidayType, true).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<HolidayDTO> uploadHolidaysFromCSV(MultipartFile file, String uploadedBy) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file is empty");
        }

        List<Holiday> holidays = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int lineNumber = 0;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // Skip header line
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] columns = line.split(",");
                if (columns.length < 2) {
                    errors.add("Line " + lineNumber + ": Invalid format - expected at least 2 columns (date,holiday)");
                    continue;
                }

                try {
                    String dateStr = columns[0].trim();
                    String name = columns[1].trim();
                    String description = columns.length > 2 ? columns[2].trim() : "";

                    // Handle different date formats including M/d/yyyy
                    if (dateStr.contains("/")) {
                        // Convert M/d/yyyy to MM/dd/yyyy format for parsing
                        String[] dateParts = dateStr.split("/");
                        if (dateParts.length == 3) {
                            String month = dateParts[0].length() == 1 ? "0" + dateParts[0] : dateParts[0];
                            String day = dateParts[1].length() == 1 ? "0" + dateParts[1] : dateParts[1];
                            String year = dateParts[2];
                            dateStr = month + "/" + day + "/" + year;
                        }
                    }

                    // Parse date - support multiple formats
                    LocalDate holidayDate = parseDate(dateStr);
                    
                    if (name.isEmpty()) {
                        errors.add("Line " + lineNumber + ": Holiday name cannot be empty");
                        continue;
                    }

                    Holiday holiday = new Holiday(holidayDate, name, description, uploadedBy);
                    holidays.add(holiday);

                } catch (DateTimeParseException e) {
                    errors.add("Line " + lineNumber + ": Invalid date format - " + columns[0]);
                } catch (Exception e) {
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                }
            }

            if (!errors.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "CSV parsing errors: " + String.join("; ", errors));
            }

            // Delete existing Google holidays before adding new ones (replace functionality)
            holidayRepository.deleteByHolidayType("GOOGLE");

            // Save all holidays
            List<Holiday> savedHolidays = holidayRepository.saveAll(holidays);
            
            return savedHolidays.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to process CSV file: " + e.getMessage());
        }
    }

    private LocalDate parseDate(String dateStr) {
        // Try different date formats
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next format
            }
        }
        
        throw new DateTimeParseException("Unable to parse date: " + dateStr, dateStr, 0);
    }

    private HolidayDTO convertToDTO(Holiday holiday) {
        HolidayDTO dto = new HolidayDTO();
        dto.setId(holiday.getId());
        dto.setHolidayDate(holiday.getHolidayDate());
        dto.setHolidayName(holiday.getHolidayName());
        dto.setDescription(holiday.getDescription());
        dto.setHolidayType(holiday.getHolidayType());
        dto.setIsActive(holiday.getIsActive());
        dto.setCreatedAt(holiday.getCreatedAt());
        dto.setUpdatedAt(holiday.getUpdatedAt());
        dto.setUploadedBy(holiday.getUploadedBy());
        return dto;
    }
}
