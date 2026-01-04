package com.pricetracker.service;

import com.pricetracker.model.PriceHistory;
import com.pricetracker.model.Product;
import com.pricetracker.repository.PriceHistoryRepository;
import com.pricetracker.repository.ProductRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PriceTrackingService {
    
    private static final Logger log = LoggerFactory.getLogger(PriceTrackingService.class);
    
    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final EmailNotificationService emailNotificationService;
    private final DesktopNotificationService desktopNotificationService;
    
    public PriceTrackingService(ProductRepository productRepository, 
                                PriceHistoryRepository priceHistoryRepository,
                                EmailNotificationService emailNotificationService,
                                DesktopNotificationService desktopNotificationService) {
        this.productRepository = productRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.emailNotificationService = emailNotificationService;
        this.desktopNotificationService = desktopNotificationService;
    }
    
    // Enhanced pattern to match Indian number format (with commas, lakhs, crores)
    private static final Pattern PRICE_PATTERN = Pattern.compile("([\\d,]+(?:\\.\\d{2})?)");
    private static final Pattern INDIAN_PRICE_PATTERN = Pattern.compile("(?:₹|Rs\\.?|INR)?\\s*([\\d,]+(?:\\.\\d{2})?)");
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    
    /**
     * Fetch current price from a product URL
     */
    public Double fetchPrice(Product product) {
        try {
            log.info("Fetching price for product: {} from URL: {}", product.getName(), product.getUrl());
            
            Document doc = Jsoup.connect(product.getUrl())
                    .userAgent(DEFAULT_USER_AGENT)
                    .timeout(15000)
                    .followRedirects(true)
                    .get();
            
            log.debug("Successfully fetched HTML document, size: {} bytes", doc.html().length());
            
            Double price = extractPrice(doc, product);
            
            if (price != null && price > 0) {
                // Get old price before updating
                Double oldPrice = product.getCurrentPrice();
                
                // Update product current price
                product.setCurrentPrice(price);
                product.setLastChecked(LocalDateTime.now());
                productRepository.save(product);
                
                // Save price history
                PriceHistory history = new PriceHistory();
                history.setProduct(product);
                history.setPrice(price);
                history.setRecordedAt(LocalDateTime.now());
                priceHistoryRepository.save(history);
                
                log.info("Price updated for {}: ₹{} (Old: ₹{}, Target: ₹{})", 
                        product.getName(), price, oldPrice, product.getTargetPrice());
                
                // Check if price is below target price (always check, regardless of previous state)
                boolean isBelowTarget = price < product.getTargetPrice();
                boolean wasNotified = product.getTargetPriceNotified() != null && product.getTargetPriceNotified();
                
                // Notify if price is below target and we haven't notified yet, OR if price was above target before
                boolean shouldNotifyTarget = isBelowTarget && (!wasNotified || (oldPrice != null && oldPrice > 0 && oldPrice >= product.getTargetPrice()));
                
                if (shouldNotifyTarget) {
                    log.info("Price is below target for {}: ₹{} (Target: ₹{})", 
                            product.getName(), price, product.getTargetPrice());
                    emailNotificationService.sendTargetPriceReachedNotification(product);
                    desktopNotificationService.showTargetPriceReachedNotification(product);
                    product.setTargetPriceNotified(true);
                    productRepository.save(product);
                } else if (!isBelowTarget && wasNotified) {
                    // Price went back above target, reset notification flag
                    product.setTargetPriceNotified(false);
                    productRepository.save(product);
                }
                
                // Check for price drop and send notifications
                if (oldPrice != null && oldPrice > 0 && price < oldPrice) {
                    log.info("Price dropped for {}: ₹{} → ₹{}", product.getName(), oldPrice, price);
                    emailNotificationService.sendPriceDropNotification(product, oldPrice, price);
                    desktopNotificationService.showPriceDropNotification(product, oldPrice, price);
                }
                
                return price;
            } else {
                log.warn("Could not extract valid price for product: {}", product.getName());
                return null;
            }
            
        } catch (IOException e) {
            log.error("Error fetching price for product {}: {}", product.getName(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error fetching price for product {}: {}", product.getName(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Extract price from HTML document with enhanced selectors for Indian e-commerce sites
     */
    private Double extractPrice(Document doc, Product product) {
        log.debug("Extracting price for product: {}", product.getName());
        
        // Try custom selector first if provided
        if (product.getPriceSelector() != null && !product.getPriceSelector().isEmpty()) {
            try {
                log.debug("Trying custom selector: {}", product.getPriceSelector());
                Element priceElement = doc.selectFirst(product.getPriceSelector());
                if (priceElement != null) {
                    String priceText = priceElement.text();
                    log.debug("Found price text with custom selector: {}", priceText);
                    Double price = parsePrice(priceText);
                    if (price != null) {
                        log.info("Successfully extracted price using custom selector: ₹{}", price);
                        return price;
                    }
                }
            } catch (Exception e) {
                log.warn("Error using custom selector: {}", e.getMessage());
            }
        }
        
        // Enhanced selectors for Indian e-commerce sites (Amazon, Flipkart, etc.)
        String[] commonSelectors = {
            // Amazon India
            "#priceblock_dealprice", "#priceblock_ourprice", "#priceblock_saleprice",
            ".a-price-whole", ".a-price .a-offscreen", "[data-asin-price]",
            "span.a-price-whole", "span#priceblock_dealprice",
            
            // Flipkart
            "._30jeq3", "._16Jk6d", ".dyC4hf", "[class*='_30jeq3']",
            "div._30jeq3", "span._30jeq3",
            
            // Generic e-commerce
            "[data-price]", "[itemprop=price]", ".price", "#price", 
            ".product-price", ".current-price", "[class*='price']",
            "span[class*='Price']", "div[class*='price']",
            "[class*='selling-price']", "[class*='offer-price']",
            "[id*='price']", "[id*='Price']",
            
            // More specific selectors
            ".price-current", ".price-now", ".final-price",
            "span.price", "div.price", "p.price",
            "[data-testid*='price']", "[data-testid*='Price']"
        };
        
        for (String selector : commonSelectors) {
            try {
                Element priceElement = doc.selectFirst(selector);
                if (priceElement != null) {
                    // Try content attribute first (for structured data)
                    String priceText = priceElement.attr("content");
                    if (priceText.isEmpty()) {
                        priceText = priceElement.text();
                    }
                    // Also try data attributes
                    if (priceText.isEmpty()) {
                        priceText = priceElement.attr("data-price");
                    }
                    
                    if (!priceText.isEmpty()) {
                        log.debug("Found price text with selector '{}': {}", selector, priceText);
                        Double price = parsePrice(priceText);
                        if (price != null && price > 0) {
                            log.info("Successfully extracted price using selector '{}': ₹{}", selector, price);
                            return price;
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Error with selector '{}': {}", selector, e.getMessage());
                // Continue to next selector
            }
        }
        
        // Try to find price in meta tags
        try {
            Element metaPrice = doc.selectFirst("meta[property='product:price:amount']");
            if (metaPrice != null) {
                String priceText = metaPrice.attr("content");
                Double price = parsePrice(priceText);
                if (price != null && price > 0) {
                    log.info("Found price in meta tag: ₹{}", price);
                    return price;
                }
            }
        } catch (Exception e) {
            log.debug("Error reading meta tag: {}", e.getMessage());
        }
        
        // Fallback: search entire document for price patterns
        log.debug("Trying fallback: searching entire document for price patterns");
        String bodyText = doc.body().text();
        Double price = parsePrice(bodyText);
        if (price != null && price > 0) {
            log.info("Found price using fallback method: ₹{}", price);
            return price;
        }
        
        log.warn("Could not extract price from document. Document preview: {}", 
                 bodyText.length() > 200 ? bodyText.substring(0, 200) : bodyText);
        return null;
    }
    
    /**
     * Parse price from text string - enhanced for Indian currency format
     */
    private Double parsePrice(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        log.debug("Parsing price from text: {}", text);
        
        // First try Indian price pattern (₹, Rs., INR)
        Matcher indianMatcher = INDIAN_PRICE_PATTERN.matcher(text);
        if (indianMatcher.find()) {
            try {
                String priceStr = indianMatcher.group(1).replace(",", "").trim();
                Double price = Double.parseDouble(priceStr);
                if (price > 0) {
                    log.debug("Parsed price using Indian pattern: ₹{}", price);
                    return price;
                }
            } catch (NumberFormatException e) {
                log.debug("Failed to parse Indian price pattern: {}", e.getMessage());
            }
        }
        
        // Remove currency symbols and common text, but keep numbers, dots, and commas
        String cleaned = text.replaceAll("[^\\d.,]", " ");
        
        // Find first number that looks like a price (at least 2 digits)
        Matcher matcher = PRICE_PATTERN.matcher(cleaned);
        Double bestPrice = null;
        double maxPrice = 0;
        
        // Find the largest reasonable price (likely the actual product price)
        while (matcher.find()) {
            try {
                String priceStr = matcher.group(1).replace(",", "").trim();
                Double price = Double.parseDouble(priceStr);
                
                // Filter out unreasonable prices (too small or too large)
                // Typical product prices: 100 to 1,00,00,000 (1 crore)
                if (price >= 10 && price <= 100000000 && price > maxPrice) {
                    maxPrice = price;
                    bestPrice = price;
                }
            } catch (NumberFormatException e) {
                // Continue searching
            }
        }
        
        if (bestPrice != null) {
            log.debug("Parsed price: ₹{}", bestPrice);
            return bestPrice;
        }
        
        log.debug("Could not parse price from text: {}", text);
        return null;
    }
    
    /**
     * Check prices for all active products (scheduled task)
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void checkAllActiveProducts() {
        log.info("Starting scheduled price check for all active products");
        List<Product> activeProducts = productRepository.findByIsActiveTrue();
        
        for (Product product : activeProducts) {
            try {
                fetchPrice(product);
                // Add delay to avoid overwhelming servers
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Price check interrupted");
                break;
            } catch (Exception e) {
                log.error("Error checking price for product {}: {}", product.getName(), e.getMessage());
            }
        }
        
        log.info("Completed price check for {} products", activeProducts.size());
    }
    
    /**
     * Manually trigger price check for a specific product
     */
    public void checkProductPrice(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        fetchPrice(product);
    }
}
