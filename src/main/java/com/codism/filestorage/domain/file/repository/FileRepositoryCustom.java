package com.codism.filestorage.domain.file.repository;

import ai.codism.common.base.repository.BaseRepositoryCustom;
import com.codism.filestorage.domain.file.dto.FileDto;
import com.codism.filestorage.domain.file.entity.FileEntity;

public interface FileRepositoryCustom extends BaseRepositoryCustom<FileEntity, FileDto> {
}
