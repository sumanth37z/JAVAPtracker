package com.pricetracker.repository;

import com.pricetracker.model.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findByProductIdOrderByRecordedAtDesc(Long productId);
    
    @Query("SELECT ph FROM PriceHistory ph WHERE ph.product.id = :productId ORDER BY ph.recordedAt DESC")
    List<PriceHistory> findRecentHistoryByProductId(@Param("productId") Long productId);
}


