package com.app.godo.services.venue;

import com.app.godo.dtos.venue.CreateVenueRequestDto;
import com.app.godo.dtos.venue.UpdateVenueDto;
import com.app.godo.dtos.venue.VenueOverviewDto;
import com.app.godo.enums.VenueType;
import com.app.godo.exceptions.general.ConflictException;
import com.app.godo.exceptions.general.NotFoundException;
import com.app.godo.exceptions.general.ParseException;
import com.app.godo.models.Image;
import com.app.godo.models.Venue;
import com.app.godo.repositories.event.EventRepository;
import com.app.godo.repositories.venue.VenueRepository;
import com.app.godo.services.event.EventService;
import com.app.godo.services.elasticsearch.VenueElasticsearchSyncService;
import com.app.godo.services.files.MinIOService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueService {
    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final MinIOService minIOService;
    private final VenueElasticsearchSyncService syncService;
    private final ObjectMapper objectMapper;

    private static final Logger logger = LogManager.getLogger(VenueService.class);

    public Page<VenueOverviewDto> filterVenues(String filter, int venueType, Pageable pageable) {
        Page<Venue> venues;

        if (venueType == -1) {
            venues = venueRepository.filterVenues(filter, filter, pageable);
        }
        else {
            venues = venueRepository.filterVenuesWithType(filter, filter, VenueType.values()[venueType], pageable);
        }

        return venues.map(VenueOverviewDto::fromEntity);
    }

    @Transactional
    public VenueOverviewDto createVenue(CreateVenueRequestDto venueRequest, MultipartFile venueImage) {
        Venue existingVenue = venueRepository.findVenueByName(venueRequest.getName());

        if (existingVenue != null) { throw new ConflictException("A venue with the entered name already exists!"); }

        // Upload image to MinIO
        String imageFilename = minIOService.uploadFile(venueImage);
        String imagePath = minIOService.getFileUrl(imageFilename);

        Venue venue = Venue.builder()
                .name(venueRequest.getName())
                .description(venueRequest.getDescription())
                .address(venueRequest.getAddress())
                .type(venueRequest.getType())
                .averageRating(0)
                .createdAt(LocalDate.from(LocalDateTime.now()))
                .build();

        venue.setImage(
                Image.builder()
                .venueImageOf(venue)
                .path(imagePath).build()
        );

        Venue savedVenue = venueRepository.save(venue);

        // Index in Elasticsearch
        syncService.indexVenue(savedVenue);

        return VenueOverviewDto.fromEntity(savedVenue);
    }

    public VenueOverviewDto findVenueById(long venueId) {
        Venue venue = venueRepository.findVenueById(venueId)
                .orElseThrow(() -> new NotFoundException("The venue you were looking for can't be found"));

        VenueOverviewDto dto = VenueOverviewDto.fromEntity(venue);

        // The PDF path lives only in Elasticsearch, not in the database.
        // Fetch it so the venue page can show a download button.
        var esDoc = syncService.getDocumentById(venueId);
        if (esDoc != null) {
            dto.setPdfPath(esDoc.getPdfPath());
        }

        return dto;
    }

    public UpdateVenueDto updateVenue(long venueId, UpdateVenueDto updateVenueDto) {
        Venue venue = venueRepository.findVenueById(venueId)
                .orElseThrow(() -> new NotFoundException("The venue you were looking for can't be found"));

        venue.setName(updateVenueDto.getName());
        venue.setAddress(updateVenueDto.getAddress());
        venue.setDescription(updateVenueDto.getDescription());
        venue.setType(updateVenueDto.getVenueType());

        venueRepository.save(venue);

        return UpdateVenueDto.fromEntity(venue);
    }

    public void deleteVenue(long venueId) {
        Venue venue = venueRepository.findVenueById(venueId)
                .orElseThrow(() -> new NotFoundException("The venue you were looking for can't be found"));


        if (!eventRepository.findByVenue(venue).isEmpty()) {
            throw new ConflictException("Venue can't be delete if events exist");
        }

        venueRepository.delete(venue);
    }

    public CreateVenueRequestDto convertToCreateVenueRequest(String venueJson) {
        CreateVenueRequestDto venue;
        try {
            venue = objectMapper.readValue(venueJson, CreateVenueRequestDto.class);
        } catch (JsonProcessingException e) {
            throw new ParseException("An expected error has occurred please try again in a moment!");
        }

        return venue;
    }

    public List<VenueOverviewDto> findTopVenues() {
        logger.info("Finding top venues");
        return venueRepository.findAllOrderByCalculatedRatingDesc()
                .stream()
                .limit(3)
                .map(VenueOverviewDto::fromEntity)
                .toList();
    }
}
