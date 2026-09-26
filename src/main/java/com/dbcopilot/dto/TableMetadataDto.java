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
@Schema(description = "Table metadata structure including columns and sample records")
public class TableMetadataDto {
    private String tableName;
    private String tableType;
    private long estimatedRowCount;
    @Builder.Default
    private List<ColumnMetadataDto> columns = new ArrayList<>();
    @Builder.Default
    private List<String> primaryKeys = new ArrayList<>();
    @Builder.Default
    private List<String> foreignKeys = new ArrayList<>();
}
