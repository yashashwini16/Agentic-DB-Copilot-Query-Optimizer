package com.dbcopilot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "semantic_query_cache")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticQueryCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "natural_query", nullable = false, columnDefinition = "TEXT")
    private String naturalQuery;

    @Column(name = "query_hash", nullable = false, unique = true, length = 64)
    private String queryHash;

    @Column(name = "generated_sql", nullable = false, columnDefinition = "TEXT")
    private String generatedSql;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "execution_latency_ms")
    private Long executionLatencyMs;

    @Column(name = "hit_count")
    @Builder.Default
    private Integer hitCount = 1;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_accessed_at")
    @Builder.Default
    private LocalDateTime lastAccessedAt = LocalDateTime.now();
}
