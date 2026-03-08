package com.codism.filestorage.domain.file.entity;

import ai.codism.common.base.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "file",
    indexes = {
        @Index(name = "idx_file_status", columnList = "status"),
        @Index(name = "idx_file_group_id", columnList = "group_id"),
        @Index(name = "idx_file_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class FileEntity extends BaseEntity {

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "stored_name", nullable = false, unique = true)
    private String storedName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(length = 10)
    private String extension;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FileStatus status = FileStatus.TEMP;

    @Column(name = "group_id", length = 50)
    private String groupId;

    @Column(name = "thumbnail_path")
    private String thumbnailPath;

    @Column(name = "image_width")
    private Integer imageWidth;

    @Column(name = "image_height")
    private Integer imageHeight;
}
