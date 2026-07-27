package com.englishcenter.academic.storage;

public record StoredFile(
        String storedFileName,
        String storageKey,
        String contentType,
        long fileSize,
        String originalFileName) {}
