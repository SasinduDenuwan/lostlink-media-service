package lostlink.media.service.controller;

import lombok.RequiredArgsConstructor;
import lostlink.media.service.dto.MediaUploadResponse;
import lostlink.media.service.service.MediaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    // POST /api/v1/media/upload (Single file)
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<MediaUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file
    ) {
        MediaUploadResponse response = mediaService.uploadFile(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // POST /api/v1/media/upload-multiple (Multiple files support)
    @PostMapping(
            value = "/upload-multiple",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<List<MediaUploadResponse>> uploadMultipleFiles(
            @RequestParam("files") List<MultipartFile> files
    ) {
        List<MediaUploadResponse> responses = mediaService.uploadMultipleFiles(files);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    // DELETE /api/v1/media/{objectName}
    @DeleteMapping("/{objectName}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String objectName
    ) {
        mediaService.deleteFile(objectName);
        return ResponseEntity.noContent().build();
    }
}