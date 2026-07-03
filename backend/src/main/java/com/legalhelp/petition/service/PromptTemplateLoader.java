package com.legalhelp.petition.service;

import com.legalhelp.common.exception.BadRequestException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PromptTemplateLoader {

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String load(String templateKey) {
        return cache.computeIfAbsent(templateKey, this::readFromClasspath);
    }

    private String readFromClasspath(String templateKey) {
        ClassPathResource resource = new ClassPathResource("petition-templates/" + templateKey + ".txt");
        if (!resource.exists()) {
            throw new BadRequestException("No prompt template configured for key: " + templateKey);
        }
        try {
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read prompt template: " + templateKey, e);
        }
    }
}
