package com.dbcopilot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Semantic Query Cache Entry")
public class CacheEntryDto {
    private Long id;
    private String naturalQuery;
    private String generatedSql;
    private String explanation;
    private Long executionLatencyMs;
    private Integer hitCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
    private Double similarityScore;
}
