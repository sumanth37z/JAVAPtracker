// Show add product modal
function showAddProductModal() {
    document.getElementById('addProductModal').style.display = 'block';
}

// Close add product modal
function closeAddProductModal() {
    document.getElementById('addProductModal').style.display = 'none';
    document.getElementById('addProductForm').reset();
}

// Close modal when clicking outside
window.onclick = function(event) {
    const modal = document.getElementById('addProductModal');
    if (event.target == modal) {
        closeAddProductModal();
    }
}

// Add new product
function addProduct(event) {
    event.preventDefault();
    
    const formData = {
        name: document.getElementById('productName').value,
        url: document.getElementById('productUrl').value,
        description: document.getElementById('productDescription').value,
        targetPrice: parseFloat(document.getElementById('targetPrice').value),
        priceSelector: document.getElementById('priceSelector').value || null,
        notificationEmail: document.getElementById('notificationEmail').value || null,
        currentPrice: 0.0,
        isActive: true
    };
    
    fetch('/api/products', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(formData)
    })
    .then(response => {
        if (response.ok) {
            return response.json();
        }
        throw new Error('Failed to add product');
    })
    .then(data => {
        alert('Product added successfully! Price check initiated...');
        closeAddProductModal();
        location.reload();
    })
    .catch(error => {
        alert('Error adding product: ' + error.message);
    });
}

// Check price for a product
function checkPrice(productId) {
    // Get current product data before checking
    fetch(`/api/products/${productId}`)
        .then(response => response.json())
        .then(product => {
            const oldPrice = product.currentPrice;
            
            // Check price
            return fetch(`/api/products/${productId}/check`, {
                method: 'POST'
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Failed to check price');
                }
                return response.json();
            })
            .then(updatedProduct => {
                // Show desktop notification if price dropped or reached target
                if (oldPrice && oldPrice > 0 && updatedProduct.currentPrice > 0) {
                    if (updatedProduct.currentPrice < oldPrice) {
                        // Price dropped
                        if (typeof notificationService !== 'undefined') {
                            notificationService.showPriceDrop(
                                updatedProduct.name,
                                oldPrice,
                                updatedProduct.currentPrice,
                                updatedProduct.targetPrice,
                                updatedProduct.url
                            );
                        }
                    }
                    
                    // Check if target reached
                    if (updatedProduct.currentPrice <= updatedProduct.targetPrice && 
                        oldPrice > updatedProduct.targetPrice) {
                        if (typeof notificationService !== 'undefined') {
                            notificationService.showTargetReached(
                                updatedProduct.name,
                                updatedProduct.currentPrice,
                                updatedProduct.targetPrice,
                                updatedProduct.url
                            );
                        }
                    }
                }
                
                alert('Price checked successfully! Current price: ₹' + updatedProduct.currentPrice.toFixed(2));
                location.reload();
            });
        })
        .catch(error => {
            console.error('Error checking price:', error);
            alert('Error checking price: ' + error);
        });
}

// Delete product by button element
function deleteProductById(button) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }
    
    const productId = button.getAttribute('data-product-id');
    console.log('Delete button clicked, product ID:', productId);
    
    if (!productId) {
        alert('Error: Product ID not found');
        console.error('Product ID not found in data-product-id attribute');
        return;
    }
    
    deleteProduct(productId);
}

// Delete product by ID
function deleteProduct(productId) {
    if (!productId) {
        alert('Error: Product ID is required');
        return;
    }
    
    if (confirm('Are you sure you want to delete this product?')) {
        console.log('Deleting product with ID:', productId);
        
        fetch(`/api/products/${productId}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json'
            }
        })
        .then(response => {
            console.log('Delete response status:', response.status, response.statusText);
            
            if (response.ok) {
                return response.text().then(text => {
                    console.log('Delete successful:', text);
                    alert('Product deleted successfully');
                    location.reload();
                });
            } else {
                return response.text().then(text => {
                    console.error('Delete failed with status', response.status, ':', text);
                    throw new Error(text || 'Failed to delete product (Status: ' + response.status + ')');
                });
            }
        })
        .catch(error => {
            console.error('Delete error:', error);
            alert('Error deleting product: ' + (error.message || error));
        });
    }
}

// View product details
function viewProduct(productId) {
    window.location.href = `/products/${productId}`;
}

