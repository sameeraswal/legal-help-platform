package com.legalhelp.petition.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Local-disk implementation of petition file storage. `file_url` on the Petition entity
 * is an opaque reference — swapping to S3-compatible storage means changing this class
 * and the download endpoint, not the schema or the rest of the pipeline.
 */
@Service
public class PetitionStorageService {

    private final Path baseDir;

    public PetitionStorageService(@Value("${app.storage.petition-dir}") String petitionDir) {
        this.baseDir = Path.of(petitionDir);
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create petition storage directory: " + baseDir, e);
        }
    }

    public String write(String relativeFileName, byte[] content) {
        Path target = baseDir.resolve(relativeFileName).normalize();
        if (!target.startsWith(baseDir)) {
            throw new IllegalArgumentException("Invalid file name");
        }
        try {
            Files.write(target, content);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write petition file: " + target, e);
        }
        return relativeFileName;
    }

    public byte[] read(String relativeFileName) {
        Path target = baseDir.resolve(relativeFileName).normalize();
        if (!target.startsWith(baseDir)) {
            throw new IllegalArgumentException("Invalid file name");
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read petition file: " + target, e);
        }
    }
}
