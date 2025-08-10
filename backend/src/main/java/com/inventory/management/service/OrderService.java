package com.inventory.management.service;

import com.inventory.management.model.*;
import com.inventory.management.repository.InventoryHistoryRepository;
import com.inventory.management.repository.OrderItemRepository;
import com.inventory.management.repository.OrderRepository;
import com.inventory.management.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;

@Service
@Transactional
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final OrderItemRepository orderItemRepository;
    
    @Autowired
    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, 
                       InventoryHistoryRepository inventoryHistoryRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.inventoryHistoryRepository = inventoryHistoryRepository;
        this.orderItemRepository = orderItemRepository;
    }
    
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findByIdWithOrderItems(id);
    }
    
    @Transactional(readOnly = true)
    public Optional<Order> getOrderByIdAndUser(Long id, User user) {
        return orderRepository.findByIdWithOrderItems(id)
                .filter(order -> order.getUser().getId().equals(user.getId()));
    }
    
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(User user) {
        return orderRepository.findByUserOrderByOrderDateDesc(user);
    }
    
    @Transactional
    public Order createOrder(Order order, User user) {
        System.out.println("=== ORDER SERVICE START ===");
        System.out.println("OrderService.createOrder called with order: " + order + ", user: " + user.getUsername());
        
        if (order == null) {
            throw new RuntimeException("Order cannot be null");
        }
        if (user == null) {
            throw new RuntimeException("User cannot be null");
        }
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            throw new RuntimeException("Order must contain at least one item");
        }
        
        // Step 1: Validate all products and check stock
        System.out.println("=== VALIDATING PRODUCTS AND STOCK ===");
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            if (product == null) {
                throw new RuntimeException("Product cannot be null in order item");
            }
            
            if (product.getUser() == null || !product.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Product '" + product.getName() + "' does not belong to this user");
            }
            
            Integer currentQuantity = product.getQuantity();
            Integer orderedQuantity = item.getQuantity();
            
            if (currentQuantity == null || orderedQuantity == null) {
                throw new RuntimeException("Product quantity and ordered quantity cannot be null");
            }
            
            if (currentQuantity < orderedQuantity) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName() + 
                    ". Available: " + currentQuantity + ", Requested: " + orderedQuantity);
            }
        }
        
        // Step 2: Create and save the order
        System.out.println("=== CREATING ORDER ===");
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");
        
        BigDecimal totalAmount = order.getOrderItems().stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);
        
        // Save order without order items first
        Order savedOrder = orderRepository.save(order);
        System.out.println("Order saved with ID: " + savedOrder.getId());
        
        // Step 3: Update product quantities and create inventory history
        System.out.println("=== UPDATING PRODUCTS AND CREATING HISTORY ===");
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            Integer currentQuantity = product.getQuantity();
            Integer orderedQuantity = item.getQuantity();
            
            // Update product quantity
            product.setQuantity(currentQuantity - orderedQuantity);
            productRepository.save(product);
            System.out.println("Updated product " + product.getName() + " quantity from " + currentQuantity + " to " + product.getQuantity());
            
            // Create inventory history
            InventoryHistory history = new InventoryHistory(
                product, user, "ORDER", currentQuantity, product.getQuantity(),
                "Order placed: " + savedOrder.getOrderNumber() + " - Quantity: " + orderedQuantity
            );
            inventoryHistoryRepository.save(history);
            System.out.println("Created inventory history for product: " + product.getName());
        }
        
        // Step 4: Save order items separately
        System.out.println("=== SAVING ORDER ITEMS ===");
        for (OrderItem item : order.getOrderItems()) {
            item.setOrder(savedOrder);
            orderItemRepository.save(item);
            System.out.println("Saved order item for product: " + item.getProduct().getName());
        }
        
        System.out.println("=== ORDER SERVICE SUCCESS ===");
        return savedOrder;
    }
    
    public Order updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findByIdWithOrderItems(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        return orderRepository.save(order);
    }
    
    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(String status) {
        return orderRepository.findByStatusOrderByOrderDateDesc(status);
    }
    
    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatusAndUser(String status, User user) {
        return orderRepository.findByStatusAndUserOrderByOrderDateDesc(status, user);
    }
    
    @Transactional(readOnly = true)
    public List<Order> getRecentOrdersByUser(User user, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return orderRepository.findRecentOrdersByUser(user, startDate);
    }
    
    @Transactional(readOnly = true)
    public long getOrderCountByStatus(User user, String status) {
        return orderRepository.countByUserAndStatus(user, status);
    }
}
