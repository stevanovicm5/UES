package com.app.godo.controllers.venue;


import com.app.godo.dtos.search.VenueSearchQueryDto;
import com.app.godo.dtos.search.VenueSearchResultDto;
import com.app.godo.dtos.venue.CreateVenueRequestDto;
import com.app.godo.dtos.venue.UpdateVenueDto;
import com.app.godo.dtos.venue.VenueOverviewDto;
import com.app.godo.services.elasticsearch.VenueElasticsearchSyncService;
import com.app.godo.services.elasticsearch.VenueSearchService;
import com.app.godo.services.files.MinIOService;
import com.app.godo.services.files.PdfProcessingService;
import com.app.godo.services.venue.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/venue")
@RequiredArgsConstructor
public class VenueController {
    private final VenueService venueService;
    private final VenueSearchService venueSearchService;
    private final VenueElasticsearchSyncService syncService;
    private final MinIOService minIOService;
    private final PdfProcessingService pdfProcessingService;

    // ========== EXISTING ENDPOINTS ==========

    @GetMapping
    public ResponseEntity<Page<VenueOverviewDto>> filterVenues(
            @RequestParam(value = "filter", defaultValue = "") String filter,
            @RequestParam(value = "venueType", defaultValue = "-1") int venueType,
            @PageableDefault(size = 8, sort = "name", direction = Sort.Direction.ASC) Pageable venuePage
    ){
        return ResponseEntity.ok(venueService.filterVenues(filter, venueType, venuePage));
    }

    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<VenueOverviewDto> createVenue(
            @RequestPart("venue") String venueJson,
            @RequestPart("image") MultipartFile imageFile) {

        CreateVenueRequestDto createVenueRequest = venueService.convertToCreateVenueRequest(venueJson);
        return ResponseEntity.ok(venueService.createVenue(createVenueRequest, imageFile));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VenueOverviewDto> getVenueById(@PathVariable long id) {
          return ResponseEntity.ok(venueService.findVenueById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UpdateVenueDto> updateVenue(@PathVariable long id, @RequestBody UpdateVenueDto updateVenueDto) {
        return ResponseEntity.ok(venueService.updateVenue(id, updateVenueDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVenue(@PathVariable long id) {
        venueService.deleteVenue(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/top")
    public ResponseEntity<List<VenueOverviewDto>> getTopVenues() {
        return ResponseEntity.ok(venueService.findTopVenues());
    }

    @GetMapping("/search")
    public ResponseEntity<Page<VenueSearchResultDto>> searchVenues(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "pdfDescription", required = false) String pdfDescription,
            @RequestParam(value = "minReviews", required = false) Integer minReviews,
            @RequestParam(value = "maxReviews", required = false) Integer maxReviews,
            @RequestParam(value = "minRating", required = false) Double minRating,
            @RequestParam(value = "maxRating", required = false) Double maxRating,
            @RequestParam(value = "ratingCategory", required = false) String ratingCategory,
            @RequestParam(value = "operator", defaultValue = "AND") String operator,
            @PageableDefault(size = 8) Pageable pageable
    ) {
        VenueSearchQueryDto query = VenueSearchQueryDto.builder()
                .name(name)
                .description(description)
                .pdfDescription(pdfDescription)
                .minReviews(minReviews)
                .maxReviews(maxReviews)
                .minRating(minRating)
                .maxRating(maxRating)
                .ratingCategory(ratingCategory)
                .operator(operator)
                .build();

        return ResponseEntity.ok(venueSearchService.search(query, pageable));
    }

    // "More Like This" - find similar venues
    //
    //   GET /api/venue/similar/5
    //   → Returns venues similar to venue ID 5
    @GetMapping("/similar/{id}")
    public ResponseEntity<List<VenueSearchResultDto>> findSimilarVenues(@PathVariable Long id) {
        return ResponseEntity.ok(venueSearchService.moreLikeThis(id));
    }

    // Upload a PDF document for a venue
    //
    //   POST /api/venue/3/pdf
    //   Body: multipart/form-data with "file" = the PDF
    //
    // What happens:
    //   1. Validates the file is a PDF
    //   2. Uploads PDF to MinIO
    //   3. Extracts text from PDF using PDFBox
    //   4. Updates the Elasticsearch index with PDF content
    @PostMapping("/{id}/pdf")
    public ResponseEntity<String> uploadVenuePdf(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile pdfFile
    ) {
        if (!pdfProcessingService.isValidPdf(pdfFile)) {
            return ResponseEntity.badRequest().body("File must be a valid PDF");
        }

        // 1. Upload PDF to MinIO
        String pdfFilename = minIOService.uploadFile(pdfFile);

        // 2. Extract text from PDF
        String pdfText = pdfProcessingService.extractText(pdfFile);

        // 3. Update Elasticsearch index with PDF content and path
        String pdfUrl = minIOService.getFileUrl(pdfFilename);
        syncService.updatePdfContent(id, pdfText, pdfUrl);

        return ResponseEntity.ok("PDF uploaded and indexed successfully");
    }

    // Download the PDF document for a venue
    //
    //   GET /api/venue/3/pdf
    //   → Returns the PDF file as a download
    //
    // How it works:
    //   1. Find the venue's ES document to get the PDF filename
    //   2. Download the file from MinIO
    //   3. Return it as a file download response
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadVenuePdf(@PathVariable Long id) {
        // 1. Find the venue's ES document to get the PDF path
        var document = syncService.getDocumentById(id);
        if (document == null || document.getPdfPath() == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. Extract filename from the full URL
        //    URL: "http://minio:9000/godo-files/abc123_info.pdf"
        //    Filename: "abc123_info.pdf"
        String pdfUrl = document.getPdfPath();
        String filename = pdfUrl.substring(pdfUrl.lastIndexOf('/') + 1);

        // 3. Download from MinIO and return as file download
        byte[] pdfBytes = minIOService.downloadFile(filename);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }
}
