package com.pricetracker.service;

import com.pricetracker.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {
    
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username:}")
    private String fromEmail;
    
    @Value("${app.notification.email.enabled:true}")
    private boolean emailEnabled;
    
    /**
     * Send price drop notification email
     */
    public void sendPriceDropNotification(Product product, Double oldPrice, Double newPrice) {
        log.info("Attempting to send price drop notification for product: {}", product.getName());
        
        if (!emailEnabled) {
            log.warn("Email notifications are disabled in configuration");
            return;
        }
        
        if (mailSender == null) {
            log.error("JavaMailSender is not configured. Check your email settings in application.properties");
            return;
        }
        
        String email = product.getNotificationEmail();
        if (email == null || email.isEmpty()) {
            // Use default email from configuration if product doesn't have one
            email = fromEmail;
            log.debug("Using default email from configuration: {}", email);
        }
        
        if (email == null || email.isEmpty()) {
            log.warn("No email address configured for product: {}. Please add email in product settings or configure spring.mail.username", product.getName());
            return;
        }
        
        log.info("Sending price drop notification to: {}", email);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("💰 Price Drop Alert: " + product.getName());
            
            double priceDrop = oldPrice - newPrice;
            double percentageDrop = (priceDrop / oldPrice) * 100;
            
            String body = String.format(
                "Great news! The price of '%s' has dropped!\n\n" +
                "📦 Product: %s\n" +
                "🔗 URL: %s\n\n" +
                "💰 Price Details:\n" +
                "   Previous Price: ₹%.2f\n" +
                "   Current Price: ₹%.2f\n" +
                "   You Save: ₹%.2f (%.1f%%)\n" +
                "   Target Price: ₹%.2f\n\n" +
                "🎯 Status: %s\n\n" +
                "Click here to view: %s",
                product.getName(),
                product.getName(),
                product.getUrl(),
                oldPrice,
                newPrice,
                priceDrop,
                percentageDrop,
                product.getTargetPrice(),
                newPrice <= product.getTargetPrice() ? "✅ Price is at or below your target!" : "Getting closer to your target!",
                product.getUrl()
            );
            
            message.setText(body);
            
            mailSender.send(message);
            log.info("Price drop notification email sent to {} for product: {}", email, product.getName());
            
        } catch (Exception e) {
            log.error("Failed to send price drop notification email: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send price below target notification
     */
    public void sendTargetPriceReachedNotification(Product product) {
        log.info("Attempting to send target price reached notification for product: {}", product.getName());
        
        if (!emailEnabled) {
            log.warn("Email notifications are disabled in configuration");
            return;
        }
        
        if (mailSender == null) {
            log.error("JavaMailSender is not configured. Check your email settings in application.properties");
            return;
        }
        
        String email = product.getNotificationEmail();
        if (email == null || email.isEmpty()) {
            email = fromEmail;
            log.debug("Using default email from configuration: {}", email);
        }
        
        if (email == null || email.isEmpty()) {
            log.warn("No email address configured for product: {}. Please add email in product settings or configure spring.mail.username", product.getName());
            return;
        }
        
        log.info("Sending target price reached notification to: {}", email);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("🎯 Target Price Reached: " + product.getName());
            
            double savings = product.getTargetPrice() - product.getCurrentPrice();
            double savingsPercent = (savings / product.getTargetPrice()) * 100;
            
            String body = String.format(
                "Great news! The price of '%s' is below your target price!\n\n" +
                "📦 Product: %s\n" +
                "💰 Current Price: ₹%.2f\n" +
                "🎯 Your Target: ₹%.2f\n" +
                "💵 You Save: ₹%.2f (%.1f%%)\n" +
                "🔗 URL: %s\n\n" +
                "Don't miss out! Click here to purchase: %s",
                product.getName(),
                product.getName(),
                product.getCurrentPrice(),
                product.getTargetPrice(),
                savings,
                savingsPercent,
                product.getUrl(),
                product.getUrl()
            );
            
            message.setText(body);
            mailSender.send(message);
            log.info("Target price reached notification sent to {} for product: {}", email, product.getName());
            
        } catch (Exception e) {
            log.error("Failed to send target price notification: {}", e.getMessage(), e);
        }
    }
}

