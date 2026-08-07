package com.dthxhieu.ticket_booking_system_be.venue.service.impl;

import com.dthxhieu.ticket_booking_system_be.common.exception.BusinessException;
import com.dthxhieu.ticket_booking_system_be.common.exception.ResourceNotFoundException;
import com.dthxhieu.ticket_booking_system_be.entity.venue.Venue;
import com.dthxhieu.ticket_booking_system_be.repository.event.EventSessionRepository;
import com.dthxhieu.ticket_booking_system_be.repository.venue.SeatRepository;
import com.dthxhieu.ticket_booking_system_be.repository.venue.VenueRepository;
import com.dthxhieu.ticket_booking_system_be.venue.dto.request.CreateVenueRequest;
import com.dthxhieu.ticket_booking_system_be.venue.dto.request.UpdateVenueRequest;
import com.dthxhieu.ticket_booking_system_be.venue.dto.response.VenueResponse;
import com.dthxhieu.ticket_booking_system_be.venue.mapper.VenueMapper;
import com.dthxhieu.ticket_booking_system_be.venue.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;
    private final EventSessionRepository eventSessionRepository;
    private final VenueMapper venueMapper;

    @Override
    @Transactional(readOnly = true)
    public List<VenueResponse> getAllVenues() {
        return venueRepository.findAll()
                .stream()
                .map(venueMapper::toVenueResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VenueResponse getVenueById(Long id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found."));

        return venueMapper.toVenueResponse(venue);
    }

    @Override
    public VenueResponse createVenue(CreateVenueRequest request) {

        // BR-02: Name must be unique (case-insensitive). Name is trimmed before
        // the check so " CGV Vincom " and "CGV Vincom" are treated as identical.
        if (venueRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new BusinessException("Venue name already exists.");
        }

        Venue venue = Venue.builder()
                .name(request.getName().trim())
                .address(request.getAddress().trim())
                .capacity(request.getCapacity())
                .build();

        Venue saved = venueRepository.save(venue);

        return venueMapper.toVenueResponse(saved);
    }

    @Override
    public VenueResponse updateVenue(Long id, UpdateVenueRequest request) {

        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found."));

        // BR-02: Check name uniqueness, excluding the current record.
        if (venueRepository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
            throw new BusinessException("Venue name already exists.");
        }

        // BR-05: New capacity cannot be less than the current number of existing seats.
        // Reducing capacity below existing seats would create an inconsistent state
        // where the venue declares fewer seats than it physically has.
        long currentSeatCount = seatRepository.countByVenueId(id);
        if (request.getCapacity() < currentSeatCount) {
            throw new BusinessException(
                    "Capacity cannot be less than the current number of seats (" + currentSeatCount + ")."
            );
        }

        venue.setName(request.getName().trim());
        venue.setAddress(request.getAddress().trim());
        venue.setCapacity(request.getCapacity());

        Venue saved = venueRepository.save(venue);

        return venueMapper.toVenueResponse(saved);
    }

    @Override
    public void deleteVenue(Long id) {

        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found."));

        // BR-06: Check event sessions first.
        // Deleting a venue that is referenced by event sessions would leave those sessions
        // without a valid location. Check this before seats because session linkage is
        // the higher-risk constraint — upcoming events could be affected.
        if (eventSessionRepository.existsByVenueId(id)) {
            throw new BusinessException("Venue cannot be deleted because it is referenced by one or more event sessions.");
        }

        // BR-07: Check seats after event sessions.
        // If seats exist, they belong to this venue and must be removed first.
        // This prevents orphaned seat records.
        if (seatRepository.countByVenueId(id) > 0) {
            throw new BusinessException("Venue cannot be deleted because it still has seats.");
        }

        venueRepository.delete(venue);
    }
}
