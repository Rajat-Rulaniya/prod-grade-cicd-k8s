package com.inventory.management.service;

import com.inventory.management.model.InventoryHistory;
import com.inventory.management.model.Product;
import com.inventory.management.model.User;
import com.inventory.management.repository.InventoryHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventoryHistoryService {
    
    private final InventoryHistoryRepository inventoryHistoryRepository;
    
    @Autowired
    public InventoryHistoryService(InventoryHistoryRepository inventoryHistoryRepository) {
        this.inventoryHistoryRepository = inventoryHistoryRepository;
    }
    
    public List<InventoryHistory> getAllHistoryByUser(User user) {
        return inventoryHistoryRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    public List<InventoryHistory> getRecentHistoryByUserWithLimit(User user, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return inventoryHistoryRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }
    
    public List<InventoryHistory> getHistoryByProduct(Product product) {
        return inventoryHistoryRepository.findByProductOrderByCreatedAtDesc(product);
    }
    
    public List<InventoryHistory> getHistoryByProductAndUser(Product product, User user) {
        return inventoryHistoryRepository.findByProductAndUserOrderByCreatedAtDesc(product, user);
    }
    
    public List<InventoryHistory> getHistoryByActionAndUser(String action, User user) {
        return inventoryHistoryRepository.findByActionAndUserOrderByCreatedAtDesc(action, user);
    }
    
    public List<InventoryHistory> getRecentHistoryByUserInDays(User user, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return inventoryHistoryRepository.findRecentHistoryByUser(user, startDate);
    }
}
