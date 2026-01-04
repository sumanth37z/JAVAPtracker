// Desktop Notification Service
class NotificationService {
    constructor() {
        this.permission = null;
        this.checkPermission();
    }
    
    checkPermission() {
        if (!("Notification" in window)) {
            console.warn("This browser does not support desktop notifications");
            return false;
        }
        
        this.permission = Notification.permission;
        
        if (this.permission === "default") {
            // Request permission when page loads
            this.requestPermission();
        }
        
        return this.permission === "granted";
    }
    
    async requestPermission() {
        if (!("Notification" in window)) {
            return false;
        }
        
        try {
            const permission = await Notification.requestPermission();
            this.permission = permission;
            
            if (permission === "granted") {
                console.log("Desktop notifications enabled");
                // Show welcome notification
                this.show("Price Tracker", "Desktop notifications enabled! You'll be notified when prices drop.");
                return true;
            } else {
                console.log("Desktop notifications denied");
                return false;
            }
        } catch (error) {
            console.error("Error requesting notification permission:", error);
            return false;
        }
    }
    
    show(title, message, options = {}) {
        if (!this.checkPermission()) {
            console.warn("Notification permission not granted");
            return;
        }
        
        const notificationOptions = {
            body: message,
            icon: "/favicon.ico", // You can add a custom icon
            badge: "/favicon.ico",
            tag: "price-tracker", // Prevents duplicate notifications
            requireInteraction: false,
            ...options
        };
        
        try {
            const notification = new Notification(title, notificationOptions);
            
            // Auto-close after 5 seconds
            setTimeout(() => {
                notification.close();
            }, 5000);
            
            // Handle click - focus window
            notification.onclick = function() {
                window.focus();
                this.close();
            };
            
            return notification;
        } catch (error) {
            console.error("Error showing notification:", error);
        }
    }
    
    showPriceDrop(productName, oldPrice, newPrice, targetPrice, productUrl) {
        const savings = oldPrice - newPrice;
        const percentage = ((savings / oldPrice) * 100).toFixed(1);
        const isAtTarget = newPrice <= targetPrice;
        
        const title = isAtTarget 
            ? `🎯 Target Reached: ${productName}`
            : `💰 Price Drop: ${productName}`;
        
        const message = isAtTarget
            ? `Price reached your target! Current: ₹${newPrice.toFixed(2)} (Target: ₹${targetPrice.toFixed(2)})`
            : `Price dropped! Was ₹${oldPrice.toFixed(2)}, now ₹${newPrice.toFixed(2)} (Save ₹${savings.toFixed(2)} - ${percentage}%)`;
        
        const notification = this.show(title, message, {
            body: message + `\nClick to view product`,
            data: { url: productUrl },
            requireInteraction: isAtTarget // Keep target notifications visible longer
        });
        
        if (notification) {
            notification.onclick = function() {
                if (productUrl) {
                    window.open(productUrl, '_blank');
                }
                window.focus();
                this.close();
            };
        }
    }
    
    showTargetReached(productName, currentPrice, targetPrice, productUrl) {
        const title = `🎯 Target Price Reached: ${productName}`;
        const message = `Price is now ₹${currentPrice.toFixed(2)} (Your target: ₹${targetPrice.toFixed(2)})`;
        
        const notification = this.show(title, message, {
            body: message + `\nClick to view product`,
            data: { url: productUrl },
            requireInteraction: true // Keep visible until user interacts
        });
        
        if (notification) {
            notification.onclick = function() {
                if (productUrl) {
                    window.open(productUrl, '_blank');
                }
                window.focus();
                this.close();
            };
        }
    }
}

// Global notification service instance
const notificationService = new NotificationService();

// Request permission on page load
document.addEventListener('DOMContentLoaded', function() {
    // Small delay to let page load
    setTimeout(() => {
        if (Notification.permission === "default") {
            // Show a subtle prompt (optional)
            console.log("Requesting notification permission...");
        }
    }, 1000);
});

