package com.codism.filestorage.domain.file.controller;

import com.codism.filestorage.domain.file.dto.FileConfirmRequest;
import com.codism.filestorage.domain.file.dto.FileDto;
import com.codism.filestorage.domain.file.entity.FileEntity;
import com.codism.filestorage.domain.file.service.FileService;
import com.codism.filestorage.domain.file.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FileStorageService storageService;

    @PostMapping("/temp")
    public ResponseEntity<FileDto> uploadTemp(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(fileService.uploadTemp(file));
    }

    @PostMapping("/temp/bulk")
    public ResponseEntity<List<FileDto>> uploadTempBulk(@RequestParam("files") List<MultipartFile> files) {
        List<FileDto> results = files.stream()
                .map(fileService::uploadTemp)
                .toList();
        return ResponseEntity.ok(results);
    }

    @PostMapping("/confirm")
    public ResponseEntity<List<FileDto>> confirm(@Valid @RequestBody FileConfirmRequest request) {
        return ResponseEntity.ok(fileService.confirm(request));
    }

    @GetMapping("/download/{storedName}")
    public ResponseEntity<Resource> download(
            @PathVariable String storedName,
            @RequestParam(value = "thumbnail", defaultValue = "false") boolean thumbnail) throws Exception {

        FileEntity entity = fileService.repository().findByStoredName(storedName)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다."));

        Path filePath;
        if (thumbnail && entity.getThumbnailPath() != null) {
            filePath = Path.of(entity.getThumbnailPath());
        } else {
            filePath = storageService.getFilePath(entity);
        }

        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) {
            throw new IllegalArgumentException("파일이 존재하지 않습니다.");
        }

        String encodedName = URLEncoder.encode(entity.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(entity.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename*=UTF-8''" + encodedName)
                .body(resource);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<FileDto>> getByGroupId(@PathVariable String groupId) {
        return ResponseEntity.ok(fileService.getFilesByGroupId(groupId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        fileService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }
}
