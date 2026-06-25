package com.app.godo.dtos.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueSearchQueryDto {

    private String name;
    private String description;
    private String pdfDescription;

    private Integer minReviews;
    private Integer maxReviews;

    private Double minRating;
    private Double maxRating;

    private String ratingCategory;

    private String operator;
}
