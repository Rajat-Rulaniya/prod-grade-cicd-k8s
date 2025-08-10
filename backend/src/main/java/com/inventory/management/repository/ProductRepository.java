package com.inventory.management.repository;

import com.inventory.management.model.Product;
import com.inventory.management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Optional<Product> findBySku(String sku);
    
    Optional<Product> findBySkuAndUser(String sku, User user);
    
    Optional<Product> findByIdAndUser(Long id, User user);
    
    List<Product> findByUserOrderByName(User user);
    
    List<Product> findByCategoryOrderByName(String category);
    
    List<Product> findByCategoryAndUserOrderByName(String category, User user);
    
    List<Product> findByNameContainingIgnoreCaseOrderByName(String name);
    
    List<Product> findByNameContainingIgnoreCaseAndUserOrderByName(String name, User user);
    
    boolean existsBySkuAndUser(String sku, User user);
    
    @Query("SELECT p FROM Product p WHERE p.user = :user AND p.quantity <= :threshold ORDER BY p.quantity")
    List<Product> findLowStockProductsByUser(@Param("user") User user, @Param("threshold") Integer threshold);
}
