package com.inventory.management.repository;

import com.inventory.management.model.InventoryHistory;
import com.inventory.management.model.Product;
import com.inventory.management.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {
    
    List<InventoryHistory> findByProductOrderByCreatedAtDesc(Product product);
    
    List<InventoryHistory> findByUserOrderByCreatedAtDesc(User user);
    
    List<InventoryHistory> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    List<InventoryHistory> findByProductAndUserOrderByCreatedAtDesc(Product product, User user);
    
    List<InventoryHistory> findByActionAndUserOrderByCreatedAtDesc(String action, User user);
    
    @Query("SELECT ih FROM InventoryHistory ih WHERE ih.user = :user AND ih.createdAt >= :startDate ORDER BY ih.createdAt DESC")
    List<InventoryHistory> findRecentHistoryByUser(@Param("user") User user, @Param("startDate") LocalDateTime startDate);
    
    @Modifying
    @Transactional
    void deleteByProduct(Product product);
}
