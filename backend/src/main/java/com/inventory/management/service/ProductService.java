package com.inventory.management.service;

import com.inventory.management.model.InventoryHistory;
import com.inventory.management.model.Product;
import com.inventory.management.model.User;
import com.inventory.management.repository.InventoryHistoryRepository;
import com.inventory.management.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {
    
    private final ProductRepository productRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    
    @Autowired
    public ProductService(ProductRepository productRepository, InventoryHistoryRepository inventoryHistoryRepository) {
        this.productRepository = productRepository;
        this.inventoryHistoryRepository = inventoryHistoryRepository;
    }
    
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<Product> getAllProductsByUser(User user) {
        return productRepository.findByUserOrderByName(user);
    }
    
    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public Optional<Product> getProductByIdAndUser(Long id, User user) {
        return productRepository.findByIdAndUser(id, user);
    }
    
    @Transactional(readOnly = true)
    public Optional<Product> getProductBySku(String sku) {
        return productRepository.findBySku(sku);
    }
    
    @Transactional(readOnly = true)
    public Optional<Product> getProductBySkuAndUser(String sku, User user) {
        return productRepository.findBySkuAndUser(sku, user);
    }
    
    public Product createProduct(Product product, User user) {
        if (productRepository.existsBySkuAndUser(product.getSku(), user)) {
            throw new RuntimeException("Product with SKU '" + product.getSku() + "' already exists for this user");
        }
        
        product.setUser(user);
        Product savedProduct = productRepository.save(product);
        
        InventoryHistory history = new InventoryHistory(
            savedProduct, user, "ADD", 0, savedProduct.getQuantity(), 
            "Product created: " + savedProduct.getName()
        );
        inventoryHistoryRepository.save(history);
        
        return savedProduct;
    }
    
    public Product updateProduct(Long id, Product productDetails, User user) {
        Product product = productRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Product not found or you don't have permission to access it"));
        
        if (!product.getSku().equals(productDetails.getSku()) && 
            productRepository.existsBySkuAndUser(productDetails.getSku(), user)) {
            throw new RuntimeException("Product with SKU '" + productDetails.getSku() + "' already exists for this user");
        }
        
        Integer previousQuantity = product.getQuantity();
        
        product.setSku(productDetails.getSku());
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setQuantity(productDetails.getQuantity());
        product.setCategory(productDetails.getCategory());
        
        Product updatedProduct = productRepository.save(product);
        
        InventoryHistory history = new InventoryHistory(
            updatedProduct, user, "UPDATE", previousQuantity, updatedProduct.getQuantity(),
            "Product updated: " + updatedProduct.getName()
        );
        inventoryHistoryRepository.save(history);
        
        return updatedProduct;
    }
    
    public void deleteProduct(Long id, User user) {
        Product product = productRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Product not found or you don't have permission to delete it"));
        
        try {
            inventoryHistoryRepository.deleteByProduct(product);
            productRepository.deleteById(id);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new RuntimeException("Cannot delete product because it has associated orders. Please delete related orders first.");
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (errorMessage.contains("foreign key") || errorMessage.contains("constraint") || 
                errorMessage.contains("referenced") || errorMessage.contains("violates")) {
                throw new RuntimeException("Cannot delete product because it has associated references. Please delete related records first.");
            } else {
                throw new RuntimeException("Error deleting product: " + e.getMessage());
            }
        }
    }
    
    public Product updateProductQuantity(Long id, Integer newQuantity, User user) {
        Product product = productRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Product not found or you don't have permission to access it"));
        
        Integer previousQuantity = product.getQuantity();
        product.setQuantity(newQuantity);
        
        Product updatedProduct = productRepository.save(product);
        
        InventoryHistory history = new InventoryHistory(
            updatedProduct, user, "UPDATE", previousQuantity, newQuantity,
            "Quantity updated for: " + updatedProduct.getName()
        );
        inventoryHistoryRepository.save(history);
        
        return updatedProduct;
    }
    
    @Transactional(readOnly = true)
    public List<Product> searchProducts(String name) {
        return productRepository.findByNameContainingIgnoreCaseOrderByName(name);
    }
    
    @Transactional(readOnly = true)
    public List<Product> searchProductsByUser(String name, User user) {
        return productRepository.findByNameContainingIgnoreCaseAndUserOrderByName(name, user);
    }
    
    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryOrderByName(category);
    }
    
    @Transactional(readOnly = true)
    public List<Product> getProductsByCategoryAndUser(String category, User user) {
        return productRepository.findByCategoryAndUserOrderByName(category, user);
    }
    
    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts(User user, Integer threshold) {
        return productRepository.findLowStockProductsByUser(user, threshold != null ? threshold : 10);
    }
}
