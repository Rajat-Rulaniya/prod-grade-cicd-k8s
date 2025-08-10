package com.inventory.management.controller;

import com.inventory.management.model.InventoryHistory;
import com.inventory.management.model.Product;
import com.inventory.management.model.User;
import com.inventory.management.repository.UserRepository;
import com.inventory.management.service.InventoryHistoryService;
import com.inventory.management.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/history")
public class InventoryHistoryController {
    
    @Autowired
    private InventoryHistoryService inventoryHistoryService;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping
    public ResponseEntity<List<InventoryHistory>> getAllHistory() {
        try {
            User currentUser = getCurrentUser();
            List<InventoryHistory> history = inventoryHistoryService.getAllHistoryByUser(currentUser);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<InventoryHistory>> getHistoryByProduct(@PathVariable Long productId) {
        try {
            User currentUser = getCurrentUser();
            Optional<Product> product = productService.getProductByIdAndUser(productId, currentUser);
            if (product.isPresent()) {
                List<InventoryHistory> history = inventoryHistoryService.getHistoryByProductAndUser(product.get(), currentUser);
                return ResponseEntity.ok(history);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/action/{action}")
    public ResponseEntity<List<InventoryHistory>> getHistoryByAction(@PathVariable String action) {
        try {
            User currentUser = getCurrentUser();
            List<InventoryHistory> history = inventoryHistoryService.getHistoryByActionAndUser(action, currentUser);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
}
