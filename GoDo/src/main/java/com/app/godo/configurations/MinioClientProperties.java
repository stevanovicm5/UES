package com.app.godo.configurations;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MinioClientProperties {
    private String endpoint;
    private String publicUrl;
    private String bucket;
}
