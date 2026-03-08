package com.codism.filestorage.domain.file.service;

import com.codism.filestorage.domain.file.entity.FileEntity;
import com.codism.filestorage.domain.file.entity.FileStatus;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Component
public class FileStorageService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Value("${file.storage.base-path}")
    private String basePath;

    @Value("${file.storage.temp-path}")
    private String tempPath;

    @Value("${file.storage.max-thumbnail-width:300}")
    private int maxThumbnailWidth;

    @Value("${file.storage.max-thumbnail-height:300}")
    private int maxThumbnailHeight;

    public FileEntity storeTemp(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);
        String storedName = UUID.randomUUID() + "." + extension;

        Path tempDir = Path.of(tempPath);
        Files.createDirectories(tempDir);
        Path targetPath = tempDir.resolve(storedName);
        file.transferTo(targetPath.toFile());

        FileEntity entity = FileEntity.builder()
                .originalName(originalName)
                .storedName(storedName)
                .filePath(targetPath.toString())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .extension(extension)
                .status(FileStatus.TEMP)
                .build();

        if (isImage(extension)) {
            setImageDimensions(entity, targetPath);
            generateThumbnail(entity, targetPath);
        }

        return entity;
    }

    public void confirmFile(FileEntity entity) throws IOException {
        if (entity.getStatus() != FileStatus.TEMP) {
            return;
        }

        String datePath = LocalDate.now().format(DATE_FORMAT);
        Path confirmDir = Path.of(basePath, datePath);
        Files.createDirectories(confirmDir);

        Path source = Path.of(entity.getFilePath());
        Path target = confirmDir.resolve(entity.getStoredName());
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        entity.setFilePath(target.toString());

        if (entity.getThumbnailPath() != null) {
            Path thumbSource = Path.of(entity.getThumbnailPath());
            if (Files.exists(thumbSource)) {
                Path thumbDir = Path.of(basePath, "thumbnails", datePath);
                Files.createDirectories(thumbDir);
                Path thumbTarget = thumbDir.resolve("thumb_" + entity.getStoredName());
                Files.move(thumbSource, thumbTarget, StandardCopyOption.REPLACE_EXISTING);
                entity.setThumbnailPath(thumbTarget.toString());
            }
        }

        entity.setStatus(FileStatus.CONFIRMED);
    }

    public void deleteFile(FileEntity entity) {
        try {
            Path filePath = Path.of(entity.getFilePath());
            Files.deleteIfExists(filePath);

            if (entity.getThumbnailPath() != null) {
                Files.deleteIfExists(Path.of(entity.getThumbnailPath()));
            }
        } catch (IOException e) {
            // 파일 삭제 실패는 로그만 남기고 진행
        }
    }

    public Path getFilePath(FileEntity entity) {
        return Path.of(entity.getFilePath());
    }

    public boolean isImage(String extension) {
        return extension != null && IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private void setImageDimensions(FileEntity entity, Path imagePath) {
        try (InputStream is = Files.newInputStream(imagePath)) {
            BufferedImage image = ImageIO.read(is);
            if (image != null) {
                entity.setImageWidth(image.getWidth());
                entity.setImageHeight(image.getHeight());
            }
        } catch (IOException ignored) {
        }
    }

    private void generateThumbnail(FileEntity entity, Path imagePath) {
        try {
            String extension = entity.getExtension();
            if ("gif".equalsIgnoreCase(extension) || "webp".equalsIgnoreCase(extension)) {
                return;
            }

            Path thumbDir = Path.of(tempPath, "thumbnails");
            Files.createDirectories(thumbDir);
            Path thumbPath = thumbDir.resolve("thumb_" + entity.getStoredName());

            Thumbnails.of(imagePath.toFile())
                    .size(maxThumbnailWidth, maxThumbnailHeight)
                    .keepAspectRatio(true)
                    .toFile(thumbPath.toFile());

            entity.setThumbnailPath(thumbPath.toString());
        } catch (IOException ignored) {
        }
    }
}
