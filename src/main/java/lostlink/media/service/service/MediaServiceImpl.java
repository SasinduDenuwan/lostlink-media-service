package lostlink.media.service.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import lostlink.media.service.dto.MediaUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    // Using final for standard Spring Boot constructor injection via Lombok
    private final Storage storage;

    @Value("${lostlink.storage.bucket-name:lostlink-media-bucket-974ch}")
    private String bucketName;

    @Override
    public MediaUploadResponse uploadFile(MultipartFile file) {
        validateImageFile(file);

        String originalFileName = file.getOriginalFilename();
        String extension = getFileExtension(originalFileName);
        String objectName = UUID.randomUUID() + extension;

        return uploadToGcs(file, originalFileName, objectName);
    }

    @Override
    public List<MediaUploadResponse> uploadMultipleFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Files list cannot be empty");
        }
        return files.stream()
                .map(this::uploadFile)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteFile(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            throw new IllegalArgumentException("Object name cannot be empty");
        }

        if (storage == null) {
            throw new IllegalStateException("GCS Storage bean is not initialized");
        }

        boolean deleted = storage.delete(BlobId.of(bucketName, objectName));
        if (!deleted) {
            throw new IllegalArgumentException("File not found in GCS: " + objectName);
        }
    }

    private MediaUploadResponse uploadToGcs(MultipartFile file, String originalFileName, String objectName) {
        if (storage == null) {
            throw new IllegalStateException("GCS Storage bean is not initialized");
        }

        try {
            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType())
                    .build();

            storage.createFrom(blobInfo, file.getInputStream());

            String url = String.format("https://storage.googleapis.com/%s/%s", bucketName, objectName);

            return MediaUploadResponse.builder()
                    .fileName(originalFileName)
                    .objectName(objectName)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .url(url)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to Google Cloud Storage: " + e.getMessage(), e);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf("."));
        }
        return "";
    }
}