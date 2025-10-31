package com.cheese.ArcticFox.v1.category.presentation;

import com.cheese.ArcticFox.v1.category.application.dto.request.CreateCategoryRequest;
import com.cheese.ArcticFox.v1.category.application.dto.request.UpdateCategoryRequest;
import com.cheese.ArcticFox.v1.category.application.dto.response.CreateCategoryResponse;
import com.cheese.ArcticFox.v1.category.application.dto.response.GetCategoryResponse;
import com.cheese.ArcticFox.v1.category.application.dto.response.UpdateCategoryResponse;
import com.cheese.ArcticFox.v1.category.application.service.CategoryV1Service;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
@RestController
public class CategoryV1Controller {

    private final CategoryV1Service categoryV1Service;

    @PostMapping
    public ResponseEntity<CreateCategoryResponse> create(
            @RequestBody @Valid CreateCategoryRequest request
    ) {
        CreateCategoryResponse response = categoryV1Service.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<GetCategoryResponse> get(
            @RequestParam(name = "path", required = false) @Nullable String path
    ) {
        GetCategoryResponse response = categoryV1Service.get(path);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{categoryId}")
    public ResponseEntity<UpdateCategoryResponse> update(
            @PathVariable(name = "categoryId") Long categoryId,
            @RequestBody @Valid UpdateCategoryRequest request) {
        UpdateCategoryResponse response = categoryV1Service.update(categoryId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(
            @PathVariable(name = "categoryId") Long categoryId) {
        categoryV1Service.delete(categoryId);
        return ResponseEntity.noContent().build();
    }
}
