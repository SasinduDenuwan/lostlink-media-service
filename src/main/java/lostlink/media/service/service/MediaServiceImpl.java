package lostlink.media.service.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import lostlink.media.service.dto.MediaUploadResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    @Autowired(required = false)
    private Storage storage;

    @Value("${findora.storage.type:local}")
    private String storageType;

    @Value("${findora.storage.local-dir:./uploads}")
    private String localDir;

    @Value("${findora.storage.local-url-prefix:http://localhost:8083/uploads/}")
    private String localUrlPrefix;

    @Value("${findora.storage.bucket-name:findora-media-bucket}")
    private String bucketName;

    @Override
    public MediaUploadResponse uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String objectName = UUID.randomUUID() + "-" + originalFileName;

        if ("gcs".equalsIgnoreCase(storageType)) {
            return uploadToGcs(file, originalFileName, "media/" + objectName);
        } else {
            return uploadToLocal(file, originalFileName, objectName);
        }
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

    private MediaUploadResponse uploadToLocal(MultipartFile file, String originalFileName, String objectName) {
        try {
            Path uploadPath = Paths.get(localDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path targetPath = uploadPath.resolve(objectName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = localUrlPrefix + objectName;

            return MediaUploadResponse.builder()
                    .fileName(originalFileName)
                    .objectName(objectName)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .url(fileUrl)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Failed to save file locally", e);
        }
    }

    private MediaUploadResponse uploadToGcs(MultipartFile file, String originalFileName, String objectName) {
        try {
            if (storage == null) {
                throw new IllegalStateException("GCS Storage bean is not initialized");
            }

            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType())
                    .build();

            storage.create(blobInfo, file.getBytes());

            String url = "https://storage.googleapis.com/" + bucketName + "/" + objectName;

            return MediaUploadResponse.builder()
                    .fileName(originalFileName)
                    .objectName(objectName)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .url(url)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to Google Cloud Storage", e);
        }
    }

    @Override
    public void deleteFile(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            throw new IllegalArgumentException("Object name cannot be empty");
        }

        if ("gcs".equalsIgnoreCase(storageType)) {
            if (storage == null) {
                throw new IllegalStateException("GCS Storage bean is not initialized");
            }
            boolean deleted = storage.delete(BlobId.of(bucketName, objectName));
            if (!deleted) {
                throw new IllegalArgumentException("File not found in GCS: " + objectName);
            }
        } else {
            try {
                Path filePath = Paths.get(localDir).resolve(objectName);
                boolean deleted = Files.deleteIfExists(filePath);
                if (!deleted) {
                    throw new IllegalArgumentException("File not found locally: " + objectName);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete local file", e);
            }
        }
    }
}