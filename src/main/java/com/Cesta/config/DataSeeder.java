
package com.Cesta.config;

import com.Cesta.model.Product;
import com.Cesta.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedProducts(ProductRepository productRepository) {
        return args -> {

            if (productRepository.count() == 0) {

                List<Product> products = List.of(
                    createProduct("Fresh Avocado", "Fruits",
                            "Creamy, fresh avocado", "350.00", "400.00", "🥑", 25),

                    createProduct("Red Apples", "Fruits",
                            "Crisp and juicy red apples", "384.00", "480.00", "🍎", 40),

                    createProduct("Fresh Bananas", "Fruits",
                            "Sweet, ripe bananas", "220.00", null, "🍌", 50),

                    createProduct("Fresh Milk", "Dairy",
                            "Fresh everyday milk", "255.00", "300.00", "🥛", 30),

                    createProduct("Fresh Bread", "Bakery",
                            "Soft and freshly baked bread", "300.00", "400.00", "🍞", 20),

                    createProduct("Fresh Carrots", "Vegetables",
                            "Crunchy, fresh carrots", "180.00", null, "🥕", 35),

                    createProduct("Fresh Tomatoes", "Vegetables",
                            "Ripe tomatoes for everyday cooking", "250.00", null, "🍅", 30),

                    createProduct("Fresh Eggs", "Dairy",
                            "Farm-fresh eggs", "600.00", null, "🥚", 20)
                );

                productRepository.saveAll(products);

                System.out.println("Cesta sample products added successfully!");
            }
        };
    }

    private static Product createProduct(
            String name,
            String category,
            String description,
            String price,
            String originalPrice,
            String imageEmoji,
            int stockQuantity) {

        Product product = new Product();

        product.setName(name);
        product.setCategory(category);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));

        if (originalPrice != null) {
            product.setOriginalPrice(new BigDecimal(originalPrice));
        }

        product.setImageEmoji(imageEmoji);
        product.setStockQuantity(stockQuantity);
        product.setActive(true);

        return product;
    }
}
