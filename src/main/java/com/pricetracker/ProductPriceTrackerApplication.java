package com.pricetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProductPriceTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductPriceTrackerApplication.class, args);
    }
}


