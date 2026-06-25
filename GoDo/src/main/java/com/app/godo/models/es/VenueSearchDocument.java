package com.app.godo.models.es;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "godo_venues")
@Setting(settingPath = "/elasticsearch/venue-index-settings.json")
public class VenueSearchDocument {


    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "serbian_analyzer",
            fielddata = true)
    private String name;

    @Field(type = FieldType.Keyword, name = "name_keyword")
    private String nameKeyword;

    @Field(type = FieldType.Text, analyzer = "serbian_analyzer")
    private String description;

    @Field(type = FieldType.Text, analyzer = "serbian_analyzer")
    private String pdfDescription;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Double)
    private Double averageRating;

    @Field(type = FieldType.Double)
    private Double performanceRating;

    @Field(type = FieldType.Double)
    private Double ambientRating;

    @Field(type = FieldType.Double)
    private Double venueRating;

    @Field(type = FieldType.Double)
    private Double overallImpressionRating;

    @Field(type = FieldType.Integer)
    private Integer reviewCount;

    @Field(type = FieldType.Keyword)
    private String address;

    @Field(type = FieldType.Keyword, index = false)
    private String pdfPath;

    @Field(type = FieldType.Keyword, index = false)
    private String imagePath;
}
