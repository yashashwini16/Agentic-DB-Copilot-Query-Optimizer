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
@Schema(description = "Column metadata definition")
public class ColumnMetadataDto {
    private String columnName;
    private String dataType;
    private boolean isNullable;
    private boolean isPrimaryKey;
    private boolean isForeignKey;
    private String foreignKeyRefTable;
    private String foreignKeyRefColumn;
    private String defaultValue;
    private String remarks;
}
