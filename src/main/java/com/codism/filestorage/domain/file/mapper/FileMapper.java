package com.codism.filestorage.domain.file.mapper;

import ai.codism.common.base.mapper.BaseMapper;
import ai.codism.common.base.mapper.MapperConfiguration;
import com.codism.filestorage.domain.file.dto.FileDto;
import com.codism.filestorage.domain.file.entity.FileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfiguration.class)
public interface FileMapper extends BaseMapper<FileEntity, FileDto> {

    @Override
    @Mapping(target = "filePath", ignore = true)
    @Mapping(target = "thumbnailPath", ignore = true)
    FileEntity toEntity(FileDto dto);
}
