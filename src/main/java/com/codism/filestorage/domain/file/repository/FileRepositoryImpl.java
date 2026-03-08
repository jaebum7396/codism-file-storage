package com.codism.filestorage.domain.file.repository;

import ai.codism.common.base.repository.BaseRepositoryImpl;
import com.codism.filestorage.domain.file.dto.FileSearchCondition;
import com.codism.filestorage.domain.file.entity.FileEntity;
import com.codism.filestorage.domain.file.entity.QFileEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.EntityPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public class FileRepositoryImpl extends BaseRepositoryImpl<FileEntity, FileSearchCondition>
        implements FileRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityPath<FileEntity> entityPath = QFileEntity.fileEntity;

    @Override
    protected BooleanBuilder buildWhereClause(FileSearchCondition condition) {
        QFileEntity file = QFileEntity.fileEntity;
        return buildWhere(
            eq(file.status, condition.getStatus()),
            eq(file.groupId, condition.getGroupId()),
            eq(file.extension, condition.getExtension())
        );
    }
}
