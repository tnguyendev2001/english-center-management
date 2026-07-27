package com.englishcenter.academic.storage;

import com.englishcenter.common.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "jpg", "jpeg", "png", "txt");

    private static final Map<String, String> EXTENSION_CONTENT_TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("txt", "text/plain"));

    private final FileStorageProperties properties;

    public LocalFileStorageService(FileStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public StoredFile store(MultipartFile file, String category) {
        validate(file);
        String sanitizedCategory = sanitizeCategory(category);
        String originalFileName = resolveOriginalFileName(file);
        String extension = extractExtension(originalFileName);
        String sanitizedBaseName = sanitizeFileName(stripExtension(originalFileName));
        String storedFileName = UUID.randomUUID() + "_" + sanitizedBaseName + "." + extension;
        String storageKey = sanitizedCategory + "/" + storedFileName;

        Path targetDirectory = resolveCategoryDirectory(sanitizedCategory);
        Path targetFile = targetDirectory.resolve(storedFileName).normalize();

        ensureWithinBaseDirectory(targetFile);

        try {
            Files.createDirectories(targetDirectory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BusinessException("Không thể lưu tệp học liệu.");
        }

        long fileSize = file.getSize();
        String contentType = resolveContentType(file, extension);

        return new StoredFile(storedFileName, storageKey, contentType, fileSize, originalFileName);
    }

    @Override
    public Resource loadAsResource(String storageKey) {
        Path filePath = resolveStoragePath(storageKey);
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new BusinessException("Không tìm thấy tệp học liệu.");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException("Không thể đọc tệp học liệu.");
            }
            return resource;
        } catch (IOException ex) {
            throw new BusinessException("Không thể đọc tệp học liệu.");
        }
    }

    @Override
    public void delete(String storageKey) {
        Path filePath = resolveStoragePath(storageKey);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new BusinessException("Không thể xóa tệp học liệu.");
        }
    }

    @Override
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Tệp tải lên không hợp lệ.");
        }

        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new BusinessException("Kích thước tệp vượt quá giới hạn cho phép.");
        }

        String originalFileName = resolveOriginalFileName(file);
        String extension = extractExtension(originalFileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("Loại tệp không được hỗ trợ.");
        }

        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType)) {
            String expectedContentType = EXTENSION_CONTENT_TYPES.get(extension);
            if (expectedContentType != null && !contentType.equalsIgnoreCase(expectedContentType)) {
                throw new BusinessException("Loại tệp không được hỗ trợ.");
            }
        }
    }

    private Path resolveStoragePath(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            throw new BusinessException("Khóa lưu trữ không hợp lệ.");
        }

        Path baseDirectory = Paths.get(properties.getBaseDir()).toAbsolutePath().normalize();
        Path filePath = baseDirectory.resolve(storageKey).normalize();
        ensureWithinBaseDirectory(filePath);
        return filePath;
    }

    private Path resolveCategoryDirectory(String category) {
        return Paths.get(properties.getBaseDir(), category).toAbsolutePath().normalize();
    }

    private void ensureWithinBaseDirectory(Path filePath) {
        Path baseDirectory = Paths.get(properties.getBaseDir()).toAbsolutePath().normalize();
        if (!filePath.startsWith(baseDirectory)) {
            throw new BusinessException("Khóa lưu trữ không hợp lệ.");
        }
    }

    private String resolveOriginalFileName(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFileName)) {
            throw new BusinessException("Tên tệp không hợp lệ.");
        }
        return Paths.get(originalFileName).getFileName().toString();
    }

    private String sanitizeCategory(String category) {
        if (!StringUtils.hasText(category)) {
            throw new BusinessException("Danh mục lưu trữ không hợp lệ.");
        }

        String sanitized = category.trim().replace('\\', '/');
        if (sanitized.contains("..") || sanitized.startsWith("/")) {
            throw new BusinessException("Danh mục lưu trữ không hợp lệ.");
        }

        sanitized = sanitized.replaceAll("[^a-zA-Z0-9/_-]", "_");
        if (!StringUtils.hasText(sanitized)) {
            throw new BusinessException("Danh mục lưu trữ không hợp lệ.");
        }
        return sanitized;
    }

    private String sanitizeFileName(String fileName) {
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!StringUtils.hasText(sanitized)) {
            return "file";
        }
        return sanitized;
    }

    private String extractExtension(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        if (!StringUtils.hasText(extension)) {
            throw new BusinessException("Loại tệp không được hỗ trợ.");
        }
        return extension.toLowerCase(Locale.ROOT);
    }

    private String stripExtension(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        if (!StringUtils.hasText(extension)) {
            return fileName;
        }
        return fileName.substring(0, fileName.length() - extension.length() - 1);
    }

    private String resolveContentType(MultipartFile file, String extension) {
        if (StringUtils.hasText(file.getContentType())) {
            return file.getContentType();
        }
        return EXTENSION_CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
    }
}
