package com.dbcopilot.repository;

import com.dbcopilot.model.SemanticQueryCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SemanticQueryCacheRepository extends JpaRepository<SemanticQueryCache, Long> {

    Optional<SemanticQueryCache> findByQueryHash(String queryHash);

    @Modifying
    @Query("UPDATE SemanticQueryCache c SET c.hitCount = c.hitCount + 1, c.lastAccessedAt = :now WHERE c.id = :id")
    void incrementHitCount(@Param("id") Long id, @Param("now") LocalDateTime now);
}
