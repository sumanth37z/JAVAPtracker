package com.pricetracker.controller;

import com.pricetracker.model.PriceHistory;
import com.pricetracker.model.Product;
import com.pricetracker.repository.PriceHistoryRepository;
import com.pricetracker.repository.ProductRepository;
import com.pricetracker.service.EmailNotificationService;
import com.pricetracker.service.PriceTrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/")
public class ProductController {
    
    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PriceTrackingService priceTrackingService;
    private final EmailNotificationService emailNotificationService;
    
    public ProductController(ProductRepository productRepository,
                             PriceHistoryRepository priceHistoryRepository,
                             PriceTrackingService priceTrackingService,
                             EmailNotificationService emailNotificationService) {
        this.productRepository = productRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.priceTrackingService = priceTrackingService;
        this.emailNotificationService = emailNotificationService;
    }
    
    @GetMapping
    public String index(Model model) {
        List<Product> products = productRepository.findAll();
        model.addAttribute("products", products);
        return "index";
    }
    
    @GetMapping("/products/{id}")
    public String viewProduct(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        List<PriceHistory> history = priceHistoryRepository.findByProductIdOrderByRecordedAtDesc(id);
        model.addAttribute("product", product);
        model.addAttribute("history", history);
        
        return "product-detail";
    }
    
    @PostMapping("/api/products")
    @ResponseBody
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        if (product.getCurrentPrice() == null) {
            product.setCurrentPrice(0.0);
        }
        Product saved = productRepository.save(product);
        
        // Fetch initial price
        priceTrackingService.fetchPrice(saved);
        
        return ResponseEntity.ok(saved);
    }
    
    @GetMapping("/api/products")
    @ResponseBody
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }
    
    @GetMapping("/api/products/{id}")
    @ResponseBody
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/api/products/{id}")
    @ResponseBody
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setName(productDetails.getName());
                    product.setUrl(productDetails.getUrl());
                    product.setDescription(productDetails.getDescription());
                    product.setTargetPrice(productDetails.getTargetPrice());
                    product.setPriceSelector(productDetails.getPriceSelector());
                    product.setNotificationEmail(productDetails.getNotificationEmail());
                    product.setIsActive(productDetails.getIsActive());
                    return ResponseEntity.ok(productRepository.save(product));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/api/products/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        try {
            if (!productRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            
            // Delete all price history first (cascade should handle this, but being explicit)
            priceHistoryRepository.deleteAll(priceHistoryRepository.findByProductIdOrderByRecordedAtDesc(id));
            
            // Delete the product
            productRepository.deleteById(id);
            
            return ResponseEntity.ok("Product deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting product: " + e.getMessage());
        }
    }
    
    @PostMapping("/api/products/{id}/check")
    @ResponseBody
    public ResponseEntity<Product> checkProductPrice(@PathVariable Long id) {
        try {
            priceTrackingService.checkProductPrice(id);
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/api/products/{id}/history")
    @ResponseBody
    public ResponseEntity<List<PriceHistory>> getPriceHistory(@PathVariable Long id) {
        List<PriceHistory> history = priceHistoryRepository.findByProductIdOrderByRecordedAtDesc(id);
        return ResponseEntity.ok(history);
    }
    
    @PostMapping("/api/products/{id}/test-email")
    @ResponseBody
    public ResponseEntity<String> testEmailNotification(@PathVariable Long id) {
        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            
            // Send test notification
            if (product.getCurrentPrice() != null && product.getCurrentPrice() > 0) {
                emailNotificationService.sendPriceDropNotification(
                    product, 
                    product.getCurrentPrice() + 100, 
                    product.getCurrentPrice()
                );
                return ResponseEntity.ok("Test email notification sent. Check your email and application logs.");
            } else {
                return ResponseEntity.badRequest().body("Product doesn't have a valid price. Please check the price first.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error sending test email: " + e.getMessage());
        }
    }
    
    @PostMapping("/api/products/{id}/test-desktop")
    @ResponseBody
    public ResponseEntity<String> testDesktopNotification(@PathVariable Long id) {
        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            if (product.getCurrentPrice() != null && product.getCurrentPrice() > 0) {
                // Show both price-drop-style and target-style desktop notifications for testing
                desktopNotificationService.showPriceDropNotification(product, product.getCurrentPrice() + 100, product.getCurrentPrice());
                desktopNotificationService.showTargetPriceReachedNotification(product);
                return ResponseEntity.ok("Desktop test notifications attempted (check system tray).");
            } else {
                return ResponseEntity.badRequest().body("Product doesn't have a valid price. Please check the price first.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error showing desktop notification: " + e.getMessage());
        }
    }
}

