package com.inventory.management.controller;

import com.inventory.management.dto.OrderDTO;
import com.inventory.management.model.Order;
import com.inventory.management.model.OrderItem;
import com.inventory.management.model.Product;
import com.inventory.management.model.User;
import com.inventory.management.repository.ProductRepository;
import com.inventory.management.repository.UserRepository;
import com.inventory.management.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        try {
            User currentUser = getCurrentUser();
            List<Order> orders = orderService.getOrdersByUser(currentUser);
            List<OrderDTO> orderDTOs = orders.stream()
                    .map(OrderDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(orderDTOs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            Optional<Order> order = orderService.getOrderByIdAndUser(id, currentUser);
            return order.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/user")
    public ResponseEntity<List<Order>> getOrdersByUser() {
        User currentUser = getCurrentUser();
        List<Order> orders = orderService.getOrdersByUser(currentUser);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/test")
    public ResponseEntity<?> testOrderCreation() {
        try {
            System.out.println("=== TEST ORDER CREATION ===");
            User currentUser = getCurrentUser();
            System.out.println("Current user: " + currentUser.getUsername() + " (ID: " + currentUser.getId() + ")");
            
            // Get a product for testing
            List<Product> products = productRepository.findByUserOrderByName(currentUser);
            if (products.isEmpty()) {
                return ResponseEntity.badRequest().body("No products found for testing");
            }
            
            Product testProduct = products.get(0);
            System.out.println("Test product: " + testProduct.getName() + " (ID: " + testProduct.getId() + ", Stock: " + testProduct.getQuantity() + ")");
            
            // Create a simple order
            Order testOrder = new Order();
            testOrder.setUser(currentUser);
            testOrder.setStatus("PENDING");
            testOrder.setOrderNumber("TEST-" + System.currentTimeMillis());
            testOrder.setTotalAmount(BigDecimal.valueOf(10.00));
            
            // Create order item
            OrderItem testItem = new OrderItem(testProduct, 1, testProduct.getPrice());
            testOrder.addOrderItem(testItem);
            
            System.out.println("Test order created with " + testOrder.getOrderItems().size() + " items");
            System.out.println("Test order total: " + testOrder.getTotalAmount());
            
            // Try to save the order
            Order savedOrder = orderService.createOrder(testOrder, currentUser);
            System.out.println("Test order saved successfully with ID: " + savedOrder.getId());
            
            return ResponseEntity.ok("Test order created successfully with ID: " + savedOrder.getId());
            
        } catch (Exception e) {
            System.out.println("=== TEST ORDER CREATION ERROR ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Test order creation failed: " + e.getMessage());
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> orderRequest) {
        try {
            System.out.println("=== ORDER CREATION START ===");
            System.out.println("Creating order with request: " + orderRequest);
            System.out.println("Request class: " + orderRequest.getClass().getName());
            User currentUser = getCurrentUser();
            System.out.println("Current user: " + currentUser.getUsername() + " (ID: " + currentUser.getId() + ")");
            
            Order order = new Order();
            order.setUser(currentUser);
            order.setStatus("PENDING");
            System.out.println("Created order object with user: " + currentUser.getUsername());
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orderItemsData = (List<Map<String, Object>>) orderRequest.get("orderItems");
            System.out.println("Order items data type: " + (orderItemsData != null ? orderItemsData.getClass().getName() : "null"));
            
            if (orderItemsData == null || orderItemsData.isEmpty()) {
                System.out.println("Order items data is null or empty");
                return ResponseEntity.badRequest().body("Order must contain at least one item");
            }
            
            System.out.println("Order items data is valid, size: " + orderItemsData.size());
            
            System.out.println("Processing " + orderItemsData.size() + " order items");
            System.out.println("Order items data: " + orderItemsData);
            BigDecimal totalAmount = BigDecimal.ZERO;
            
            for (int i = 0; i < orderItemsData.size(); i++) {
                Map<String, Object> itemData = orderItemsData.get(i);
                System.out.println("Processing item " + (i + 1) + ": " + itemData);
                System.out.println("Item product data: " + itemData.get("product"));
                @SuppressWarnings("unchecked")
                Map<String, Object> productData = (Map<String, Object>) itemData.get("product");
                System.out.println("Product data: " + productData);
                
                if (productData == null) {
                    System.out.println("ERROR: Product data is null for item " + (i + 1));
                    return ResponseEntity.badRequest().body("Product data is missing for item " + (i + 1));
                }
                
                Long productId = Long.valueOf(productData.get("id").toString());
                Integer quantity = Integer.valueOf(itemData.get("quantity").toString());
                BigDecimal unitPrice = new BigDecimal(itemData.get("unitPrice").toString());
                System.out.println("Parsed values - Product ID: " + productId + ", Quantity: " + quantity + ", Unit Price: " + unitPrice);
                
                System.out.println("Current user ID: " + currentUser.getId());
                
                Product product = productRepository.findByIdAndUser(productId, currentUser)
                    .orElseThrow(() -> new RuntimeException("Product not found or you don't have access to it. Product ID: " + productId + ", User: " + currentUser.getUsername()));
                
                System.out.println("Found product: " + product.getName() + " with stock: " + product.getQuantity() + ", Product user ID: " + product.getUser().getId());
                
                OrderItem orderItem = new OrderItem(product, quantity, unitPrice);
                order.addOrderItem(orderItem);
                System.out.println("Added order item - Product: " + product.getName() + ", Quantity: " + quantity + ", Unit Price: " + unitPrice);
                
                totalAmount = totalAmount.add(orderItem.getTotalPrice());
                System.out.println("Updated total amount: " + totalAmount);
            }
            
            order.setTotalAmount(totalAmount);
            System.out.println("Final total amount: " + totalAmount);
            System.out.println("Order items count: " + order.getOrderItems().size());
            System.out.println("Order items: " + order.getOrderItems());
            System.out.println("Order items details:");
            order.getOrderItems().forEach(item -> {
                System.out.println("  - Product: " + item.getProduct().getName() + ", Quantity: " + item.getQuantity() + ", Unit Price: " + item.getUnitPrice() + ", Total: " + item.getTotalPrice());
            });
            
            System.out.println("About to call orderService.createOrder...");
            Order createdOrder = orderService.createOrder(order, currentUser);
            System.out.println("Order created successfully with ID: " + createdOrder.getId());
            System.out.println("Order details - Number: " + createdOrder.getOrderNumber() + ", Status: " + createdOrder.getStatus() + ", Total: " + createdOrder.getTotalAmount());
            System.out.println("Order items in created order: " + createdOrder.getOrderItems().size());
            System.out.println("Created order items details:");
            createdOrder.getOrderItems().forEach(item -> {
                System.out.println("  - Product: " + item.getProduct().getName() + ", Quantity: " + item.getQuantity() + ", Unit Price: " + item.getUnitPrice() + ", Total: " + item.getTotalPrice());
            });
            System.out.println("=== ORDER CREATION SUCCESS ===");
            return ResponseEntity.ok(createdOrder);
            
        } catch (RuntimeException e) {
            System.out.println("=== RUNTIME EXCEPTION ===");
            System.out.println("Runtime exception during order creation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            System.out.println("=== GENERAL EXCEPTION ===");
            System.out.println("General exception during order creation: " + e.getMessage());
            e.printStackTrace();
            System.out.println("Exception type: " + e.getClass().getName());
            if (e.getCause() != null) {
                System.out.println("Cause: " + e.getCause().getMessage());
                e.getCause().printStackTrace();
            }
            return ResponseEntity.internalServerError().body("Internal server error occurred: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            User currentUser = getCurrentUser();
            String status = request.get("status");
            
            Optional<Order> orderOpt = orderService.getOrderByIdAndUser(id, currentUser);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Order updatedOrder = orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error updating order status: " + e.getMessage());
        }
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable String status) {
        try {
            User currentUser = getCurrentUser();
            List<Order> orders = orderService.getOrdersByStatusAndUser(status, currentUser);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        System.out.println("Getting current user for username: " + username);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
        System.out.println("Found user: " + user.getUsername() + " with ID: " + user.getId());
        return user;
    }
}
