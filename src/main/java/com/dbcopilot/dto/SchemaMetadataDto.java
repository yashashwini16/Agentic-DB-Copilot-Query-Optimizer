package com.dbcopilot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Overall database schema definition")
public class SchemaMetadataDto {
    private String databaseProductName;
    private String databaseProductVersion;
    private String schemaName;
    @Builder.Default
    private List<TableMetadataDto> tables = new ArrayList<>();
}
