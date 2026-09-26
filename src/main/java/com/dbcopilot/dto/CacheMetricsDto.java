package com.dbcopilot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated Semantic Query Cache Metrics")
public class CacheMetricsDto {
    private long totalQueries;
    private long cacheHits;
    private long cacheMisses;
    private double hitRatePercent;
    private double averageLatencyCachedMs;
    private double averageLatencyUncachedMs;
    private double latencyReductionPercent;
    private long totalTokensSavedEstimate;
}
