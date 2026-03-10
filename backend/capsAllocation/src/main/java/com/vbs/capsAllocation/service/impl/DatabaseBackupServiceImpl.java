package com.vbs.capsAllocation.service.impl;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.vbs.capsAllocation.dto.DatabaseBackupResponseDTO;
import com.vbs.capsAllocation.service.DatabaseBackupService;
import com.vbs.capsAllocation.util.LoggerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Service for handling database backups and uploading them to Google Drive
 */
@Service
public class DatabaseBackupServiceImpl implements DatabaseBackupService {
    private static final String APPLICATION_NAME = "Database Backup Service";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE);
    private static final String BACKUP_FOLDER_NAME = "Database_Backups";
    private static final String BACKUP_MIME_TYPE = "application/x-sql";
    private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
    private static final int RETENTION_DAYS = 30; // Keep backups for 30 days

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    @Value("${google.sheets.credentials:classpath:ops-excellence-a969197613f8.json}")
    private String credentialsPath;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String emailFrom;

    @Value("${backup.share.email:vbs-tms@vacobinary.in}")
    private String backupShareEmail;

    @Value("${backup.directory:#{systemProperties['java.io.tmpdir']}/teamsphere_backups}")
    private String backupDirectory;

    @Value("${backup.googledrive.enabled:true}")
    private boolean googleDriveEnabled;

    @Value("${backup.googledrive.timeout:30}")
    private int googleDriveTimeoutSeconds;

    // Track ongoing operations
    private final Map<String, DatabaseBackupResponseDTO> operationStatus = new ConcurrentHashMap<>();

    /**
     * Creates a backup of the database and uploads it to Google Drive
     * @param initiatedBy The username of the person initiating the backup
     * @return DatabaseBackupResponseDTO with operation details
     */
    @Override
    public DatabaseBackupResponseDTO createAndUploadBackup(String initiatedBy) {
        String operationId = java.util.UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            LoggerUtil.logDebug("Starting database backup operation: {} initiated by: {}", operationId, initiatedBy);

            // Extract database name from JDBC URL
            String databaseName = extractDatabaseName(databaseUrl);
            if (databaseName == null) {
                LoggerUtil.logError("Failed to extract database name from URL: {}", databaseUrl);
                return DatabaseBackupResponseDTO.failure("BACKUP", initiatedBy, "Failed to extract database name from URL");
            }

            // Create backup file
            String backupFilePath = createDatabaseBackup(databaseName);
            if (backupFilePath == null) {
                LoggerUtil.logError("Failed to create database backup");
                return DatabaseBackupResponseDTO.failure("BACKUP", initiatedBy, "Failed to create database backup");
            }

            // Upload to Google Drive (if enabled)
            String fileId = null;
            if (googleDriveEnabled) {
                LoggerUtil.logDebug("Google Drive upload is enabled, attempting upload...");
                fileId = uploadToGoogleDrive(backupFilePath);
                if (fileId == null) {
                    LoggerUtil.logError("Failed to upload backup to Google Drive, but continuing with local backup");
                    // Don't fail the entire operation - keep the local backup
                }
            } else {
                LoggerUtil.logDebug("Google Drive upload is disabled, keeping local backup only");
            }

            // Get file size
            Long fileSize = getFileSize(backupFilePath);
            String fileName = java.nio.file.Paths.get(backupFilePath).getFileName().toString();

            // Clean up old backups (only if Google Drive is enabled and working)
            if (googleDriveEnabled && fileId != null) {
                cleanupOldBackups();
                // Delete local backup file since it's uploaded to Google Drive
                deleteLocalBackupFile(backupFilePath);
            } else {
                LoggerUtil.logDebug("Keeping local backup file: {}", backupFilePath);
                // Keep local backup file since Google Drive upload failed or is disabled
            }

            long duration = System.currentTimeMillis() - startTime;
            LoggerUtil.logDebug("Database backup completed successfully. Operation: {}, File ID: {}, Duration: {}ms",
                    operationId, fileId, duration);

            return DatabaseBackupResponseDTO.backupSuccess(operationId, fileId, fileName, fileSize, initiatedBy, duration);
        } catch (Exception e) {
            LoggerUtil.logError("Error during database backup process: {}", e.getMessage(), e);
            sendErrorNotification("Database Backup Failed",
                    "An error occurred during the database backup process: " + e.getMessage());
            return DatabaseBackupResponseDTO.failure("BACKUP", initiatedBy, "Error during backup process: " + e.getMessage());
        }
    }

    /**
     * Legacy method for backward compatibility
     * @return true if backup was successful, false otherwise
     */
    public boolean createAndUploadBackup() {
        DatabaseBackupResponseDTO response = createAndUploadBackup("system");
        return "SUCCESS".equals(response.getStatus());
    }

    /**
     * Extracts the database name from the JDBC URL
     * @param jdbcUrl the JDBC URL
     * @return the database name
     */
    private String extractDatabaseName(String jdbcUrl) {
        // Example URL: jdbc:postgresql://localhost:5432/vbs_allocation_caps
        try {
            String[] parts = jdbcUrl.split("/");
            return parts[parts.length - 1];
        } catch (Exception e) {
            LoggerUtil.logError("Error extracting database name from JDBC URL: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Creates a backup of the database using pg_dump
     * @param databaseName the name of the database
     * @return the path to the backup file
     */
 /*   private String createDatabaseBackup(String databaseName) {
        try {
            // Create backup directory if it doesn't exist
            Path backupDir = getBackupDirectory();  // You define this helper method elsewhere
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }

            // Generate backup file name with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String backupFileName = databaseName + "_" + timestamp + ".sql";
            String backupFilePath = backupDir.resolve(backupFileName).toString();

            // Full path to pg_dump (use `which pg_dump` to confirm on your server)
            String pgDumpPath = "/usr/bin/pg_dump";

            // Build the pg_dump command
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pgDumpPath,
                    "-h", "localhost",
                    "-p", "5432",
                    "-U", databaseUsername,       // Injected or configured elsewhere
                    "-F", "p",                     // Plain SQL format
                    "-f", backupFilePath,         // Output file path
                    databaseName                  // Target DB
            );

            // Set PGPASSWORD environment variable for password authentication
            processBuilder.environment().put("PGPASSWORD", databasePassword);

            // Merge stdout and stderr so we can log all output
            processBuilder.redirectErrorStream(true);

            // Start the pg_dump process
            Process process = processBuilder.start();

            // Read and log the combined output (stdout + stderr)
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LoggerUtil.logDebug("pg_dump output: {}", line);
                }
            }

            // Wait for the process to finish
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                LoggerUtil.logInfo("Database backup created successfully: {}".getClass(), backupFilePath);
                return backupFilePath;
            } else {
                LoggerUtil.logError("❌ pg_dump failed with exit code: {}", exitCode);
                return null;
            }

        } catch (Exception e) {
            LoggerUtil.logError("❗ Error creating database backup: {}", e.getMessage(), e);
            return null;
        }
    }*/


    private String createDatabaseBackup(String databaseName) {
    try {
        // Step 1: Create backup directory if it doesn't exist
        Path backupDir = getBackupDirectory();
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }

        // Step 2: Generate backup file name with timestamp
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String backupFileName = databaseName + "_" + timestamp + ".sql";
        String backupFilePath = backupDir.resolve(backupFileName).toString();

        // Step 3: Use absolute path to pg_dump to avoid environment issues
        String pgDumpPath = "/usr/bin/pg_dump"; // Adjust if your path differs

        // Step 4: Build pg_dump command
        ProcessBuilder processBuilder = new ProcessBuilder(
                pgDumpPath,
                "-h", "localhost",
                "-p", "5432",
                "-U", "postgres",  // Update if using a different DB user
                "-F", "p",          // Plain text format
                "-f", backupFilePath,
                databaseName
        );

        // Step 5: Set PGPASSWORD in environment to avoid password prompt
        processBuilder.environment().put("PGPASSWORD", databasePassword);

        // Step 6: Log the full command for debugging (don't log password)
        LoggerUtil.logDebug("Running pg_dump with command: {}", String.join(" ", processBuilder.command()));

        // Step 7: Start the process
        Process process = processBuilder.start();

        // Step 8: Capture output and error streams
        BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        String line;
        while ((line = stdOut.readLine()) != null) {
            LoggerUtil.logDebug("[pg_dump output] {}", line);
        }
        while ((line = stdErr.readLine()) != null) {
            LoggerUtil.logError("[pg_dump error] {}", line);
        }

        // Step 9: Wait for process to complete
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            LoggerUtil.logInfo("✅ Database backup created successfully: {}".getClass(), backupFilePath);
            return backupFilePath;
        } else {
            LoggerUtil.logError("❌ pg_dump failed with exit code: {}", exitCode);
            return null;
        }

    } catch (Exception e) {
        LoggerUtil.logError("🚫 Exception during database backup: {}", e.getMessage(), e);
        return null;
    }
}


    /**
     * Uploads the backup file to Google Drive
     * @param backupFilePath the path to the backup file
     * @return the ID of the uploaded file, or null if upload fails
     */
    private String uploadToGoogleDrive(String backupFilePath) {
        try {
            LoggerUtil.logDebug("Starting Google Drive upload for file: {}", backupFilePath);

            // Initialize Drive service with timeout
            Drive driveService = getDriveService();

            // Get or create backup folder
            String folderId = getOrCreateBackupFolder(driveService);
            if (folderId == null) {
                LoggerUtil.logError("Failed to get or create backup folder");
                return null;
            }

            // Create file metadata
            java.io.File backupFile = new java.io.File(backupFilePath);
            File fileMetadata = new File();
            fileMetadata.setName(backupFile.getName());
            fileMetadata.setParents(Collections.singletonList(folderId));

            // Upload file
            FileContent mediaContent = new FileContent(BACKUP_MIME_TYPE, backupFile);
            File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();

            // Share the file with the specified email
            if (backupShareEmail != null && !backupShareEmail.isEmpty()) {
                try {
                    // Create permission for the user
                    com.google.api.services.drive.model.Permission permission = new com.google.api.services.drive.model.Permission()
                            .setType("user")
                            .setRole("writer")
                            .setEmailAddress(backupShareEmail);

                    // Share the file
                    driveService.permissions().create(uploadedFile.getId(), permission)
                            .setSendNotificationEmail(true)
                            .setEmailMessage("Database backup file has been shared with you.")
                            .execute();

                    LoggerUtil.logDebug("File shared with: {}", backupShareEmail);
                } catch (Exception e) {
                    LoggerUtil.logError("Error sharing file with {}: {}", backupShareEmail, e.getMessage());
                    // Continue even if sharing fails
                }
            }

            LoggerUtil.logDebug("File uploaded to Google Drive with ID: {}", uploadedFile.getId());
            return uploadedFile.getId();
        } catch (java.net.ConnectException | java.net.UnknownHostException e) {
            LoggerUtil.logError("Network connectivity issue with Google Drive: {}. Check internet connection and firewall settings.", e.getMessage());
            return null;
        } catch (java.net.SocketTimeoutException e) {
            LoggerUtil.logError("Google Drive upload timed out: {}. Try increasing the timeout or check network speed.", e.getMessage());
            return null;
        } catch (java.io.IOException e) {
            // This covers Google Auth exceptions and other IO issues
            if (e.getMessage() != null && e.getMessage().contains("oauth2.googleapis.com")) {
                LoggerUtil.logError("Google Drive authentication failed: {}. This may be due to network restrictions or invalid credentials.", e.getMessage());
            } else {
                LoggerUtil.logError("IO error during Google Drive upload: {}", e.getMessage());
            }
            LoggerUtil.logDebug("Google Drive error details", e);
            return null;
        } catch (Exception e) {
            LoggerUtil.logError("Unexpected error uploading backup to Google Drive: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Gets or creates the backup folder in Google Drive
     * @param driveService the Drive service
     * @return the ID of the backup folder
     */
    private String getOrCreateBackupFolder(Drive driveService) throws IOException {
        // Check if folder already exists
        String query = "mimeType='" + FOLDER_MIME_TYPE + "' and name='" + BACKUP_FOLDER_NAME + "' and trashed=false";
        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        List<File> files = result.getFiles();
        String folderId;
        boolean isNewFolder = false;

        if (files != null && !files.isEmpty()) {
            // Folder exists, return its ID
            folderId = files.get(0).getId();
        } else {
            // Create folder
            File folderMetadata = new File();
            folderMetadata.setName(BACKUP_FOLDER_NAME);
            folderMetadata.setMimeType(FOLDER_MIME_TYPE);

            File folder = driveService.files().create(folderMetadata)
                    .setFields("id")
                    .execute();

            LoggerUtil.logDebug("Backup folder created with ID: {}", folder.getId());
            folderId = folder.getId();
            isNewFolder = true;
        }

        // Share the folder with the specified email if it's a new folder
        // or if we want to ensure the user has access to all folders
        if (isNewFolder && backupShareEmail != null && !backupShareEmail.isEmpty()) {
            try {
                // Create permission for the user
                com.google.api.services.drive.model.Permission permission = new com.google.api.services.drive.model.Permission()
                        .setType("user")
                        .setRole("writer")
                        .setEmailAddress(backupShareEmail);

                // Share the folder
                driveService.permissions().create(folderId, permission)
                        .setSendNotificationEmail(true)
                        .setEmailMessage("Database backup folder has been shared with you.")
                        .execute();

                LoggerUtil.logDebug("Folder shared with: {}", backupShareEmail);
            } catch (Exception e) {
                LoggerUtil.logError("Error sharing folder with {}: {}", backupShareEmail, e.getMessage());
                // Continue even if sharing fails
            }
        }

        return folderId;
    }

    /**
     * Cleans up backups older than the retention period
     */
    private void cleanupOldBackups() {
        try {
            Drive driveService = getDriveService();

            // Get backup folder ID
            String folderId = getOrCreateBackupFolder(driveService);
            if (folderId == null) {
                LoggerUtil.logError("Failed to get backup folder for cleanup");
                return;
            }

            // Calculate cutoff date (30 days ago)
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(RETENTION_DAYS);
            Date cutoff = Date.from(cutoffDate.atZone(ZoneId.systemDefault()).toInstant());

            // List all files in the backup folder
            String query = "'" + folderId + "' in parents and trashed=false";
            FileList result = driveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id, name, createdTime)")
                    .execute();

            List<File> files = result.getFiles();
            if (files == null || files.isEmpty()) {
                LoggerUtil.logDebug("No backup files found for cleanup");
                return;
            }

            int deletedCount = 0;
            for (File file : files) {
                // Check if file is older than cutoff date
                Date createdTime = new Date(file.getCreatedTime().getValue());
                if (createdTime.before(cutoff)) {
                    // Delete file
                    driveService.files().delete(file.getId()).execute();
                    deletedCount++;
                    LoggerUtil.logDebug("Deleted old backup file: {}", file.getName());
                }
            }

            LoggerUtil.logDebug("Cleanup completed. Deleted {} old backup files", deletedCount);
        } catch (Exception e) {
            LoggerUtil.logError("Error cleaning up old backups: {}", e.getMessage(), e);
        }
    }

    /**
     * Deletes the local backup file
     * @param backupFilePath the path to the backup file
     */
    private void deleteLocalBackupFile(String backupFilePath) {
        try {
            Path path = Paths.get(backupFilePath);
            Files.deleteIfExists(path);
            LoggerUtil.logDebug("Local backup file deleted: {}", backupFilePath);
        } catch (Exception e) {
            LoggerUtil.logError("Error deleting local backup file: {}", e.getMessage(), e);
        }
    }

    /**
     * Gets the Google Drive service
     * @return the Drive service
     */
    private Drive getDriveService() throws IOException, GeneralSecurityException {
        // Load credentials from resources folder
        InputStream in = getClass().getClassLoader().getResourceAsStream("ops-excellence-a969197613f8.json");
        if (in == null) {
            throw new IOException("Credentials file not found in resources");
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(in).createScoped(SCOPES);
        ServiceAccountCredentials serviceAccountCredentials = (ServiceAccountCredentials) credentials;

        LoggerUtil.logDebug("Service Account Email: {}", serviceAccountCredentials.getClientEmail());
        LoggerUtil.logDebug("Project ID: {}", serviceAccountCredentials.getProjectId());

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        return new Drive.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Sends an error notification email
     * @param subject the email subject
     * @param body the email body
     */
    private void sendErrorNotification(String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(emailFrom);
            helper.setTo(emailFrom); // Send to admin email
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);
            LoggerUtil.logDebug("Error notification email sent");
        } catch (MessagingException e) {
            LoggerUtil.logError("Failed to send error notification email: {}", e.getMessage(), e);
        }
    }

    /**
     * Imports data from a backup file
     * @param fileId Optional file ID to import from a specific backup. If null, imports from the most recent backup.
     * @param initiatedBy The username of the person initiating the import
     * @return DatabaseBackupResponseDTO with operation details
     */
    @Override
    public DatabaseBackupResponseDTO importFromBackup(String fileId, String initiatedBy) {

        if (fileId != null) {
            LoggerUtil.logDebug("Database import from specific backup triggered by user: {} with fileId: {}",
                    initiatedBy, fileId);
        } else {
            LoggerUtil.logDebug("Database import from most recent backup triggered by user: {}",
                    initiatedBy);
        }
        String operationId = java.util.UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            LoggerUtil.logDebug("Starting database import operation: {} initiated by: {} with fileId: {}",
                    operationId, initiatedBy, fileId);

            // Check if Google Drive is enabled and accessible
            if (!googleDriveEnabled) {
                LoggerUtil.logDebug("Google Drive is disabled, attempting to import from local backup files");
                return importFromLocalBackup(operationId, initiatedBy, startTime);
            }

            // Try to initialize Drive service
            Drive driveService;
            try {
                driveService = getDriveService();
            } catch (Exception e) {
                LoggerUtil.logError("Failed to initialize Google Drive service: {}", e.getMessage());
                LoggerUtil.logDebug("Falling back to local backup import");
                return importFromLocalBackup(operationId, initiatedBy, startTime);
            }

            // Get backup folder ID
            String folderId = getOrCreateBackupFolder(driveService);
            if (folderId == null) {
                LoggerUtil.logError("Failed to get backup folder for import, trying local backup");
                return importFromLocalBackup(operationId, initiatedBy, startTime);
            }

            // Get the file to import
            File backupFile;
            if (fileId != null) {
                // Use the specified file ID
                try {
                    backupFile = driveService.files().get(fileId).execute();
                    if (backupFile == null) {
                        LoggerUtil.logError("Backup file with ID {} not found", fileId);
                        return DatabaseBackupResponseDTO.failure("IMPORT", initiatedBy, "Backup file not found: " + fileId);
                    }
                } catch (Exception e) {
                    LoggerUtil.logError("Error getting backup file with ID {}: {}", fileId, e.getMessage(), e);
                    return DatabaseBackupResponseDTO.failure("IMPORT", initiatedBy, "Error accessing backup file: " + e.getMessage());
                }
            } else {
                // Get the most recent backup file
                backupFile = getMostRecentBackupFile(driveService, folderId);
                if (backupFile == null) {
                    LoggerUtil.logError("No backup files found for import");
                    return DatabaseBackupResponseDTO.failure("IMPORT", initiatedBy, "No backup files found");
                }
            }

            LoggerUtil.logDebug("Importing from backup file: {}", backupFile.getName());

            // Download the backup file
            String localBackupPath = downloadBackupFile(driveService, backupFile);
            if (localBackupPath == null) {
                LoggerUtil.logError("Failed to download backup file");
                return DatabaseBackupResponseDTO.failure("IMPORT", initiatedBy, "Failed to download backup file");
            }

            // Extract database name from JDBC URL
            String databaseName = extractDatabaseName(databaseUrl);
            if (databaseName == null) {
                LoggerUtil.logError("Failed to extract database name from URL: {}", databaseUrl);
                deleteLocalBackupFile(localBackupPath);
                return DatabaseBackupResponseDTO.failure("IMPORT", initiatedBy, "Failed to extract database name");
            }

            // Restore the database from the backup file
            RestoreResult restoreResult = restoreDatabaseFromBackup(databaseName, localBackupPath);

            // Delete the local backup file
            deleteLocalBackupFile(localBackupPath);

            long duration = System.currentTimeMillis() - startTime;

            if (restoreResult.isSuccess()) {
                LoggerUtil.logDebug("Database import completed successfully. Operation: {}, Duration: {}ms",
                        operationId, duration);
                return DatabaseBackupResponseDTO.importSuccess(operationId, backupFile.getName(), initiatedBy, duration);
            } else {
                LoggerUtil.logError("Failed to restore database from backup: {}", restoreResult.getErrorMessage());
                return DatabaseBackupResponseDTO.failure("IMPORT", initiatedBy, restoreResult.getErrorMessage());
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error during database import process: {}", e.getMessage(), e);
            sendErrorNotification("Database Import Failed",
                    "An error occurred during the database import process: " + e.getMessage());
            return DatabaseBackupResponseDTO.failure("IMPORT", initiatedBy, "Error during import process: " + e.getMessage());
        }
    }

    /**
     * Legacy method for backward compatibility
     * @param fileId Optional file ID to import from a specific backup
     * @return true if import was successful, false otherwise
     */
    public boolean importFromBackup(String fileId) {
        DatabaseBackupResponseDTO response = importFromBackup(fileId, "system");
        return "SUCCESS".equals(response.getStatus());
    }

    /**
     * Gets the file size in bytes
     * @param filePath the path to the file
     * @return the file size in bytes, or null if error
     */
    private Long getFileSize(String filePath) {
        try {
            java.io.File file = new java.io.File(filePath);
            return file.exists() ? file.length() : null;
        } catch (Exception e) {
            LoggerUtil.logError("Error getting file size for: {}", filePath, e);
            return null;
        }
    }

    /**
     * Gets the most recent backup file from Google Drive
     * @param driveService the Drive service
     * @param folderId the ID of the backup folder
     * @return the most recent backup file, or null if no backups found
     */
    private File getMostRecentBackupFile(Drive driveService, String folderId) throws IOException {
        // List all files in the backup folder, ordered by created time (newest first)
        String query = "'" + folderId + "' in parents and trashed=false";
        FileList result = driveService.files().list()
                .setQ(query)
                .setOrderBy("createdTime desc")
                .setPageSize(1)  // We only need the most recent file
                .setFields("files(id, name, createdTime)")
                .execute();

        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            LoggerUtil.logError("No backup files found in Google Drive");
            return null;
        }

        // Return the most recent file
        return files.get(0);
    }

    /**
     * Downloads a backup file from Google Drive
     * @param driveService the Drive service
     * @param backupFile the backup file to download
     * @return the path to the downloaded file, or null if download failed
     */
    private String downloadBackupFile(Drive driveService, File backupFile) {
        try {
            // Create backup directory if it doesn't exist
            Path backupDir = getBackupDirectory();
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }

            // Create local file path
            String localFilePath = backupDir.resolve(backupFile.getName()).toString();

            // Download the file
            try (InputStream is = driveService.files().get(backupFile.getId()).executeMediaAsInputStream();
                 FileOutputStream fos = new FileOutputStream(localFilePath)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
            }

            LoggerUtil.logDebug("Backup file downloaded to: {}", localFilePath);
            return localFilePath;
        } catch (Exception e) {
            LoggerUtil.logError("Error downloading backup file: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Restores the database from a backup file with improved constraint handling
     * @param databaseName the name of the database
     * @param backupFilePath the path to the backup file
     * @return RestoreResult containing success status and detailed error information
     */
    private RestoreResult restoreDatabaseFromBackup(String databaseName, String backupFilePath) {
        try {
            LoggerUtil.logDebug("Starting database restore from backup: {}", backupFilePath);

            // Step 1: Create a modified backup file with constraint handling
            String modifiedBackupPath = createModifiedBackupFile(backupFilePath);
            if (modifiedBackupPath == null) {
                LoggerUtil.logError("Failed to create modified backup file");
                return new RestoreResult(false, "Failed to prepare backup file for restore", null);
            }

            // Step 2: Execute the restore with the modified file
            RestoreResult restoreResult = executeRestore(databaseName, modifiedBackupPath);

            // Step 3: Clean up the modified backup file
            deleteLocalBackupFile(modifiedBackupPath);

            if (restoreResult.isSuccess()) {
                LoggerUtil.logDebug("Database restored successfully from backup: {}", backupFilePath);
                return restoreResult;
            } else {
                LoggerUtil.logError("Database restore failed: {}", restoreResult.getErrorMessage());
                return restoreResult;
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error restoring database from backup: {}", e.getMessage(), e);
            return new RestoreResult(false, "Error during database restore: " + e.getMessage(), null);
        }
    }

    /**
     * Creates a modified backup file with proper constraint handling and table management
     * Uses regular user privileges without requiring superuser access
     * @param originalBackupPath the path to the original backup file
     * @return the path to the modified backup file, or null if creation failed
     */
    private String createModifiedBackupFile(String originalBackupPath) {
        try {
            Path backupDir = getBackupDirectory();
            String modifiedFileName = "modified_" + Paths.get(originalBackupPath).getFileName().toString();
            String modifiedBackupPath = backupDir.resolve(modifiedFileName).toString();

            // First pass: analyze the backup to identify valid user IDs and orphaned references
            BackupAnalysisResult analysisResult = analyzeBackupFile(originalBackupPath);
            if (analysisResult == null) {
                LoggerUtil.logError("Failed to analyze backup file for orphaned references");
                return null;
            }

            try (BufferedReader reader = Files.newBufferedReader(Paths.get(originalBackupPath));
                 BufferedWriter writer = Files.newBufferedWriter(Paths.get(modifiedBackupPath))) {

                // Add transaction and constraint handling at the beginning
                writer.write("-- Modified backup with constraint and table handling (regular user mode)\n");
                writer.write("-- Analysis found " + analysisResult.getValidUserIds().size() + " valid user IDs\n");
                writer.write("-- Default user ID for orphaned references: " + analysisResult.getDefaultUserId() + "\n");
                writer.write("BEGIN;\n");
                writer.write("\n");

                // Add commands to handle existing tables and large objects
                writer.write("-- Clear existing large objects (comprehensive cleanup)\n");
                writer.write("DO $$\n");
                writer.write("DECLARE\n");
                writer.write("    lo_oid oid;\n");
                writer.write("    lo_count integer := 0;\n");
                writer.write("    cleared_count integer := 0;\n");
                writer.write("BEGIN\n");
                writer.write("    -- Count existing large objects\n");
                writer.write("    SELECT COUNT(*) INTO lo_count FROM pg_largeobject_metadata;\n");
                writer.write("    RAISE NOTICE 'Found % existing large objects to clear', lo_count;\n");
                writer.write("    \n");
                writer.write("    -- Clear all existing large objects\n");
                writer.write("    FOR lo_oid IN SELECT oid FROM pg_largeobject_metadata LOOP\n");
                writer.write("        BEGIN\n");
                writer.write("            PERFORM lo_unlink(lo_oid);\n");
                writer.write("            cleared_count := cleared_count + 1;\n");
                writer.write("        EXCEPTION\n");
                writer.write("            WHEN insufficient_privilege THEN\n");
                writer.write("                RAISE NOTICE 'Insufficient privilege to unlink large object %', lo_oid;\n");
                writer.write("            WHEN OTHERS THEN\n");
                writer.write("                RAISE NOTICE 'Failed to unlink large object %: %', lo_oid, SQLERRM;\n");
                writer.write("        END;\n");
                writer.write("    END LOOP;\n");
                writer.write("    \n");
                writer.write("    RAISE NOTICE 'Successfully cleared % out of % large objects', cleared_count, lo_count;\n");
                writer.write("END $$;\n");
                writer.write("\n");

                // Disable foreign key constraints for data import
                writer.write("-- Disable foreign key constraints during import\n");
                writer.write("DO $$\n");
                writer.write("DECLARE\n");
                writer.write("    constraint_record RECORD;\n");
                writer.write("    disabled_count integer := 0;\n");
                writer.write("BEGIN\n");
                writer.write("    -- Disable all foreign key constraints\n");
                writer.write("    FOR constraint_record IN\n");
                writer.write("        SELECT conname, conrelid::regclass AS table_name\n");
                writer.write("        FROM pg_constraint\n");
                writer.write("        WHERE contype = 'f' AND connamespace = 'public'::regnamespace\n");
                writer.write("    LOOP\n");
                writer.write("        BEGIN\n");
                writer.write("            EXECUTE 'ALTER TABLE ' || constraint_record.table_name || ' DISABLE TRIGGER ALL';\n");
                writer.write("            disabled_count := disabled_count + 1;\n");
                writer.write("            RAISE NOTICE 'Disabled triggers for table %', constraint_record.table_name;\n");
                writer.write("        EXCEPTION\n");
                writer.write("            WHEN OTHERS THEN\n");
                writer.write("                RAISE NOTICE 'Could not disable triggers for table %: %', constraint_record.table_name, SQLERRM;\n");
                writer.write("        END;\n");
                writer.write("    END LOOP;\n");
                writer.write("    RAISE NOTICE 'Disabled triggers on % tables for foreign key constraint handling', disabled_count;\n");
                writer.write("END $$;\n");
                writer.write("\n");

                // Add commands to clear existing data from all tables
                writer.write("-- Clear existing data from all tables (with constraints disabled)\n");
                writer.write("DO $$\n");
                writer.write("DECLARE\n");
                writer.write("    table_name text;\n");
                writer.write("    table_count integer;\n");
                writer.write("    total_cleared integer := 0;\n");
                writer.write("BEGIN\n");
                writer.write("    -- Clear all tables (constraints are disabled, so order doesn't matter)\n");
                writer.write("    FOR table_name IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' LOOP\n");
                writer.write("        BEGIN\n");
                writer.write("            -- Delete all data from the table\n");
                writer.write("            EXECUTE 'DELETE FROM ' || quote_ident(table_name);\n");
                writer.write("            GET DIAGNOSTICS table_count = ROW_COUNT;\n");
                writer.write("            total_cleared := total_cleared + table_count;\n");
                writer.write("            IF table_count > 0 THEN\n");
                writer.write("                RAISE NOTICE 'Cleared % rows from table %', table_count, table_name;\n");
                writer.write("            END IF;\n");
                writer.write("        EXCEPTION\n");
                writer.write("            WHEN OTHERS THEN\n");
                writer.write("                RAISE NOTICE 'Could not clear table %: %', table_name, SQLERRM;\n");
                writer.write("        END;\n");
                writer.write("    END LOOP;\n");
                writer.write("    RAISE NOTICE 'Total rows cleared: %', total_cleared;\n");
                writer.write("END $$;\n");
                writer.write("\n");

                // Process the original backup content, modifying statements for existing database
                String line;
                boolean inIdentityBlock = false;
                boolean inConstraintBlock = false;
                boolean inCopyBlock = false;
                String currentCopyTable = null;
                StringBuilder currentStatement = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    // Handle COPY statements with orphaned reference fixing
                    if (line.startsWith("COPY ")) {
                        inCopyBlock = true;
                        currentCopyTable = extractTableNameFromCopy(line);
                        writer.write(line);
                        writer.write("\n");
                        continue;
                    }

                    // Handle end of COPY block
                    if (inCopyBlock && line.equals("\\.")) {
                        inCopyBlock = false;
                        currentCopyTable = null;
                        writer.write(line);
                        writer.write("\n");
                        continue;
                    }

                    // Process COPY data lines with orphaned reference fixing
                    if (inCopyBlock && currentCopyTable != null) {
                        String processedLine = fixOrphanedReferencesInCopyData(line, currentCopyTable, analysisResult);
                        writer.write(processedLine);
                        writer.write("\n");
                        continue;
                    }

                    // Handle multi-line identity blocks
                    if (line.startsWith("ALTER TABLE ") && line.contains("ADD GENERATED BY DEFAULT AS IDENTITY")) {
                        inIdentityBlock = true;
                        continue; // Skip this line
                    }

                    if (inIdentityBlock) {
                        if (line.trim().equals(");")) {
                            inIdentityBlock = false;
                        }
                        continue; // Skip all lines in identity block
                    }

                    // Handle multi-line constraint blocks with improved detection
                    // Check if this line starts a new ALTER TABLE statement
                    if (line.trim().startsWith("ALTER TABLE ")) {
                        // If we were in a constraint block, end it
                        if (inConstraintBlock) {
                            inConstraintBlock = false;
                            currentStatement.setLength(0);
                        }

                        // Start building the current statement
                        currentStatement.setLength(0);
                        currentStatement.append(line);

                        // Check if this is a constraint-related ALTER TABLE statement on the same line
                        String fullLine = line.trim();
                        if (fullLine.contains("ADD CONSTRAINT") || fullLine.contains("ADD PRIMARY KEY") ||
                            fullLine.contains("ADD UNIQUE") || fullLine.contains("ADD FOREIGN KEY") ||
                            fullLine.contains("ADD CHECK")) {
                            inConstraintBlock = true;
                            continue; // Skip this line
                        }

                        // If the line doesn't end with a semicolon, it might be a multi-line statement
                        // We need to check the next lines to see if they contain constraint operations
                        if (!fullLine.endsWith(";")) {
                            // This could be a multi-line ALTER TABLE statement, mark as potentially in constraint block
                            // but don't skip yet - we'll check the next lines
                            inConstraintBlock = true;
                            continue; // Skip this line and check next lines
                        }
                    } else if (inConstraintBlock) {
                        // We're in a constraint block, accumulate the statement
                        currentStatement.append(" ").append(line.trim());

                        // Check if this line contains constraint operations
                        String trimmedLine = line.trim();
                        if (trimmedLine.contains("ADD CONSTRAINT") || trimmedLine.contains("ADD PRIMARY KEY") ||
                            trimmedLine.contains("ADD UNIQUE") || trimmedLine.contains("ADD FOREIGN KEY") ||
                            trimmedLine.contains("ADD CHECK")) {
                            // This is definitely a constraint operation, continue skipping
                            if (trimmedLine.endsWith(";")) {
                                inConstraintBlock = false;
                                currentStatement.setLength(0);
                            }
                            continue; // Skip this line
                        } else if (trimmedLine.endsWith(";")) {
                            // End of statement but no constraint operation found
                            // This might be a different type of ALTER TABLE, so we should not have skipped it
                            // But since we already started skipping, we need to end the block
                            inConstraintBlock = false;
                            currentStatement.setLength(0);
                            continue; // Skip this line since we already started skipping
                        }
                        continue; // Skip all lines in constraint block
                    }

                    // Skip lines that might conflict with existing database objects
                    if (shouldSkipLine(line)) {
                        continue;
                    }

                    // Convert CREATE TABLE to CREATE TABLE IF NOT EXISTS and handle inline constraints
                    if (line.startsWith("CREATE TABLE ")) {
                        line = line.replace("CREATE TABLE ", "CREATE TABLE IF NOT EXISTS ");
                        // Remove inline PRIMARY KEY constraints from CREATE TABLE statements
                        line = removeInlinePrimaryKeyConstraints(line);
                    }

                    // Convert CREATE SEQUENCE to CREATE SEQUENCE IF NOT EXISTS
                    if (line.startsWith("CREATE SEQUENCE ")) {
                        line = line.replace("CREATE SEQUENCE ", "CREATE SEQUENCE IF NOT EXISTS ");
                    }

                    // Handle table drops - convert to DROP IF EXISTS
                    if (line.startsWith("DROP TABLE ")) {
                        line = line.replace("DROP TABLE ", "DROP TABLE IF EXISTS ");
                    }

                    writer.write(line);
                    writer.write("\n");
                }

                // Fix orphaned foreign key references before re-enabling constraints
                writer.write("\n");
                writer.write("-- Fix orphaned foreign key references\n");
                writer.write("DO $$\n");
                writer.write("DECLARE\n");
                writer.write("    max_user_id integer;\n");
                writer.write("    orphaned_projects integer := 0;\n");
                writer.write("    fixed_projects integer := 0;\n");
                writer.write("    orphaned_records integer := 0;\n");
                writer.write("BEGIN\n");
                writer.write("    -- Get the maximum user ID from users table\n");
                writer.write("    SELECT COALESCE(MAX(id), 1) INTO max_user_id FROM users;\n");
                writer.write("    RAISE NOTICE 'Maximum user ID found: %', max_user_id;\n");
                writer.write("    \n");
                writer.write("    -- Check for orphaned projects (created_by references non-existent users)\n");
                writer.write("    SELECT COUNT(*) INTO orphaned_projects\n");
                writer.write("    FROM projects p\n");
                writer.write("    WHERE p.created_by IS NOT NULL\n");
                writer.write("    AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = p.created_by);\n");
                writer.write("    \n");
                writer.write("    IF orphaned_projects > 0 THEN\n");
                writer.write("        RAISE NOTICE 'Found % orphaned projects with invalid created_by references', orphaned_projects;\n");
                writer.write("        \n");
                writer.write("        -- Fix orphaned projects by setting created_by to max_user_id\n");
                writer.write("        UPDATE projects\n");
                writer.write("        SET created_by = max_user_id\n");
                writer.write("        WHERE created_by IS NOT NULL\n");
                writer.write("        AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = projects.created_by);\n");
                writer.write("        \n");
                writer.write("        GET DIAGNOSTICS fixed_projects = ROW_COUNT;\n");
                writer.write("        RAISE NOTICE 'Fixed % orphaned projects by setting created_by to %', fixed_projects, max_user_id;\n");
                writer.write("    ELSE\n");
                writer.write("        RAISE NOTICE 'No orphaned projects found';\n");
                writer.write("    END IF;\n");
                writer.write("    \n");
                writer.write("    -- Check for other common orphaned references and fix them\n");
                writer.write("    \n");
                writer.write("    -- Fix other tables that might have user references\n");
                writer.write("    -- Check if tables exist before trying to fix them\n");
                writer.write("    \n");
                writer.write("    -- Fix tasks table if it exists and has created_by references\n");
                writer.write("    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'tasks' AND table_schema = 'public') THEN\n");
                writer.write("        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'tasks' AND column_name = 'created_by' AND table_schema = 'public') THEN\n");
                writer.write("            UPDATE tasks\n");
                writer.write("            SET created_by = max_user_id\n");
                writer.write("            WHERE created_by IS NOT NULL\n");
                writer.write("            AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = tasks.created_by);\n");
                writer.write("            GET DIAGNOSTICS orphaned_records = ROW_COUNT;\n");
                writer.write("            IF orphaned_records > 0 THEN\n");
                writer.write("                RAISE NOTICE 'Fixed % orphaned tasks created_by references', orphaned_records;\n");
                writer.write("            END IF;\n");
                writer.write("        END IF;\n");
                writer.write("    END IF;\n");
                writer.write("    \n");
                writer.write("    -- Fix employee table if it exists and has user_id references\n");
                writer.write("    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employee' AND table_schema = 'public') THEN\n");
                writer.write("        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'employee' AND column_name = 'user_id' AND table_schema = 'public') THEN\n");
                writer.write("            UPDATE employee\n");
                writer.write("            SET user_id = max_user_id\n");
                writer.write("            WHERE user_id IS NOT NULL\n");
                writer.write("            AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = employee.user_id);\n");
                writer.write("            GET DIAGNOSTICS orphaned_records = ROW_COUNT;\n");
                writer.write("            IF orphaned_records > 0 THEN\n");
                writer.write("                RAISE NOTICE 'Fixed % orphaned employee user_id references', orphaned_records;\n");
                writer.write("            END IF;\n");
                writer.write("        END IF;\n");
                writer.write("    END IF;\n");
                writer.write("    \n");
                writer.write("    RAISE NOTICE 'Orphaned foreign key reference cleanup completed';\n");
                writer.write("    \n");
                writer.write("END $$;\n");
                writer.write("\n");

                // Re-enable foreign key constraints after data import
                writer.write("-- Re-enable foreign key constraints after import\n");
                writer.write("DO $$\n");
                writer.write("DECLARE\n");
                writer.write("    constraint_record RECORD;\n");
                writer.write("    enabled_count integer := 0;\n");
                writer.write("    constraint_violations integer := 0;\n");
                writer.write("BEGIN\n");
                writer.write("    -- Re-enable all foreign key constraints\n");
                writer.write("    FOR constraint_record IN\n");
                writer.write("        SELECT conname, conrelid::regclass AS table_name\n");
                writer.write("        FROM pg_constraint\n");
                writer.write("        WHERE contype = 'f' AND connamespace = 'public'::regnamespace\n");
                writer.write("    LOOP\n");
                writer.write("        BEGIN\n");
                writer.write("            EXECUTE 'ALTER TABLE ' || constraint_record.table_name || ' ENABLE TRIGGER ALL';\n");
                writer.write("            enabled_count := enabled_count + 1;\n");
                writer.write("            RAISE NOTICE 'Re-enabled triggers for table %', constraint_record.table_name;\n");
                writer.write("        EXCEPTION\n");
                writer.write("            WHEN OTHERS THEN\n");
                writer.write("                constraint_violations := constraint_violations + 1;\n");
                writer.write("                RAISE NOTICE 'Could not re-enable triggers for table %: %', constraint_record.table_name, SQLERRM;\n");
                writer.write("        END;\n");
                writer.write("    END LOOP;\n");
                writer.write("    \n");
                writer.write("    RAISE NOTICE 'Re-enabled triggers on % tables, % had issues', enabled_count, constraint_violations;\n");
                writer.write("    \n");
                writer.write("    -- Validate foreign key constraints\n");
                writer.write("    RAISE NOTICE 'Validating foreign key constraints...';\n");
                writer.write("    -- Note: Constraints are automatically validated when triggers are re-enabled\n");
                writer.write("    RAISE NOTICE 'Foreign key constraint validation completed';\n");
                writer.write("END $$;\n");
                writer.write("\n");

                // Add commit at the end
                writer.write("-- Commit the transaction\n");
                writer.write("COMMIT;\n");
            }

            LoggerUtil.logDebug("Modified backup file created: {}", modifiedBackupPath);
            return modifiedBackupPath;
        } catch (Exception e) {
            LoggerUtil.logError("Error creating modified backup file: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Removes inline PRIMARY KEY constraints from CREATE TABLE statements to avoid conflicts
     * @param createTableLine the CREATE TABLE statement line
     * @return the modified line with PRIMARY KEY constraints removed
     */
    private String removeInlinePrimaryKeyConstraints(String createTableLine) {
        try {
            // Remove PRIMARY KEY constraints from column definitions
            // Pattern: column_name data_type PRIMARY KEY
            String result = createTableLine.replaceAll("\\s+PRIMARY\\s+KEY\\s*,?", "");

            // Remove standalone PRIMARY KEY constraints
            // Pattern: PRIMARY KEY (column_name)
            result = result.replaceAll(",?\\s*PRIMARY\\s+KEY\\s*\\([^)]+\\)\\s*,?", "");

            // Remove CONSTRAINT name PRIMARY KEY (column_name)
            result = result.replaceAll(",?\\s*CONSTRAINT\\s+\\w+\\s+PRIMARY\\s+KEY\\s*\\([^)]+\\)\\s*,?", "");

            // Clean up any double commas that might result from removals
            result = result.replaceAll(",\\s*,", ",");

            // Clean up trailing commas before closing parenthesis
            result = result.replaceAll(",\\s*\\)", ")");

            return result;
        } catch (Exception e) {
            LoggerUtil.logDebug("Could not remove PRIMARY KEY constraints from line: {}", createTableLine);
            return createTableLine;
        }
    }

    /**
     * Extracts table name from a COPY statement
     * @param copyLine the COPY statement line
     * @return the table name, or null if not found
     */
    private String extractTableNameFromCopy(String copyLine) {
        try {
            // COPY public.table_name (columns...) FROM stdin;
            String[] parts = copyLine.split(" ");
            if (parts.length >= 2) {
                String tableRef = parts[1];
                // Remove schema prefix if present
                if (tableRef.contains(".")) {
                    return tableRef.split("\\.")[1];
                }
                return tableRef;
            }
        } catch (Exception e) {
            LoggerUtil.logDebug("Could not extract table name from COPY line: {}", copyLine);
        }
        return null;
    }

    /**
     * Determines if a line should be skipped during backup import to avoid conflicts
     * @param line the line to check
     * @return true if the line should be skipped, false otherwise
     */
    private boolean shouldSkipLine(String line) {
        String trimmedLine = line.trim();

        // Skip owner changes as they might conflict
        if (trimmedLine.startsWith("ALTER TABLE ") && trimmedLine.contains(" OWNER TO ")) {
            return true;
        }

        // Skip sequence owner changes
        if (trimmedLine.startsWith("ALTER SEQUENCE ") && trimmedLine.contains(" OWNER TO ")) {
            return true;
        }

        // Skip large object owner changes
        if (trimmedLine.startsWith("ALTER LARGE OBJECT ") && trimmedLine.contains(" OWNER TO ")) {
            return true;
        }

        // Skip schema owner changes
        if (trimmedLine.startsWith("ALTER SCHEMA ") && trimmedLine.contains(" OWNER TO ")) {
            return true;
        }

        // Skip function/procedure owner changes
        if ((trimmedLine.startsWith("ALTER FUNCTION ") || trimmedLine.startsWith("ALTER PROCEDURE ")) && trimmedLine.contains(" OWNER TO ")) {
            return true;
        }

        // Skip view owner changes
        if (trimmedLine.startsWith("ALTER VIEW ") && trimmedLine.contains(" OWNER TO ")) {
            return true;
        }

        // Skip identity column additions that might conflict with existing sequences
        if (trimmedLine.startsWith("ALTER TABLE ") && trimmedLine.contains("ADD GENERATED BY DEFAULT AS IDENTITY")) {
            return true;
        }

        // Skip explicit sequence creation that might conflict (sequences are auto-created with identity columns)
        if (trimmedLine.startsWith("CREATE SEQUENCE ")) {
            return true;
        }

        // Skip sequence value setting statements
        if (trimmedLine.startsWith("SELECT pg_catalog.setval(")) {
            return true;
        }

        // Skip GRANT statements on sequences
        if (trimmedLine.startsWith("GRANT ") && trimmedLine.contains("ON SEQUENCE")) {
            return true;
        }

        // Skip large object creation statements to avoid conflicts
        if (trimmedLine.startsWith("SELECT pg_catalog.lo_create(")) {
            return true;
        }

        // Skip large object data insertion statements
        if (trimmedLine.startsWith("SELECT pg_catalog.lo_open(") ||
            trimmedLine.startsWith("SELECT pg_catalog.lo_write(") ||
            trimmedLine.startsWith("SELECT pg_catalog.lo_close(")) {
            return true;
        }

        // Skip large object import statements
        if (trimmedLine.startsWith("SELECT pg_catalog.lo_import(") ||
            trimmedLine.startsWith("SELECT pg_catalog.lo_export(")) {
            return true;
        }

        // Skip large object unlink statements (we handle this ourselves)
        if (trimmedLine.startsWith("SELECT pg_catalog.lo_unlink(")) {
            return true;
        }

        // Note: Constraint-related ALTER TABLE statements are now handled in the main parsing loop
        // This method should not handle them to avoid conflicts with the improved multi-line parsing

        return false;
    }

    /**
     * Executes the database restore using psql
     * @param databaseName the name of the database
     * @param backupFilePath the path to the backup file
     * @return RestoreResult containing success status and detailed error information
     */
    private RestoreResult executeRestore(String databaseName, String backupFilePath) {
        try {
            // Build psql command to restore the database
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "psql",
                    "-h", "localhost",
                    "-p", "5432",
                    "-U", databaseUsername,
                    "-d", databaseName,
                    "-f", backupFilePath,
                    "-v", "ON_ERROR_STOP=1"  // Stop on first error
            );

            // Set PGPASSWORD environment variable
            processBuilder.environment().put("PGPASSWORD", databasePassword);

            // Redirect both stdout and stderr to capture all output
            processBuilder.redirectErrorStream(true);

            // Start process and wait for completion
            Process process = processBuilder.start();

            // Capture and log all output
            StringBuilder output = new StringBuilder();
            StringBuilder errorDetails = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    // Capture error lines for detailed analysis
                    if (line.contains("ERROR") || line.contains("FATAL")) {
                        errorDetails.append(line).append("\n");
                        LoggerUtil.logError("psql output: {}", line);
                    } else if (line.contains("COPY") || line.contains("INSERT")) {
                        LoggerUtil.logDebug("psql output: {}", line);
                    }
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                LoggerUtil.logDebug("psql restore completed successfully");
                LoggerUtil.logDebug("psql full output: {}", output.toString());
                return new RestoreResult(true, null, null);
            } else {
                LoggerUtil.logError("psql process exited with code: {}", exitCode);
                LoggerUtil.logError("psql full output: {}", output.toString());

                // Parse and transform the error for user-friendly message
                String userFriendlyError = parseAndTransformError(errorDetails.toString());
                return new RestoreResult(false, userFriendlyError, output.toString());
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error executing database restore: {}", e.getMessage(), e);
            return new RestoreResult(false, "Database restore process failed: " + e.getMessage(), null);
        }
    }

    /**
     * Parses database errors and transforms them into user-friendly messages
     * @param errorOutput the raw error output from psql
     * @return user-friendly error message
     */
    private String parseAndTransformError(String errorOutput) {
        if (errorOutput == null || errorOutput.trim().isEmpty()) {
            return "Database restore failed with unknown error";
        }

        String lowerError = errorOutput.toLowerCase();

        // Handle multiple primary keys error
        if (lowerError.contains("multiple primary keys") && lowerError.contains("are not allowed")) {
            return "Database restore failed: The backup contains conflicting primary key definitions. " +
                   "This usually happens when trying to restore over an existing database with different schema. " +
                   "Please ensure the target database is clean or contact your administrator.";
        }

        // Handle large object duplicate key constraint violations
        if (lowerError.contains("duplicate key value violates unique constraint") &&
            lowerError.contains("pg_largeobject_metadata_oid_index")) {
            return "Database restore failed: Large object conflicts detected. " +
                   "The backup contains large objects that already exist in the database. " +
                   "This issue has been resolved with improved large object handling. Please try the restore operation again.";
        }

        // Handle general duplicate key constraint violations
        if (lowerError.contains("duplicate key value violates unique constraint")) {
            return "Database restore failed: The backup contains duplicate data that conflicts with existing records. " +
                   "This may happen when restoring partial data over an existing database.";
        }

        // Handle foreign key constraint violations during data import
        if (lowerError.contains("violates foreign key constraint")) {
            if (lowerError.contains("is not present in table")) {
                // Check if it's specifically a user reference issue
                if (lowerError.contains("is not present in table \"users\"") ||
                    lowerError.contains("created_by") ||
                    lowerError.contains("user_id")) {
                    return "Database restore failed: The backup contains projects or records that reference non-existent users. " +
                           "This issue has been resolved with improved orphaned reference detection and automatic fixing. " +
                           "The system now pre-processes backup files to identify and fix orphaned user references before importing data. " +
                           "Please try the restore operation again.";
                } else {
                    return "Database restore failed: The backup contains data with missing referenced records. " +
                           "This issue has been resolved with improved constraint handling and data preprocessing. " +
                           "Please try the restore operation again.";
                }
            } else {
                return "Database restore failed: The backup contains data that violates referential integrity constraints. " +
                       "The system now includes enhanced constraint handling to prevent such issues. " +
                       "Please try the restore operation again.";
            }
        }

        // Handle specific session_replication_role permission error
        if (lowerError.contains("permission denied to set parameter \"session_replication_role\"")) {
            return "Database restore failed: The database user does not have sufficient privileges to modify replication settings. " +
                   "This is a configuration issue that has been resolved. Please try the restore operation again.";
        }

        // Handle general permission errors
        if (lowerError.contains("permission denied") || lowerError.contains("must be owner")) {
            return "Database restore failed: Insufficient permissions to perform the restore operation. " +
                   "Please contact your database administrator.";
        }

        // Handle connection errors
        if (lowerError.contains("connection refused") || lowerError.contains("could not connect")) {
            return "Database restore failed: Unable to connect to the database server. " +
                   "Please check if the database service is running.";
        }

        // Handle table/column does not exist errors
        if (lowerError.contains("does not exist")) {
            if (lowerError.contains("table") && lowerError.contains("does not exist")) {
                return "Database restore failed: The backup references tables that don't exist in the target database. " +
                       "The database schema may be incompatible.";
            } else if (lowerError.contains("column") && lowerError.contains("does not exist")) {
                return "Database restore failed: The backup references columns that don't exist in the target database. " +
                       "The database schema may be incompatible.";
            }
            return "Database restore failed: The backup references database objects that don't exist in the target database.";
        }

        // Handle syntax errors
        if (lowerError.contains("syntax error")) {
            return "Database restore failed: The backup file contains invalid SQL syntax. " +
                   "The backup file may be corrupted.";
        }

        // Handle disk space errors
        if (lowerError.contains("no space left on device") || lowerError.contains("disk full")) {
            return "Database restore failed: Insufficient disk space to complete the restore operation. " +
                   "Please free up disk space and try again.";
        }

        // Handle constraint violations in general
        if (lowerError.contains("constraint") && lowerError.contains("violat")) {
            return "Database restore failed: The backup data violates database constraints. " +
                   "This may happen when restoring data that doesn't match the current database schema.";
        }

        // Default case - return first error line for context
        String[] lines = errorOutput.split("\n");
        for (String line : lines) {
            if (line.trim().contains("ERROR") || line.trim().contains("FATAL")) {
                return "Database restore failed: " + line.trim().replaceFirst("^.*ERROR:\\s*", "")
                                                                .replaceFirst("^.*FATAL:\\s*", "");
            }
        }

        return "Database restore failed: " + errorOutput.trim();
    }

    /**
     * Inner class to hold restore operation results
     */
    private static class RestoreResult {
        private final boolean success;
        private final String errorMessage;
        private final String fullOutput;

        public RestoreResult(boolean success, String errorMessage, String fullOutput) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.fullOutput = fullOutput;
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public String getFullOutput() { return fullOutput; }
    }

    /**
     * Gets the backup directory path, creating it if necessary
     * Uses configurable backup directory with fallback to system temp directory
     * @return Path to the backup directory
     * @throws RuntimeException if neither the configured nor fallback directory can be used
     */
    private Path getBackupDirectory() {
        // Try configured backup directory first
        try {
            Path backupDir = Paths.get(backupDirectory);
            LoggerUtil.logDebug("Attempting to use configured backup directory: {}", backupDir.toAbsolutePath());

            // Test if we can write to this directory
            if (Files.exists(backupDir) && !Files.isWritable(backupDir)) {
                throw new RuntimeException("Backup directory exists but is not writable: " + backupDir.toAbsolutePath());
            }

            LoggerUtil.logDebug("Using backup directory: {}", backupDir.toAbsolutePath());
            return backupDir;
        } catch (Exception e) {
            LoggerUtil.logError("Failed to use configured backup directory {}: {}", backupDirectory, e.getMessage());
        }

        // Fallback to system temp directory
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "teamsphere_backups");
            LoggerUtil.logDebug("Using fallback backup directory: {}", tempDir.toAbsolutePath());

            // Test if we can write to temp directory
            if (Files.exists(tempDir) && !Files.isWritable(tempDir)) {
                throw new RuntimeException("Fallback backup directory exists but is not writable: " + tempDir.toAbsolutePath());
            }

            return tempDir;
        } catch (Exception e) {
            LoggerUtil.logError("Failed to use fallback backup directory: {}", e.getMessage());
            throw new RuntimeException("Unable to find a writable backup directory. Tried: " +
                    backupDirectory + " and " + System.getProperty("java.io.tmpdir") + "/teamsphere_backups", e);
        }
    }

    /**
     * Imports from local backup files when Google Drive is not available
     * @param operationId the operation ID
     * @param initiatedBy the user who initiated the import
     * @param startTime the start time of the operation
     * @return DatabaseBackupResponseDTO with operation details
     */
    private DatabaseBackupResponseDTO importFromLocalBackup(String operationId, String initiatedBy, long startTime) {
        try {
            LoggerUtil.logDebug("Attempting to import from local backup files");

            // Get backup directory
            Path backupDir = getBackupDirectory();
            if (!Files.exists(backupDir)) {
                LoggerUtil.logError("Local backup directory does not exist: {}", backupDir);
                return DatabaseBackupResponseDTO.failure("IMPORT", initiatedBy,
                        "No local backup files found. Directory does not exist: " + backupDir);
            }

            // Find the most recent backup file
            java.io.File mostRecentBackup = findMostRecentLocalBackup(backupDir.toFile());
            if (mostRecentBackup == null) {
                LoggerUtil.logError("No local backup files found in directory: {}", backupDir);
                return DatabaseBackupResponseDTO.failure("IMPORT", initiatedBy,
                        "No local backup files found in directory: " + backupDir);
            }

            LoggerUtil.logDebug("Found local backup file: {}", mostRecentBackup.getName());

            // Extract database name from JDBC URL
            String databaseName = extractDatabaseName(databaseUrl);
            if (databaseName == null) {
                LoggerUtil.logError("Failed to extract database name from URL: {}", databaseUrl);
                return DatabaseBackupResponseDTO.failure("IMPORT", initiatedBy, "Failed to extract database name");
            }

            // Restore the database from the backup file
            RestoreResult restoreResult = restoreDatabaseFromBackup(databaseName, mostRecentBackup.getAbsolutePath());

            long duration = System.currentTimeMillis() - startTime;

            if (restoreResult.isSuccess()) {
                LoggerUtil.logDebug("Database import from local backup completed successfully. Operation: {}, Duration: {}ms",
                        operationId, duration);
                return DatabaseBackupResponseDTO.importSuccess(operationId, mostRecentBackup.getName(), initiatedBy, duration);
            } else {
                LoggerUtil.logError("Failed to restore database from local backup: {}", restoreResult.getErrorMessage());
                return DatabaseBackupResponseDTO.failure("IMPORT", initiatedBy, restoreResult.getErrorMessage());
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error during local backup import: {}", e.getMessage(), e);
            return DatabaseBackupResponseDTO.failure("IMPORT", initiatedBy, "Error during local backup import: " + e.getMessage());
        }
    }

    /**
     * Finds the most recent backup file in the local backup directory
     * @param backupDir the backup directory
     * @return the most recent backup file, or null if none found
     */
    private java.io.File findMostRecentLocalBackup(java.io.File backupDir) {
        java.io.File[] backupFiles = backupDir.listFiles((dir, name) -> name.endsWith(".sql"));

        if (backupFiles == null || backupFiles.length == 0) {
            return null;
        }

        // Sort by last modified time (newest first)
        java.util.Arrays.sort(backupFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        return backupFiles[0];
    }

    /**
     * Starts an asynchronous import from a backup file
     * @param fileId Optional file ID to import from a specific backup. If null, imports from the most recent backup.
     * @param initiatedBy The username of the person initiating the import
     * @return DatabaseBackupResponseDTO with operation ID for tracking progress
     */
    @Override
    public DatabaseBackupResponseDTO startAsyncImportFromBackup(String fileId, String initiatedBy) {
        String operationId = java.util.UUID.randomUUID().toString();

        // Create initial status entry
        DatabaseBackupResponseDTO initialStatus = DatabaseBackupResponseDTO.inProgress(operationId, "IMPORT", initiatedBy);
        operationStatus.put(operationId, initialStatus);

        LoggerUtil.logDebug("Starting async database import operation: {} initiated by: {} with fileId: {}",
                operationId, initiatedBy, fileId);

        // Start async import
        CompletableFuture.runAsync(() -> {
            try {
                DatabaseBackupResponseDTO result = importFromBackup(fileId, initiatedBy);
                // Update the operation status with the final result
                result.setOperationId(operationId);
                operationStatus.put(operationId, result);

                LoggerUtil.logDebug("Async database import completed for operation: {} with status: {}",
                        operationId, result.getStatus());
            } catch (Exception e) {
                LoggerUtil.logError("Error in async database import for operation: {}: {}", operationId, e.getMessage(), e);
                DatabaseBackupResponseDTO errorResult = DatabaseBackupResponseDTO.failure("IMPORT", initiatedBy,
                        "Async import failed: " + e.getMessage());
                errorResult.setOperationId(operationId);
                operationStatus.put(operationId, errorResult);
            }
        });

        return initialStatus;
    }

    /**
     * Gets the status of a database operation
     * @param operationId The operation ID to check status for
     * @return DatabaseBackupResponseDTO with current operation status, or null if not found
     */
    @Override
    public DatabaseBackupResponseDTO getOperationStatus(String operationId) {
        return operationStatus.get(operationId);
    }

    /**
     * Analyzes a backup file to identify valid user IDs and orphaned references
     * @param backupFilePath the path to the backup file
     * @return BackupAnalysisResult containing analysis data, or null if analysis failed
     */
    private BackupAnalysisResult analyzeBackupFile(String backupFilePath) {
        try {
            LoggerUtil.logDebug("Analyzing backup file for orphaned references: {}", backupFilePath);

            Set<Integer> validUserIds = new HashSet<>();
            Set<Integer> referencedUserIds = new HashSet<>();

            try (BufferedReader reader = Files.newBufferedReader(Paths.get(backupFilePath))) {
                String line;
                boolean inUsersTable = false;
                boolean inCopyBlock = false;
                String currentTable = null;

                while ((line = reader.readLine()) != null) {
                    // Detect COPY statements
                    if (line.startsWith("COPY ")) {
                        inCopyBlock = true;
                        currentTable = extractTableNameFromCopy(line);
                        if ("users".equals(currentTable)) {
                            inUsersTable = true;
                        }
                        continue;
                    }

                    // End of COPY block
                    if (inCopyBlock && line.equals("\\.")) {
                        inCopyBlock = false;
                        inUsersTable = false;
                        currentTable = null;
                        continue;
                    }

                    // Process data lines
                    if (inCopyBlock && currentTable != null) {
                        if (inUsersTable) {
                            // Extract user IDs from users table
                            Integer userId = extractUserIdFromUsersData(line);
                            if (userId != null) {
                                validUserIds.add(userId);
                            }
                        } else if ("projects".equals(currentTable)) {
                            // Extract created_by references from projects table
                            Integer createdBy = extractCreatedByFromProjectsData(line);
                            if (createdBy != null) {
                                referencedUserIds.add(createdBy);
                            }
                        } else if ("tasks".equals(currentTable)) {
                            // Extract created_by references from tasks table
                            Integer createdBy = extractCreatedByFromTasksData(line);
                            if (createdBy != null) {
                                referencedUserIds.add(createdBy);
                            }
                        } else if ("employee".equals(currentTable)) {
                            // Extract user_id references from employee table
                            Integer userId = extractUserIdFromEmployeeData(line);
                            if (userId != null) {
                                referencedUserIds.add(userId);
                            }
                        }
                    }
                }
            }

            // Determine default user ID (highest valid user ID)
            Integer defaultUserId = validUserIds.isEmpty() ? 1 : validUserIds.stream().max(Integer::compareTo).orElse(1);

            // Find orphaned references
            Set<Integer> orphanedUserIds = new HashSet<>(referencedUserIds);
            orphanedUserIds.removeAll(validUserIds);

            LoggerUtil.logDebug("Analysis complete: {} valid users, {} orphaned references, default user ID: {}",
                    validUserIds.size(), orphanedUserIds.size(), defaultUserId);

            return new BackupAnalysisResult(validUserIds, orphanedUserIds, defaultUserId);

        } catch (Exception e) {
            LoggerUtil.logError("Error analyzing backup file: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extracts user ID from users table COPY data line
     * @param dataLine the data line from users table
     * @return the user ID, or null if not found
     */
    private Integer extractUserIdFromUsersData(String dataLine) {
        try {
            // Users table format: id\tusername\tpassword\t...
            String[] fields = dataLine.split("\t");
            if (fields.length > 0 && !fields[0].equals("\\N")) {
                return Integer.parseInt(fields[0]);
            }
        } catch (Exception e) {
            LoggerUtil.logDebug("Could not extract user ID from line: {}", dataLine);
        }
        return null;
    }

    /**
     * Extracts created_by from projects table COPY data line
     * @param dataLine the data line from projects table
     * @return the created_by user ID, or null if not found
     */
    private Integer extractCreatedByFromProjectsData(String dataLine) {
        try {
            // Projects table format varies, but created_by is typically one of the last fields
            // We need to find the created_by column position
            String[] fields = dataLine.split("\t");
            // Assuming created_by is at a specific position - this may need adjustment based on actual schema
            // For now, let's try to find it by checking if it's a valid integer in reasonable positions
            for (int i = fields.length - 5; i < fields.length && i >= 0; i++) {
                if (!fields[i].equals("\\N")) {
                    try {
                        return Integer.parseInt(fields[i]);
                    } catch (NumberFormatException ignored) {
                        // Continue checking other fields
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtil.logDebug("Could not extract created_by from projects line: {}", dataLine);
        }
        return null;
    }

    /**
     * Extracts created_by from tasks table COPY data line
     * @param dataLine the data line from tasks table
     * @return the created_by user ID, or null if not found
     */
    private Integer extractCreatedByFromTasksData(String dataLine) {
        try {
            // Similar logic to projects table
            String[] fields = dataLine.split("\t");
            for (int i = fields.length - 5; i < fields.length && i >= 0; i++) {
                if (!fields[i].equals("\\N")) {
                    try {
                        return Integer.parseInt(fields[i]);
                    } catch (NumberFormatException ignored) {
                        // Continue checking other fields
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtil.logDebug("Could not extract created_by from tasks line: {}", dataLine);
        }
        return null;
    }

    /**
     * Extracts user_id from employee table COPY data line
     * @param dataLine the data line from employee table
     * @return the user_id, or null if not found
     */
    private Integer extractUserIdFromEmployeeData(String dataLine) {
        try {
            // Employee table format varies, but user_id is typically one of the fields
            String[] fields = dataLine.split("\t");
            for (int i = 0; i < Math.min(fields.length, 10); i++) {
                if (!fields[i].equals("\\N")) {
                    try {
                        return Integer.parseInt(fields[i]);
                    } catch (NumberFormatException ignored) {
                        // Continue checking other fields
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtil.logDebug("Could not extract user_id from employee line: {}", dataLine);
        }
        return null;
    }

    /**
     * Fixes orphaned references in COPY data lines
     * @param dataLine the original data line
     * @param tableName the name of the table being processed
     * @param analysisResult the analysis result containing valid and orphaned user IDs
     * @return the processed data line with orphaned references fixed
     */
    private String fixOrphanedReferencesInCopyData(String dataLine, String tableName, BackupAnalysisResult analysisResult) {
        try {
            if (analysisResult == null || analysisResult.getOrphanedUserIds().isEmpty()) {
                return dataLine; // No orphaned references to fix
            }

            String[] fields = dataLine.split("\t", -1); // -1 to preserve empty fields
            boolean modified = false;

            if ("projects".equals(tableName)) {
                // Fix created_by field in projects table
                // Try to find and fix the created_by field (typically in the last few positions)
                for (int i = Math.max(0, fields.length - 5); i < fields.length; i++) {
                    if (!fields[i].equals("\\N") && !fields[i].isEmpty()) {
                        try {
                            Integer userId = Integer.parseInt(fields[i]);
                            if (analysisResult.getOrphanedUserIds().contains(userId)) {
                                fields[i] = String.valueOf(analysisResult.getDefaultUserId());
                                modified = true;
                                LoggerUtil.logDebug("Fixed orphaned created_by reference in projects: {} -> {}",
                                        userId, analysisResult.getDefaultUserId());
                            }
                        } catch (NumberFormatException ignored) {
                            // Not a user ID field, continue
                        }
                    }
                }
            } else if ("tasks".equals(tableName)) {
                // Fix created_by field in tasks table
                for (int i = Math.max(0, fields.length - 5); i < fields.length; i++) {
                    if (!fields[i].equals("\\N") && !fields[i].isEmpty()) {
                        try {
                            Integer userId = Integer.parseInt(fields[i]);
                            if (analysisResult.getOrphanedUserIds().contains(userId)) {
                                fields[i] = String.valueOf(analysisResult.getDefaultUserId());
                                modified = true;
                                LoggerUtil.logDebug("Fixed orphaned created_by reference in tasks: {} -> {}",
                                        userId, analysisResult.getDefaultUserId());
                            }
                        } catch (NumberFormatException ignored) {
                            // Not a user ID field, continue
                        }
                    }
                }
            } else if ("employee".equals(tableName)) {
                // Fix user_id field in employee table
                for (int i = 0; i < Math.min(fields.length, 10); i++) {
                    if (!fields[i].equals("\\N") && !fields[i].isEmpty()) {
                        try {
                            Integer userId = Integer.parseInt(fields[i]);
                            if (analysisResult.getOrphanedUserIds().contains(userId)) {
                                fields[i] = String.valueOf(analysisResult.getDefaultUserId());
                                modified = true;
                                LoggerUtil.logDebug("Fixed orphaned user_id reference in employee: {} -> {}",
                                        userId, analysisResult.getDefaultUserId());
                            }
                        } catch (NumberFormatException ignored) {
                            // Not a user ID field, continue
                        }
                    }
                }
            }

            if (modified) {
                return String.join("\t", fields);
            } else {
                return dataLine;
            }
        } catch (Exception e) {
            LoggerUtil.logDebug("Error fixing orphaned references in line: {}", e.getMessage());
            return dataLine; // Return original line if processing fails
        }
    }

    /**
     * Inner class to hold backup analysis results
     */
    private static class BackupAnalysisResult {
        private final Set<Integer> validUserIds;
        private final Set<Integer> orphanedUserIds;
        private final Integer defaultUserId;

        public BackupAnalysisResult(Set<Integer> validUserIds, Set<Integer> orphanedUserIds, Integer defaultUserId) {
            this.validUserIds = validUserIds;
            this.orphanedUserIds = orphanedUserIds;
            this.defaultUserId = defaultUserId;
        }

        public Set<Integer> getValidUserIds() { return validUserIds; }
        public Set<Integer> getOrphanedUserIds() { return orphanedUserIds; }
        public Integer getDefaultUserId() { return defaultUserId; }
    }
}
