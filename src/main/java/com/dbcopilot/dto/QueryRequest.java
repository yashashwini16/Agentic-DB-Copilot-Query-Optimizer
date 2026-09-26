package com.dbcopilot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for Natural Language to SQL query translation")
public class QueryRequest {

    @NotBlank(message = "Prompt cannot be blank")
    @Schema(description = "Natural language question or request", example = "Show me the top 5 highest spending customers with their total order count")
    private String prompt;

    @Builder.Default
    @Schema(description = "Whether to check Pgvector semantic query cache before invoking LLM", example = "true")
    private boolean enableCache = true;

    @Builder.Default
    @Schema(description = "Maximum self-correction retries when execution error is detected", example = "3")
    private int maxCorrectionAttempts = 3;

    @Builder.Default
    @Schema(description = "Whether to run EXPLAIN query plan analysis and optimization suggestions", example = "true")
    private boolean analyzePerformance = true;
}
