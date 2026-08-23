package lostlink.media.service.service;

import lostlink.media.service.dto.MediaUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface MediaService {
    MediaUploadResponse uploadFile(MultipartFile file);

    void deleteFile(String objectName);
}
