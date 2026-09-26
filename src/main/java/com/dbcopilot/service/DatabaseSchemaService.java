package com.dbcopilot.service;

import com.dbcopilot.dto.ColumnMetadataDto;
import com.dbcopilot.dto.SchemaMetadataDto;
import com.dbcopilot.dto.TableMetadataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseSchemaService {

    private final DataSource dataSource;

    // Cache the schema prompt string in memory to avoid repetitive introspection
    private String cachedSchemaContext;
    private SchemaMetadataDto cachedSchemaMetadata;

    public synchronized SchemaMetadataDto getSchemaMetadata() {
        if (cachedSchemaMetadata != null) {
            return cachedSchemaMetadata;
        }

        SchemaMetadataDto schemaDto = new SchemaMetadataDto();
        List<TableMetadataDto> tables = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            schemaDto.setDatabaseProductName(metaData.getDatabaseProductName());
            schemaDto.setDatabaseProductVersion(metaData.getDatabaseProductVersion());
            schemaDto.setSchemaName("public");

            String catalog = conn.getCatalog();
            String schema = "public";

            // If using H2 or other db, handle null schema
            if ("H2".equalsIgnoreCase(metaData.getDatabaseProductName())) {
                schema = "PUBLIC";
            }

            try (ResultSet tableRs = metaData.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
                while (tableRs.next()) {
                    String tableName = tableRs.getString("TABLE_NAME");
                    // Skip internal / system / cache tables from general SQL synthesis prompts unless requested
                    if (tableName.equalsIgnoreCase("semantic_query_cache") || tableName.startsWith("pg_")) {
                        continue;
                    }

                    TableMetadataDto tableDto = new TableMetadataDto();
                    tableDto.setTableName(tableName);
                    tableDto.setTableType(tableRs.getString("TABLE_TYPE"));

                    // Extract Primary Keys
                    List<String> primaryKeys = new ArrayList<>();
                    try (ResultSet pkRs = metaData.getPrimaryKeys(catalog, schema, tableName)) {
                        while (pkRs.next()) {
                            primaryKeys.add(pkRs.getString("COLUMN_NAME"));
                        }
                    }
                    tableDto.setPrimaryKeys(primaryKeys);

                    // Extract Foreign Keys
                    List<String> foreignKeys = new ArrayList<>();
                    Map<String, String[]> fkMap = new HashMap<>(); // col -> [pkTable, pkCol]
                    try (ResultSet fkRs = metaData.getImportedKeys(catalog, schema, tableName)) {
                        while (fkRs.next()) {
                            String fkCol = fkRs.getString("FKCOLUMN_NAME");
                            String pkTable = fkRs.getString("PKTABLE_NAME");
                            String pkCol = fkRs.getString("PKCOLUMN_NAME");
                            foreignKeys.add(String.format("%s -> %s(%s)", fkCol, pkTable, pkCol));
                            fkMap.put(fkCol, new String[]{pkTable, pkCol});
                        }
                    }
                    tableDto.setForeignKeys(foreignKeys);

                    // Extract Columns
                    List<ColumnMetadataDto> columns = new ArrayList<>();
                    try (ResultSet colRs = metaData.getColumns(catalog, schema, tableName, "%")) {
                        while (colRs.next()) {
                            String colName = colRs.getString("COLUMN_NAME");
                            String typeName = colRs.getString("TYPE_NAME");
                            boolean nullable = "YES".equalsIgnoreCase(colRs.getString("IS_NULLABLE"));
                            String defaultVal = colRs.getString("COLUMN_DEF");
                            String remarks = colRs.getString("REMARKS");

                            boolean isPk = primaryKeys.contains(colName);
                            boolean isFk = fkMap.containsKey(colName);
                            String fkTable = isFk ? fkMap.get(colName)[0] : null;
                            String fkCol = isFk ? fkMap.get(colName)[1] : null;

                            columns.add(ColumnMetadataDto.builder()
                                    .columnName(colName)
                                    .dataType(typeName)
                                    .isNullable(nullable)
                                    .isPrimaryKey(isPk)
                                    .isForeignKey(isFk)
                                    .foreignKeyRefTable(fkTable)
                                    .foreignKeyRefColumn(fkCol)
                                    .defaultValue(defaultVal)
                                    .remarks(remarks)
                                    .build());
                        }
                    }
                    tableDto.setColumns(columns);
                    tables.add(tableDto);
                }
            }
            schemaDto.setTables(tables);
            cachedSchemaMetadata = schemaDto;
        } catch (SQLException e) {
            log.error("Error inspecting database metadata", e);
        }

        return schemaDto;
    }

    /**
     * Generates a concise DDL / Schema summary prompt to inject into LLM system prompts
     */
    public synchronized String getSchemaPromptContext() {
        if (cachedSchemaContext != null) {
            return cachedSchemaContext;
        }

        SchemaMetadataDto metadata = getSchemaMetadata();
        StringBuilder sb = new StringBuilder();
        sb.append("### DATABASE SCHEMA (Dialect: PostgreSQL)\n\n");

        for (TableMetadataDto table : metadata.getTables()) {
            sb.append("Table: `").append(table.getTableName()).append("`\n");
            sb.append("Columns:\n");
            for (ColumnMetadataDto col : table.getColumns()) {
                sb.append("  - `").append(col.getColumnName()).append("` (").append(col.getDataType());
                if (col.isPrimaryKey()) {
                    sb.append(", PRIMARY KEY");
                }
                if (col.isForeignKey()) {
                    sb.append(", REFERENCES ").append(col.getForeignKeyRefTable()).append("(").append(col.getForeignKeyRefColumn()).append(")");
                }
                if (!col.isNullable()) {
                    sb.append(", NOT NULL");
                }
                sb.append(")\n");
            }
            if (!table.getForeignKeys().isEmpty()) {
                sb.append("Foreign Keys: ").append(String.join(", ", table.getForeignKeys())).append("\n");
            }
            sb.append("\n");
        }

        cachedSchemaContext = sb.toString();
        return cachedSchemaContext;
    }

    public synchronized void invalidateCache() {
        this.cachedSchemaContext = null;
        this.cachedSchemaMetadata = null;
    }
}
