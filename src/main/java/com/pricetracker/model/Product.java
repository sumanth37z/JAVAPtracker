package com.pricetracker.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, length = 1000)
    private String url;
    
    @Column(length = 500)
    private String description;
    
    private String imageUrl;
    
    @Column(nullable = false)
    private Double targetPrice;
    
    @Column(nullable = false)
    private Double currentPrice;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime lastChecked;
    
    @Column(nullable = false)
    private Boolean isActive;
    
    // CSS selector for price extraction (optional)
    @Column(length = 500)
    private String priceSelector;
    
    // Email for price drop notifications
    @Column(length = 255)
    private String notificationEmail;
    
    // Track if we've already notified about price being below target (to avoid spam)
    @Column(nullable = false)
    private Boolean targetPriceNotified;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PriceHistory> priceHistory = new ArrayList<>();
    
    public Product() {
    }
    
    public Product(Long id, String name, String url, String description, String imageUrl, 
                   Double targetPrice, Double currentPrice, LocalDateTime createdAt, 
                   LocalDateTime lastChecked, Boolean isActive, String priceSelector, 
                   List<PriceHistory> priceHistory) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.description = description;
        this.imageUrl = imageUrl;
        this.targetPrice = targetPrice;
        this.currentPrice = currentPrice;
        this.createdAt = createdAt;
        this.lastChecked = lastChecked;
        this.isActive = isActive;
        this.priceSelector = priceSelector;
        this.priceHistory = priceHistory != null ? priceHistory : new ArrayList<>();
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastChecked = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (targetPriceNotified == null) {
            targetPriceNotified = false;
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public Double getTargetPrice() {
        return targetPrice;
    }
    
    public void setTargetPrice(Double targetPrice) {
        this.targetPrice = targetPrice;
    }
    
    public Double getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastChecked() {
        return lastChecked;
    }
    
    public void setLastChecked(LocalDateTime lastChecked) {
        this.lastChecked = lastChecked;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public String getPriceSelector() {
        return priceSelector;
    }
    
    public void setPriceSelector(String priceSelector) {
        this.priceSelector = priceSelector;
    }
    
    public String getNotificationEmail() {
        return notificationEmail;
    }
    
    public void setNotificationEmail(String notificationEmail) {
        this.notificationEmail = notificationEmail;
    }
    
    public Boolean getTargetPriceNotified() {
        return targetPriceNotified;
    }
    
    public void setTargetPriceNotified(Boolean targetPriceNotified) {
        this.targetPriceNotified = targetPriceNotified;
    }
    
    public List<PriceHistory> getPriceHistory() {
        return priceHistory;
    }
    
    public void setPriceHistory(List<PriceHistory> priceHistory) {
        this.priceHistory = priceHistory;
    }
}
