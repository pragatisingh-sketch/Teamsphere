package com.vbs.capsAllocation.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vbs.capsAllocation.service.EmployeeDataFileService;
import com.vbs.capsAllocation.util.LoggerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class EmployeeDataFileServiceImpl implements EmployeeDataFileService {

    private static final String EMPLOYEE_DATA_FILE = "employee-requests.json";

    private final String dataDirectory;
    private Path dataFilePath;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Autowired
    private ObjectMapper objectMapper;

    public EmployeeDataFileServiceImpl(@Value("${employee.data.directory:./data}") String dataDirectory) {
        this.dataDirectory = dataDirectory;
        // Log immediately to help with debugging deployment issues
        System.out.println("EmployeeDataFileServiceImpl: dataDirectory set to: " + dataDirectory);
    }

    @PostConstruct
    public void init() {
        try {
            LoggerUtil.logInfo(EmployeeDataFileServiceImpl.class,
                    "Initializing EmployeeDataFileService with directory: {}", dataDirectory);
            this.dataFilePath = Paths.get(dataDirectory, EMPLOYEE_DATA_FILE);
            initializeDataFile();
        } catch (Exception e) {
            LoggerUtil.logError("Fatal error initializing EmployeeDataFileService: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize EmployeeDataFileService with directory: " + dataDirectory,
                    e);
        }
    }

    @Override
    public void initializeDataFile() {
        try {
            // Create data directory if it doesn't exist
            Path dataDir = Paths.get(dataDirectory);
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
                LoggerUtil.logInfo(EmployeeDataFileServiceImpl.class, "Created data directory: {}",
                        dataDir.toAbsolutePath());
            }

            // Create empty JSON file if it doesn't exist
            if (!Files.exists(dataFilePath)) {
                Map<String, Object> emptyData = new HashMap<>();
                objectMapper.writeValue(dataFilePath.toFile(), emptyData);
                LoggerUtil.logInfo(EmployeeDataFileServiceImpl.class, "Created employee data file: {}",
                        dataFilePath.toAbsolutePath());
            }
        } catch (IOException e) {
            LoggerUtil.logError("Error initializing employee data file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize employee data file", e);
        }
    }

    @Override
    public String storeEmployeeData(String employeeData) {
        lock.writeLock().lock();
        try {
            // Generate unique key
            String key = generateUniqueKey();

            // Read existing data
            Map<String, Object> allData = readDataFile();

            // Create entry with metadata
            Map<String, Object> entry = new HashMap<>();
            entry.put("employeeData", employeeData);
            entry.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            entry.put("created", System.currentTimeMillis());

            // Store the entry
            allData.put(key, entry);

            // Write back to file
            writeDataFile(allData);

            LoggerUtil.logInfo(EmployeeDataFileServiceImpl.class, "Stored employee data with key: {}", key);
            return key;

        } catch (Exception e) {
            LoggerUtil.logError("Error storing employee data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store employee data", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String storeEmployeeDataWithOriginal(String employeeData, String originalData) {
        lock.writeLock().lock();
        try {
            // Generate unique key
            String key = generateUniqueKey();

            // Read existing data
            Map<String, Object> allData = readDataFile();

            // Create entry with metadata
            Map<String, Object> entry = new HashMap<>();
            entry.put("employeeData", employeeData);
            entry.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            entry.put("created", System.currentTimeMillis());

            // Store original data if provided
            if (originalData != null) {
                entry.put("originalData", originalData);
            }

            // Store the entry
            allData.put(key, entry);

            // Write back to file
            writeDataFile(allData);

            LoggerUtil.logInfo(EmployeeDataFileServiceImpl.class, "Stored employee data with original data, key: {}",
                    key);
            return key;

        } catch (Exception e) {
            LoggerUtil.logError("Error storing employee data with original: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store employee data with original", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<String> getEmployeeData(String key) {
        lock.readLock().lock();
        try {
            Map<String, Object> allData = readDataFile();

            if (allData.containsKey(key)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> entry = (Map<String, Object>) allData.get(key);
                String employeeData = (String) entry.get("employeeData");
                return Optional.ofNullable(employeeData);
            }

            return Optional.empty();

        } catch (Exception e) {
            LoggerUtil.logError("Error retrieving employee data for key {}: {}", key, e.getMessage(), e);
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<String> getOriginalEmployeeData(String key) {
        lock.readLock().lock();
        try {
            Map<String, Object> allData = readDataFile();

            if (allData.containsKey(key)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> entry = (Map<String, Object>) allData.get(key);
                String originalData = (String) entry.get("originalData");
                return Optional.ofNullable(originalData);
            }

            return Optional.empty();

        } catch (Exception e) {
            LoggerUtil.logError("Error retrieving original employee data for key {}: {}", key, e.getMessage(), e);
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean removeEmployeeData(String key) {
        lock.writeLock().lock();
        try {
            Map<String, Object> allData = readDataFile();

            if (allData.containsKey(key)) {
                allData.remove(key);
                writeDataFile(allData);
                LoggerUtil.logInfo(EmployeeDataFileServiceImpl.class, "Removed employee data with key: {}", key);
                return true;
            }

            return false;

        } catch (Exception e) {
            LoggerUtil.logError("Error removing employee data for key {}: {}", key, e.getMessage(), e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean existsEmployeeData(String key) {
        lock.readLock().lock();
        try {
            Map<String, Object> allData = readDataFile();
            return allData.containsKey(key);
        } catch (Exception e) {
            LoggerUtil.logError("Error checking existence of employee data for key {}: {}", key, e.getMessage(), e);
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    private String generateUniqueKey() {
        return "emp_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private Map<String, Object> readDataFile() throws IOException {
        if (!Files.exists(dataFilePath)) {
            return new HashMap<>();
        }

        return objectMapper.readValue(dataFilePath.toFile(), new TypeReference<Map<String, Object>>() {
        });
    }

    private void writeDataFile(Map<String, Object> data) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(dataFilePath.toFile(), data);
    }

    @Override
    public java.util.Set<String> getAllKeys() {
        lock.readLock().lock();
        try {
            Map<String, Object> allData = readDataFile();
            return allData.keySet();
        } catch (Exception e) {
            LoggerUtil.logError("Error getting all keys: {}", e.getMessage(), e);
            return new java.util.HashSet<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int cleanupOldData(int daysOld) {
        lock.writeLock().lock();
        try {
            Map<String, Object> allData = readDataFile();
            long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);

            java.util.List<String> keysToRemove = new java.util.ArrayList<>();

            for (Map.Entry<String, Object> entry : allData.entrySet()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> entryData = (Map<String, Object>) entry.getValue();
                Object createdObj = entryData.get("created");

                if (createdObj instanceof Number) {
                    long created = ((Number) createdObj).longValue();
                    if (created < cutoffTime) {
                        keysToRemove.add(entry.getKey());
                    }
                }
            }

            // Remove old entries
            for (String key : keysToRemove) {
                allData.remove(key);
            }

            if (!keysToRemove.isEmpty()) {
                writeDataFile(allData);
                LoggerUtil.logInfo(EmployeeDataFileServiceImpl.class, "Cleaned up {} old employee data entries",
                        keysToRemove.size());
            }

            return keysToRemove.size();

        } catch (Exception e) {
            LoggerUtil.logError("Error during cleanup: {}", e.getMessage(), e);
            return 0;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
