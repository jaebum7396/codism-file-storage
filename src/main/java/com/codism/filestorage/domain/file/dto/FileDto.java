package com.codism.filestorage.domain.file.dto;

import ai.codism.common.base.dto.BaseDto;
import com.codism.filestorage.domain.file.entity.FileStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileDto extends BaseDto {

    private String originalName;
    private String storedName;
    private String contentType;
    private Long fileSize;
    private String extension;
    private FileStatus status;
    private String groupId;
    private String url;
    private String thumbnailUrl;
    private Integer imageWidth;
    private Integer imageHeight;
}
