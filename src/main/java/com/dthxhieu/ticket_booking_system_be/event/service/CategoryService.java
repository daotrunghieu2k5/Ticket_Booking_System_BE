package com.dthxhieu.ticket_booking_system_be.event.service;

import com.dthxhieu.ticket_booking_system_be.event.dto.request.CreateCategoryRequest;
import com.dthxhieu.ticket_booking_system_be.event.dto.request.UpdateCategoryRequest;
import com.dthxhieu.ticket_booking_system_be.event.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    // Returns all categories - available to public users (FR-01).
    List<CategoryResponse> getAllCategories();

    // Returns one category by id - available to public users (FR-02).
    CategoryResponse getCategoryById(Long id);

    // Creates a new category - admin only (FR-03, BR-06).
    CategoryResponse createCategory(CreateCategoryRequest request);

    // Updates an existing category - admin only (FR-04, BR-06).
    CategoryResponse updateCategory(Long id, UpdateCategoryRequest request);

    // Deletes a category if it is not used by any event - admin only (FR-05, BR-04, BR-06).
    void deleteCategory(Long id);
}
