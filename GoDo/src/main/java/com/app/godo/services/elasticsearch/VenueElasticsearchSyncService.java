package com.app.godo.services.elasticsearch;

import com.app.godo.enums.ReviewStatus;
import com.app.godo.models.Rating;
import com.app.godo.models.Review;
import com.app.godo.models.Venue;
import com.app.godo.models.es.VenueSearchDocument;
import com.app.godo.repositories.elasticsearch.VenueElasticsearchRepository;
import com.app.godo.repositories.venue.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VenueElasticsearchSyncService {

    private final VenueElasticsearchRepository venueSearchRepository;
    private final VenueRepository venueRepository;

    // Index a venue WITHOUT PDF content
    public void indexVenue(Venue venue) {
        VenueSearchDocument document = convertToSearchDocument(venue);
        venueSearchRepository.save(document);
        log.info("Indexed venue '{}' (ID: {}) to Elasticsearch", venue.getName(), venue.getId());
    }

    // Index a venue WITH PDF content and file paths
    // Called when creating a venue with PDF, or when re-indexing after PDF upload
    public void indexVenueWithPdf(Venue venue, String pdfText, String pdfPath, String imagePath) {
        VenueSearchDocument document = convertToSearchDocument(venue);
        document.setPdfDescription(pdfText);
        document.setPdfPath(pdfPath);
        document.setImagePath(imagePath);
        venueSearchRepository.save(document);
        log.info("Indexed venue '{}' (ID: {}) with PDF content to Elasticsearch",
                venue.getName(), venue.getId());
    }

    // Update only the PDF-related fields in an existing ES document
    // Called when a PDF is uploaded/replaced for an existing venue
    public void updatePdfContent(Long venueId, String pdfText, String pdfPath) {
        venueSearchRepository.findById(venueId).ifPresent(doc -> {
            doc.setPdfDescription(pdfText);
            doc.setPdfPath(pdfPath);
            venueSearchRepository.save(doc);
            log.info("Updated PDF content for venue ID: {}", venueId);
        });
    }

    public void indexVenueById(Long venueId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new RuntimeException("Venue not found: " + venueId));
        indexVenue(venue);
    }

    public void deleteVenueFromIndex(Long venueId) {
        venueSearchRepository.deleteById(venueId);
        log.info("Deleted venue (ID: {}) from Elasticsearch index", venueId);
    }

    public void indexAllVenues() {
        List<Venue> allVenues = venueRepository.findAll();
        log.info("Starting bulk indexing of {} venues", allVenues.size());

        for (Venue venue : allVenues) {
            try {
                indexVenue(venue);
            } catch (Exception e) {
                log.error("Failed to index venue '{}' (ID: {}): {}",
                        venue.getName(), venue.getId(), e.getMessage());
            }
        }

        log.info("Completed bulk indexing of venues");
    }


    // helpers

    private VenueSearchDocument convertToSearchDocument(Venue venue) {
        List<Review> activeReviews = getActiveReviews(venue);

        return VenueSearchDocument.builder()
                .id(venue.getId())
                .name(venue.getName())
                .nameKeyword(venue.getName())
                .description(venue.getDescription())
                .address(venue.getAddress())
                .type(venue.getType().name())
                .averageRating(venue.getAverageRating())
                .performanceRating(calculateAverageRating(activeReviews, "performance"))
                .ambientRating(calculateAverageRating(activeReviews, "ambient"))
                .venueRating(calculateAverageRating(activeReviews, "venue"))
                .overallImpressionRating(calculateAverageRating(activeReviews, "overallImpression"))
                .reviewCount(activeReviews.size())
                .imagePath(venue.getImage() != null ? venue.getImage().getPath() : null)
                .build();
    }

    private List<Review> getActiveReviews(Venue venue) {
        if (venue.getReviews() == null) {
            return List.of();
        }
        return venue.getReviews().stream()
                .filter(r -> r.getStatus() != ReviewStatus.DELETED)
                .toList();
    }

    private Double calculateAverageRating(List<Review> reviews, String category) {
        List<Integer> ratings = reviews.stream()
                .filter(r -> r.getRating() != null)
                .map(r -> getRatingByCategory(r.getRating(), category))
                .filter(r -> r > 0)
                .toList();

        if (ratings.isEmpty()) {
            return 0.0;
        }

        return ratings.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }

    private int getRatingByCategory(Rating rating, String category) {
        return switch (category) {
            case "performance" -> rating.getPerformance();
            case "ambient" -> rating.getAmbient();
            case "venue" -> rating.getVenue();
            case "overallImpression" -> rating.getOverallImpression();
            default -> 0;
        };
    }
}
