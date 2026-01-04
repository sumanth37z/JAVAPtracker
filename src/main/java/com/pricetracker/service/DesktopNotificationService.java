package com.pricetracker.service;

import com.pricetracker.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

@Service
public class DesktopNotificationService {
    
    private static final Logger log = LoggerFactory.getLogger(DesktopNotificationService.class);
    
    @Value("${app.notification.desktop.enabled:true}")
    private boolean desktopNotificationsEnabled;
    
    private SystemTray systemTray;
    private TrayIcon trayIcon;
    private boolean initialized = false;
    
    public DesktopNotificationService() {
        initializeSystemTray();
    }
    
    private void initializeSystemTray() {
        if (!SystemTray.isSupported()) {
            log.warn("SystemTray is not supported on this system");
            return;
        }
        
        if (!desktopNotificationsEnabled) {
            log.debug("Desktop notifications are disabled");
            return;
        }
        
        try {
            systemTray = SystemTray.getSystemTray();
            
            // Create tray icon (using a simple default icon)
            Image image = createDefaultIcon();
            trayIcon = new TrayIcon(image, "Product Price Tracker");
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("Product Price Tracker - Click to open");
            
            // Add action listener to open browser
            trayIcon.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        java.awt.Desktop.getDesktop().browse(
                            new java.net.URI("http://localhost:8080")
                        );
                    } catch (Exception ex) {
                        log.error("Failed to open browser: {}", ex.getMessage());
                    }
                }
            });
            
            systemTray.add(trayIcon);
            initialized = true;
            log.info("Desktop notification system initialized");
            
        } catch (Exception e) {
            log.error("Failed to initialize system tray: {}", e.getMessage(), e);
        }
    }
    
    private Image createDefaultIcon() {
        // Create a simple colored icon
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 16, 16);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("P", 4, 13);
        g.dispose();
        return image;
    }
    
    /**
     * Show desktop notification for price drop
     */
    public void showPriceDropNotification(Product product, Double oldPrice, Double newPrice) {
        if (!initialized || !desktopNotificationsEnabled) {
            log.debug("Desktop notifications not available or disabled");
            return;
        }
        
        try {
            double priceDrop = oldPrice - newPrice;
            double percentageDrop = (priceDrop / oldPrice) * 100;
            
            String title = "💰 Price Drop Alert!";
            String message = String.format(
                "%s\n" +
                "Previous: ₹%.2f → Current: ₹%.2f\n" +
                "You Save: ₹%.2f (%.1f%%)",
                product.getName(),
                oldPrice,
                newPrice,
                priceDrop,
                percentageDrop
            );
            
            showNotification(title, message, TrayIcon.MessageType.INFO);
            log.info("Desktop notification shown for price drop: {}", product.getName());
            
        } catch (Exception e) {
            log.error("Failed to show desktop notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Show desktop notification for target price reached or price below target
     */
    public void showTargetPriceReachedNotification(Product product) {
        if (!initialized || !desktopNotificationsEnabled) {
            return;
        }
        
        try {
            double savings = product.getTargetPrice() - product.getCurrentPrice();
            double savingsPercent = (savings / product.getTargetPrice()) * 100;
            
            String title = "🎯 Price Below Target!";
            String message = String.format(
                "%s\n" +
                "Current Price: ₹%.2f\n" +
                "Your Target: ₹%.2f\n" +
                "You Save: ₹%.2f (%.1f%%)\n" +
                "Time to buy!",
                product.getName(),
                product.getCurrentPrice(),
                product.getTargetPrice(),
                savings,
                savingsPercent
            );
            
            showNotification(title, message, TrayIcon.MessageType.INFO);
            log.info("Desktop notification shown for price below target: {}", product.getName());
            
        } catch (Exception e) {
            log.error("Failed to show desktop notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Show a desktop notification
     */
    private void showNotification(String title, String message, TrayIcon.MessageType messageType) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, messageType);
        }
    }
    
    /**
     * Cleanup on shutdown
     */
    public void cleanup() {
        if (systemTray != null && trayIcon != null) {
            systemTray.remove(trayIcon);
        }
    }
}

