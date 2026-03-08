package com.codism.filestorage.domain.file.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class FileConfirmRequest {

    @NotEmpty(message = "파일 ID 목록은 필수입니다.")
    private List<Long> fileIds;

    private String groupId;
}
