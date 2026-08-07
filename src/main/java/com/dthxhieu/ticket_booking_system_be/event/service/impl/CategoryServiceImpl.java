package com.dthxhieu.ticket_booking_system_be.event.service.impl;

import com.dthxhieu.ticket_booking_system_be.common.exception.BusinessException;
import com.dthxhieu.ticket_booking_system_be.common.exception.ResourceNotFoundException;
import com.dthxhieu.ticket_booking_system_be.entity.event.Category;
import com.dthxhieu.ticket_booking_system_be.event.dto.request.CreateCategoryRequest;
import com.dthxhieu.ticket_booking_system_be.event.dto.request.UpdateCategoryRequest;
import com.dthxhieu.ticket_booking_system_be.event.dto.response.CategoryResponse;
import com.dthxhieu.ticket_booking_system_be.event.mapper.CategoryMapper;
import com.dthxhieu.ticket_booking_system_be.event.service.CategoryService;
import com.dthxhieu.ticket_booking_system_be.repository.event.CategoryRepository;
import com.dthxhieu.ticket_booking_system_be.repository.event.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        // findAll() returns all rows; no pagination is needed per spec section 2.
        // The stream + map pattern keeps the controller free of mapping logic.
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        return categoryMapper.toCategoryResponse(category);
    }

    @Override
    public CategoryResponse createCategory(CreateCategoryRequest request) {

        // BR-01: Name uniqueness is case-insensitive.
        // existsByNameIgnoreCase() performs the check efficiently with a DB query
        // rather than fetching all records and comparing in memory.
        if (categoryRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new BusinessException("Category name already exists.");
        }

        // Name is trimmed to strip accidental leading/trailing spaces before storage.
        // BR-01: Surrounding spaces are ignored for uniqueness - storing trimmed value
        // ensures the DB UNIQUE constraint and application logic stay consistent.
        Category category = Category.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .build();

        Category saved = categoryRepository.save(category);

        return categoryMapper.toCategoryResponse(saved);
    }

    @Override
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        // BR-05: Updating must not create a duplicate name conflict with ANOTHER category.
        // existsByNameIgnoreCaseAndIdNot() excludes the current record from the uniqueness check,
        // so renaming a category to its own trimmed name does not trigger a false positive.
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
            throw new BusinessException("Category name already exists.");
        }

        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());

        Category saved = categoryRepository.save(category);

        return categoryMapper.toCategoryResponse(saved);
    }

    @Override
    public void deleteCategory(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        // BR-04: A category in use by at least one event must not be deleted.
        // This check prevents orphaned events (events with a null or dangling category FK).
        // The check is done BEFORE the delete so the error is returned before any DB mutation.
        if (eventRepository.existsByCategoryId(id)) {
            throw new BusinessException("Category cannot be deleted because it is used by one or more events.");
        }

        categoryRepository.delete(category);
    }
}
