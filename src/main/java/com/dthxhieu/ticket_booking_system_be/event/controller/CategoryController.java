package com.dthxhieu.ticket_booking_system_be.event.controller;

import com.dthxhieu.ticket_booking_system_be.common.response.ApiResponse;
import com.dthxhieu.ticket_booking_system_be.event.dto.request.CreateCategoryRequest;
import com.dthxhieu.ticket_booking_system_be.event.dto.request.UpdateCategoryRequest;
import com.dthxhieu.ticket_booking_system_be.event.dto.response.CategoryResponse;
import com.dthxhieu.ticket_booking_system_be.event.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Two URL prefixes are used intentionally:
//   /api/v1/categories       — public read endpoints (GET all, GET by id)
//   /api/v1/admin/categories — admin write endpoints (POST, PUT, DELETE)
// This separation makes it trivial to protect admin paths in SecurityConfig
// with a single .requestMatchers("/api/v1/admin/**").hasRole("ADMIN") rule
// without needing method-level @PreAuthorize annotations.
@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // --- Public Endpoints ---

    @GetMapping("/api/v1/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> data = categoryService.getAllCategories();

        return ResponseEntity.ok(ApiResponse.<List<CategoryResponse>>builder()
                .success(true)
                .message("Categories retrieved successfully.")
                .data(data)
                .build());
    }

    @GetMapping("/api/v1/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(
            @PathVariable Long id
    ) {
        CategoryResponse data = categoryService.getCategoryById(id);

        return ResponseEntity.ok(ApiResponse.<CategoryResponse>builder()
                .success(true)
                .message("Category retrieved successfully.")
                .data(data)
                .build());
    }

    // --- Admin Endpoints ---

    @PostMapping("/api/v1/admin/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        CategoryResponse data = categoryService.createCategory(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<CategoryResponse>builder()
                        .success(true)
                        .message("Category created successfully.")
                        .data(data)
                        .build());
    }

    @PutMapping("/api/v1/admin/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        CategoryResponse data = categoryService.updateCategory(id, request);

        return ResponseEntity.ok(ApiResponse.<CategoryResponse>builder()
                .success(true)
                .message("Category updated successfully.")
                .data(data)
                .build());
    }

    @DeleteMapping("/api/v1/admin/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable Long id
    ) {
        categoryService.deleteCategory(id);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Category deleted successfully.")
                .build());
    }
}
