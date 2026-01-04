# Product Price Tracker

A web-based Java application for tracking product prices in real-time. Built with Spring Boot, this application allows you to monitor prices of products from various online stores and get notified when prices drop.

## Features

- ✅ Add products to track by URL
- ✅ Automatic price extraction from product pages
- ✅ Price history tracking with visual charts
- ✅ Scheduled price checks (every hour)
- ✅ Manual price check on demand
- ✅ Target price setting
- ✅ Beautiful, responsive web interface
- ✅ Price change tracking and visualization

## Technology Stack

- **Backend**: Spring Boot 3.2.0
- **Database**: H2 (in-memory/file-based)
- **Frontend**: HTML, CSS, JavaScript, Thymeleaf
- **Web Scraping**: Jsoup
- **Charts**: Chart.js

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

## Installation & Running

1. **Clone or download this project**

2. **Build the project**:
   ```bash
   mvn clean install
   ```

3. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application**:
   Open your browser and navigate to: `http://localhost:8080`

## Usage

### Adding a Product

1. Click the "+ Add Product" button
2. Fill in the product details:
   - **Product Name**: A descriptive name for the product
   - **Product URL**: The full URL of the product page
   - **Description**: Optional description
   - **Target Price**: Your desired price (in USD)
   - **Price CSS Selector**: Optional - if automatic detection fails, provide a CSS selector for the price element (e.g., `.price`, `#price`, `[data-price]`)

3. Click "Add Product"
4. The system will automatically fetch the current price

### Viewing Product Details

- Click on any product card to view detailed information
- See price history chart and table
- Check price manually using the "Check Price Now" button

### Price Tracking

- Prices are automatically checked every hour for all active products
- You can manually trigger a price check at any time
- Price history is stored and displayed in charts and tables

## Database

The application uses H2 database which stores data in a file (`./data/pricetracker.mv.db`). 

To access the H2 console:
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/pricetracker`
- Username: `sa`
- Password: (leave empty)

## API Endpoints

- `GET /` - Main page with all products
- `GET /products/{id}` - Product detail page
- `GET /api/products` - Get all products (JSON)
- `POST /api/products` - Create new product (JSON)
- `GET /api/products/{id}` - Get product by ID (JSON)
- `PUT /api/products/{id}` - Update product (JSON)
- `DELETE /api/products/{id}` - Delete product
- `POST /api/products/{id}/check` - Manually check price
- `GET /api/products/{id}/history` - Get price history (JSON)

## Price Extraction

The application uses intelligent price extraction:

1. First tries custom CSS selector if provided
2. Then tries common price selectors (`.price`, `#price`, `[data-price]`, etc.)
3. Falls back to pattern matching in the entire page

If automatic extraction fails, you can provide a custom CSS selector when adding the product.

## Configuration

Edit `src/main/resources/application.properties` to customize:
- Server port (default: 8080)
- Database settings
- Price check frequency (in `PriceTrackingService.java`)

## Troubleshooting

### Price Not Extracted

If the price is not being extracted correctly:
1. Inspect the product page HTML
2. Find the CSS selector for the price element
3. Add it as the "Price CSS Selector" when adding/editing the product

### Connection Errors

Some websites may block automated requests. The application includes:
- User-Agent headers to mimic browser requests
- Timeout settings
- Error handling

## License

This project is open source and available for personal and commercial use.

## Contributing

Feel free to submit issues and enhancement requests!


