package com.codism.filestorage.domain.file.dto;

import ai.codism.common.base.dto.BaseSearchCondition;
import com.codism.filestorage.domain.file.entity.FileStatus;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FileSearchCondition extends BaseSearchCondition {

    private FileStatus status;
    private String groupId;
    private String extension;
}
