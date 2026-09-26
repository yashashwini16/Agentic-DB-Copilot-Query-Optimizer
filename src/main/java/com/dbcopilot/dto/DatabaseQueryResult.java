package com.dbcopilot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Structured relational query execution result")
public class DatabaseQueryResult {

    @Schema(description = "Whether the SQL query executed successfully on the database", example = "true")
    private boolean success;

    @Builder.Default
    @Schema(description = "Column names in projection order")
    private List<String> columns = new ArrayList<>();

    @Builder.Default
    @Schema(description = "Column SQL data types")
    private List<String> columnTypes = new ArrayList<>();

    @Builder.Default
    @Schema(description = "Tabular row data represented as maps of column name to value")
    private List<Map<String, Object>> rows = new ArrayList<>();

    @Schema(description = "Total number of rows returned", example = "5")
    private int rowCount;

    @Schema(description = "Execution time in milliseconds on the database engine", example = "12")
    private long executionTimeMs;

    @Schema(description = "SQL error message if execution failed")
    private String errorMessage;

    @Schema(description = "SQL state code if execution failed", example = "42703")
    private String sqlState;
}
