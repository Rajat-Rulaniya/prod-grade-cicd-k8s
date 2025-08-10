package com.inventory.management.controller;

import com.inventory.management.dto.ProductDTO;
import com.inventory.management.model.Product;
import com.inventory.management.model.User;
import com.inventory.management.repository.UserRepository;
import com.inventory.management.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        try {
            User currentUser = getCurrentUser();
            List<Product> products = productService.getAllProductsByUser(currentUser);
            List<ProductDTO> productDTOs = products.stream()
                    .map(ProductDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(productDTOs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            Optional<Product> product = productService.getProductByIdAndUser(id, currentUser);
            return product.map(p -> ResponseEntity.ok(new ProductDTO(p)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductDTO> getProductBySku(@PathVariable String sku) {
        try {
            User currentUser = getCurrentUser();
            Optional<Product> product = productService.getProductBySkuAndUser(sku, currentUser);
            return product.map(p -> ResponseEntity.ok(new ProductDTO(p)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Product product) {
        try {
            User currentUser = getCurrentUser();
            Product createdProduct = productService.createProduct(product, currentUser);
            return ResponseEntity.ok(new ProductDTO(createdProduct));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Internal server error occurred");
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
        try {
            User currentUser = getCurrentUser();
            Product updatedProduct = productService.updateProduct(id, productDetails, currentUser);
            return ResponseEntity.ok(new ProductDTO(updatedProduct));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Internal server error occurred");
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            productService.deleteProduct(id, currentUser);
            return ResponseEntity.ok().body("Product deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Internal server error occurred");
        }
    }
    
    @PutMapping("/{id}/quantity")
    public ResponseEntity<?> updateProductQuantity(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        try {
            User currentUser = getCurrentUser();
            Integer newQuantity = request.get("quantity");
            Product updatedProduct = productService.updateProductQuantity(id, newQuantity, currentUser);
            return ResponseEntity.ok(new ProductDTO(updatedProduct));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Internal server error occurred");
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<ProductDTO>> searchProducts(@RequestParam String name) {
        try {
            User currentUser = getCurrentUser();
            List<Product> products = productService.searchProductsByUser(name, currentUser);
            List<ProductDTO> productDTOs = products.stream()
                    .map(ProductDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(productDTOs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductDTO>> getProductsByCategory(@PathVariable String category) {
        try {
            User currentUser = getCurrentUser();
            List<Product> products = productService.getProductsByCategoryAndUser(category, currentUser);
            List<ProductDTO> productDTOs = products.stream()
                    .map(ProductDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(productDTOs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductDTO>> getLowStockProducts(@RequestParam(defaultValue = "10") Integer threshold) {
        try {
            User currentUser = getCurrentUser();
            List<Product> products = productService.getLowStockProducts(currentUser, threshold);
            List<ProductDTO> productDTOs = products.stream()
                    .map(ProductDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(productDTOs);
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
