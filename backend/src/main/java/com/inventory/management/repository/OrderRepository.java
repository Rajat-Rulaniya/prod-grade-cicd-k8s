package com.inventory.management.repository;

import com.inventory.management.model.Order;
import com.inventory.management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product WHERE o.user = :user ORDER BY o.orderDate DESC")
    List<Order> findByUserOrderByOrderDateDesc(@Param("user") User user);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product WHERE o.status = :status ORDER BY o.orderDate DESC")
    List<Order> findByStatusOrderByOrderDateDesc(@Param("status") String status);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product WHERE o.status = :status AND o.user = :user ORDER BY o.orderDate DESC")
    List<Order> findByStatusAndUserOrderByOrderDateDesc(@Param("status") String status, @Param("user") User user);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product WHERE o.user = :user AND o.orderDate >= :startDate ORDER BY o.orderDate DESC")
    List<Order> findRecentOrdersByUser(@Param("user") User user, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user = :user AND o.status = :status")
    long countByUserAndStatus(@Param("user") User user, @Param("status") String status);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product WHERE o.id = :id")
    Optional<Order> findByIdWithOrderItems(@Param("id") Long id);
}
