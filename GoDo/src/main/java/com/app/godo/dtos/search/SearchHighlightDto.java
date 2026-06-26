package com.app.godo.dtos.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchHighlightDto {
    private String field;    // friendly label: "Name", "Description", "PDF"
    private String snippet;  // the highlighted text with <em> tags
}
