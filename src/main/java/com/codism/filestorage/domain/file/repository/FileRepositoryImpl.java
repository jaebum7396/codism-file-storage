package com.codism.filestorage.domain.file.repository;

import ai.codism.common.base.dto.BaseSearchCondition;
import ai.codism.common.base.repository.BaseRepositoryImpl;
import com.codism.filestorage.domain.file.dto.FileDto;
import com.codism.filestorage.domain.file.dto.FileSearchCondition;
import com.codism.filestorage.domain.file.entity.FileEntity;
import com.codism.filestorage.domain.file.entity.QFileEntity;
import com.codism.filestorage.domain.file.mapper.FileMapper;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.EntityPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public class FileRepositoryImpl extends BaseRepositoryImpl<FileEntity, FileDto, FileMapper>
        implements FileRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final FileMapper mapper;
    private final EntityPath<FileEntity> entityPath = QFileEntity.fileEntity;

    @Override
    protected BooleanBuilder buildWhereFromCondition(BaseSearchCondition condition) {
        BooleanBuilder builder = buildBaseConditionBuilder(condition, null);
        if (condition instanceof FileSearchCondition fc) {
            QFileEntity file = QFileEntity.fileEntity;
            if (fc.getStatus() != null) builder.and(file.status.eq(fc.getStatus()));
            if (fc.getGroupId() != null) builder.and(file.groupId.eq(fc.getGroupId()));
            if (fc.getExtension() != null) builder.and(file.extension.eq(fc.getExtension()));
        }
        return builder;
    }
}
