package com.legalhelp.petition.controller;

import com.legalhelp.common.security.AuthPrincipal;
import com.legalhelp.petition.dto.CaseCategoryResponse;
import com.legalhelp.petition.dto.CaseCategoryUpsertRequest;
import com.legalhelp.petition.service.CaseCategoryService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
public class CategoryAdminController {

    private final CaseCategoryService caseCategoryService;

    public CategoryAdminController(CaseCategoryService caseCategoryService) {
        this.caseCategoryService = caseCategoryService;
    }

    @GetMapping
    public List<CaseCategoryResponse> listAll() {
        return caseCategoryService.listAll();
    }

    @PostMapping
    public CaseCategoryResponse create(@Valid @RequestBody CaseCategoryUpsertRequest request,
                                        @AuthenticationPrincipal AuthPrincipal actor) {
        return caseCategoryService.create(request, actor);
    }

    @PutMapping("/{id}")
    public CaseCategoryResponse update(@PathVariable Long id, @Valid @RequestBody CaseCategoryUpsertRequest request,
                                        @AuthenticationPrincipal AuthPrincipal actor) {
        return caseCategoryService.update(id, request, actor);
    }
}
