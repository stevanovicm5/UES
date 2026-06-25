package com.app.godo.dtos.search;

import com.app.godo.models.es.VenueSearchDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueSearchResultDto {

    private Long id;
    private String name;
    private String description;
    private String address;
    private String type;
    private Double averageRating;
    private Integer reviewCount;
    private String imagePath;
    private String pdfPath;

    private List<String> highlights;

    public static VenueSearchResultDto fromDocument(VenueSearchDocument doc) {
        return VenueSearchResultDto.builder()
                .id(doc.getId())
                .name(doc.getName())
                .description(doc.getDescription())
                .address(doc.getAddress())
                .type(doc.getType())
                .averageRating(doc.getAverageRating())
                .reviewCount(doc.getReviewCount())
                .imagePath(doc.getImagePath())
                .pdfPath(doc.getPdfPath())
                .build();
    }
}