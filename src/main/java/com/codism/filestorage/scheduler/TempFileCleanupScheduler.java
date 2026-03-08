package com.codism.filestorage.scheduler;

import com.codism.filestorage.domain.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TempFileCleanupScheduler {

    private final FileService fileService;

    @Scheduled(cron = "${scheduler.temp-cleanup.cron}")
    public void cleanupExpiredTempFiles() {
        int deleted = fileService.cleanupExpiredTemp();
        if (deleted > 0) {
            log.info("만료된 임시 파일 {}건 정리 완료", deleted);
        }
    }
}
