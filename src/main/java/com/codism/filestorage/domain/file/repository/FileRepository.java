package com.codism.filestorage.domain.file.repository;

import ai.codism.common.base.repository.BaseRepository;
import com.codism.filestorage.domain.file.entity.FileEntity;
import com.codism.filestorage.domain.file.entity.FileStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends BaseRepository<FileEntity>, FileRepositoryCustom {

    Optional<FileEntity> findByStoredName(String storedName);

    List<FileEntity> findByGroupId(String groupId);

    List<FileEntity> findByStatusAndCreatedAtBefore(FileStatus status, LocalDateTime dateTime);

    List<FileEntity> findByIdInAndStatus(List<Long> ids, FileStatus status);
}
