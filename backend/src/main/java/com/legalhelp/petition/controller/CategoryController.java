package com.legalhelp.petition.controller;

import com.legalhelp.petition.dto.CaseCategoryResponse;
import com.legalhelp.petition.service.CaseCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CaseCategoryService caseCategoryService;

    public CategoryController(CaseCategoryService caseCategoryService) {
        this.caseCategoryService = caseCategoryService;
    }

    @GetMapping
    public List<CaseCategoryResponse> listActive() {
        return caseCategoryService.listActive();
    }
}
