package com.englishcenter.academic.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StoredFile store(MultipartFile file, String category);

    Resource loadAsResource(String storageKey);

    void delete(String storageKey);

    void validate(MultipartFile file);
}
