package com.dthxhieu.ticket_booking_system_be.event.service.impl;

import com.dthxhieu.ticket_booking_system_be.common.enums.EventStatus;
import com.dthxhieu.ticket_booking_system_be.common.exception.BusinessException;
import com.dthxhieu.ticket_booking_system_be.common.exception.ResourceNotFoundException;
import com.dthxhieu.ticket_booking_system_be.entity.event.Category;
import com.dthxhieu.ticket_booking_system_be.entity.event.Event;
import com.dthxhieu.ticket_booking_system_be.event.dto.request.CreateEventRequest;
import com.dthxhieu.ticket_booking_system_be.event.dto.request.UpdateEventRequest;
import com.dthxhieu.ticket_booking_system_be.event.dto.response.EventDetailResponse;
import com.dthxhieu.ticket_booking_system_be.event.dto.response.EventPageResponse;
import com.dthxhieu.ticket_booking_system_be.event.dto.response.EventSummaryResponse;
import com.dthxhieu.ticket_booking_system_be.event.mapper.EventMapper;
import com.dthxhieu.ticket_booking_system_be.event.service.EventService;
import com.dthxhieu.ticket_booking_system_be.repository.event.CategoryRepository;
import com.dthxhieu.ticket_booking_system_be.repository.event.EventRepository;
import com.dthxhieu.ticket_booking_system_be.repository.event.EventSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventSessionRepository eventSessionRepository;
    private final EventMapper eventMapper;

    @Override
    @Transactional(readOnly = true)
    public EventPageResponse getEvents(
            String keyword,
            Long categoryId,
            EventStatus status,
            Pageable pageable
    ) {
        // BR-11: Public callers pass status = ACTIVE. Admin callers may pass null (all) or a specific status.
        // The controller is responsible for enforcing this rule by always passing ACTIVE for public endpoints.
        Page<Event> page = eventRepository.findByFilters(keyword, categoryId, status, pageable);

        List<EventSummaryResponse> content = page.getContent()
                .stream()
                .map(eventMapper::toSummary)
                .toList();

        return EventPageResponse.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EventDetailResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found."));

        return eventMapper.toDetail(event);
    }

    @Override
    public EventDetailResponse createEvent(CreateEventRequest request) {
        // BR-03: Trim title before all checks and persistence.
        String title = request.getTitle().trim();

        // BR-02: Title must be unique (case-insensitive).
        if (eventRepository.existsByTitleIgnoreCase(title)) {
            throw new BusinessException("Event title already exists.");
        }

        // BR-04: Category must exist.
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        // BR-05: Category should be ACTIVE.
        // Note: The current Category entity does not have a status field.
        // This validation will be enforced when Category gains a status column in a future US.
        // if (!category.isActive()) { throw new BusinessException("Category is not active."); }

        // BR-09: saleStartTime must be before saleEndTime.
        validateSaleTime(request.getSaleStartTime(), request.getSaleEndTime());

        Event event = Event.builder()
                .title(title)
                .description(request.getDescription())
                .posterUrl(request.getPosterUrl())
                .bannerUrl(request.getBannerUrl())
                .saleStartTime(request.getSaleStartTime())
                .saleEndTime(request.getSaleEndTime())
                // Default duration and ageLimit — these fields are required NOT NULL in DB (V9).
                // US-10 does not expose them in the API; default values are used until
                // an Event Management extension adds them.
                .duration(0)
                .ageLimit(0)
                // BR-10: Default status to ACTIVE if not provided (§7 spec).
                .status(request.getStatus() != null ? request.getStatus() : EventStatus.ACTIVE)
                .category(category)
                .build();

        return eventMapper.toDetail(eventRepository.save(event));
    }

    @Override
    public EventDetailResponse updateEvent(Long id, UpdateEventRequest request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found."));

        String title = request.getTitle().trim();

        // BR-02: Check uniqueness excluding the current event.
        if (eventRepository.existsByTitleIgnoreCaseAndIdNot(title, id)) {
            throw new BusinessException("Event title already exists.");
        }

        // BR-04: Category must exist.
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        // BR-05 placeholder: see createEvent comment.

        // BR-09: Validate sale time range.
        validateSaleTime(request.getSaleStartTime(), request.getSaleEndTime());

        // BR-15: Update only event fields — event sessions, seats, bookings untouched.
        event.setTitle(title);
        event.setDescription(request.getDescription());
        event.setPosterUrl(request.getPosterUrl());
        event.setBannerUrl(request.getBannerUrl());
        event.setSaleStartTime(request.getSaleStartTime());
        event.setSaleEndTime(request.getSaleEndTime());
        event.setStatus(request.getStatus());
        event.setCategory(category);

        return eventMapper.toDetail(eventRepository.save(event));
    }

    @Override
    public void deleteEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found."));

        // BR-13: If the event has event sessions, it must NOT be physically deleted.
        // Reason: deleting would break referential integrity for existing bookings.
        // Instead, soft-delete by marking status INACTIVE.
        if (eventSessionRepository.existsByEventId(id)) {
            event.setStatus(EventStatus.INACTIVE);
            eventRepository.save(event);
            return;
        }

        // BR-14: No event sessions — safe to physically delete.
        eventRepository.delete(event);
    }

    @Override
    public void activateEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found."));

        event.setStatus(EventStatus.ACTIVE);
        eventRepository.save(event);
    }

    @Override
    public void deactivateEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found."));

        event.setStatus(EventStatus.INACTIVE);
        eventRepository.save(event);
    }

    // --- Private Helpers ---

    // BR-09: saleStartTime must be strictly before saleEndTime.
    private void validateSaleTime(
            java.time.LocalDateTime saleStartTime,
            java.time.LocalDateTime saleEndTime
    ) {
        if (!saleStartTime.isBefore(saleEndTime)) {
            throw new BusinessException("Sale start time must be before sale end time.");
        }
    }
}
