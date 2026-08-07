package com.dthxhieu.ticket_booking_system_be.event.mapper;

import com.dthxhieu.ticket_booking_system_be.entity.event.Category;
import com.dthxhieu.ticket_booking_system_be.event.dto.response.CategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    // Maps the Category entity to the CategoryResponse DTO.
    // The mapper is a @Component (not a static utility class) so it follows
    // the project pattern established by AuthMapper and can be injected
    // wherever needed without coupling callers to static state.
    public CategoryResponse toCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}
