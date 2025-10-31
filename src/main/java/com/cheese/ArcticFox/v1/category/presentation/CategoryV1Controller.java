package com.cheese.ArcticFox.v1.category.presentation;

import com.cheese.ArcticFox.v1.category.application.dto.request.CreateCategoryRequest;
import com.cheese.ArcticFox.v1.category.application.dto.response.CreateCategoryResponse;
import com.cheese.ArcticFox.v1.category.application.service.CategoryV1Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
