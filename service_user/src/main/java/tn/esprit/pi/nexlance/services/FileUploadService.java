package tn.esprit.pi.nexlance.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;
    
    @Value("${backend.url:http://localhost:8080}")
    private String backendUrl;

    /**
     * Upload a KYC document
     * @param file The file to upload
     * @param userId The user ID
     * @param documentType The document type
     * @return The relative path to the uploaded file
     */
    public String uploadKYCDocument(MultipartFile file, UUID userId, String documentType) {
        validateFile(file);
        
        try {
            // Create directory structure: uploads/kyc/userId/YYYY-MM/
            String datePath = LocalDate.now().toString().substring(0, 7); // YYYY-MM
            Path uploadPath = Paths.get(uploadDir, "kyc", userId.toString(), datePath);
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String filename = documentType + "_" + UUID.randomUUID() + extension;
            
            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Return full URL
            String fileUrl = backendUrl + "/uploads/kyc/" + userId + "/" + datePath + "/" + filename;
            log.info("File uploaded successfully: {}", fileUrl);
            
            return fileUrl;
            
        } catch (IOException e) {
            log.error("Failed to upload file", e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Upload an avatar image
     * @param file The file to upload
     * @param userId The user ID
     * @return The relative path to the uploaded file
     */
    public String uploadAvatar(MultipartFile file, UUID userId) {
        log.info("uploadAvatar called - userId: {}, uploadDir: {}", userId, uploadDir);
        
        validateImageFile(file);
        log.info("File validation passed");
        
        try {
            Path uploadPath = Paths.get(uploadDir, "avatars");
            log.info("Upload path: {}", uploadPath.toAbsolutePath());
            
            if (!Files.exists(uploadPath)) {
                log.info("Creating directories: {}", uploadPath);
                Files.createDirectories(uploadPath);
            }
            
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String filename = userId + extension;
            
            Path filePath = uploadPath.resolve(filename);
            log.info("Saving file to: {}", filePath.toAbsolutePath());
            
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Return full URL so frontend can access it
            String avatarUrl = backendUrl + "/uploads/avatars/" + filename;
            log.info("Avatar uploaded successfully: {}", avatarUrl);
            
            return avatarUrl;
            
        } catch (IOException e) {
            log.error("Failed to upload avatar - IOException", e);
            throw new RuntimeException("Failed to upload avatar: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to upload avatar - Unexpected exception", e);
            throw new RuntimeException("Failed to upload avatar: " + e.getMessage());
        }
    }

    /**
     * Delete a file
     * @param fileUrl The file URL (full or relative path)
     */
    public void deleteFile(String fileUrl) {
        try {
            if (fileUrl != null) {
                // Handle both full URL and relative path
                String relativePath = fileUrl;
                if (fileUrl.startsWith(backendUrl)) {
                    relativePath = fileUrl.substring(backendUrl.length());
                }
                
                if (relativePath.startsWith("/uploads/")) {
                    Path filePath = Paths.get(uploadDir + relativePath.substring(8));
                    Files.deleteIfExists(filePath);
                    log.info("File deleted successfully: {}", relativePath);
                }
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", fileUrl, e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
            throw new IllegalArgumentException("File must be an image or PDF");
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        if (file.getSize() > 50 * 1024 * 1024) { // 50MB
            throw new IllegalArgumentException("File size exceeds maximum limit of 50MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
