package lostlink.media.service.service;

import lostlink.media.service.dto.MediaUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MediaService {

    MediaUploadResponse uploadFile(MultipartFile file);

    List<MediaUploadResponse> uploadMultipleFiles(List<MultipartFile> files);

    void deleteFile(String objectName);
}