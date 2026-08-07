package com.dthxhieu.ticket_booking_system_be.event.dto.request;

import com.dthxhieu.ticket_booking_system_be.common.enums.EventStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventRequest {

    // BR-01, BR-02, BR-03: required, unique (case-insensitive), trimmed in service.
    @NotBlank(message = "Event title is required")
    @Size(max = 255, message = "Event title must not exceed 255 characters")
    private String title;

    // BR-06: required description.
    @NotBlank(message = "Event description is required")
    @Size(max = 5000, message = "Event description must not exceed 5000 characters")
    private String description;

    // BR-04: must reference an existing category.
    @NotNull(message = "Category ID is required")
    private Long categoryId;

    // BR-07: required poster URL.
    @NotBlank(message = "Poster URL is required")
    @Size(max = 500, message = "Poster URL must not exceed 500 characters")
    @URL(message = "Poster must be a valid URL")
    private String posterUrl;

    // BR-08: optional banner URL.
    @Size(max = 500, message = "Banner URL must not exceed 500 characters")
    @URL(message = "Banner must be a valid URL")
    private String bannerUrl;

    // BR-09: saleStartTime < saleEndTime validated in service.
    @NotNull(message = "Sale start time is required")
    private LocalDateTime saleStartTime;

    @NotNull(message = "Sale end time is required")
    private LocalDateTime saleEndTime;

    // Optional — defaults to ACTIVE in service if not provided (§7 spec).
    private EventStatus status;
}
