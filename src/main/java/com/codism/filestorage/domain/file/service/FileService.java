package com.codism.filestorage.domain.file.service;

import ai.codism.common.base.service.BaseServiceImpl;
import com.codism.filestorage.domain.file.dto.FileConfirmRequest;
import com.codism.filestorage.domain.file.dto.FileDto;
import com.codism.filestorage.domain.file.entity.FileEntity;
import com.codism.filestorage.domain.file.entity.FileStatus;
import com.codism.filestorage.domain.file.mapper.FileMapper;
import com.codism.filestorage.domain.file.repository.FileRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService extends BaseServiceImpl<FileEntity, FileDto, FileRepository> {

    private final FileRepository repository;
    private final FileMapper mapper;
    private final FileStorageService storageService;

    @Value("${file.storage.base-url}")
    private String baseUrl;

    @Value("${file.storage.allowed-extensions}")
    private String allowedExtensions;

    @Value("${file.storage.temp-expiry-hours:24}")
    private int tempExpiryHours;

    @Transactional
    public FileDto uploadTemp(MultipartFile file) {
        validateFile(file);
        try {
            FileEntity entity = storageService.storeTemp(file);
            entity = repository.save(entity);
            return toFileDto(entity);
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드에 실패했습니다.", e);
        }
    }

    @Transactional
    public List<FileDto> confirm(FileConfirmRequest request) {
        List<FileEntity> files = repository.findByIdInAndStatus(request.getFileIds(), FileStatus.TEMP);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("확정할 파일이 없습니다.");
        }

        return files.stream().map(entity -> {
            try {
                storageService.confirmFile(entity);
                if (request.getGroupId() != null) {
                    entity.setGroupId(request.getGroupId());
                }
                return toFileDto(entity);
            } catch (IOException e) {
                throw new RuntimeException("파일 확정에 실패했습니다: " + entity.getOriginalName(), e);
            }
        }).collect(Collectors.toList());
    }

    @Transactional
    public void deleteFile(Long id) {
        FileEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다."));
        storageService.deleteFile(entity);
        entity.setStatus(FileStatus.DELETED);
        entity.setDelYn("Y");
    }

    public List<FileDto> getFilesByGroupId(String groupId) {
        return repository.findByGroupId(groupId).stream()
                .filter(e -> e.getStatus() == FileStatus.CONFIRMED)
                .map(this::toFileDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public int cleanupExpiredTemp() {
        LocalDateTime expiry = LocalDateTime.now().minusHours(tempExpiryHours);
        List<FileEntity> expiredFiles = repository.findByStatusAndCreatedAtBefore(FileStatus.TEMP, expiry);

        for (FileEntity entity : expiredFiles) {
            storageService.deleteFile(entity);
            entity.setStatus(FileStatus.DELETED);
            entity.setDelYn("Y");
        }

        return expiredFiles.size();
    }

    private FileDto toFileDto(FileEntity entity) {
        FileDto dto = mapper.toDto(entity);
        dto.setUrl(baseUrl + "/download/" + entity.getStoredName());
        if (entity.getThumbnailPath() != null) {
            dto.setThumbnailUrl(baseUrl + "/download/" + entity.getStoredName() + "?thumbnail=true");
        }
        return dto;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            throw new IllegalArgumentException("파일 확장자를 확인할 수 없습니다.");
        }

        String extension = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        Set<String> allowed = Arrays.stream(allowedExtensions.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        if (!allowed.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다: " + extension);
        }
    }
}
