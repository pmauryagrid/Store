package com.spring.store.config;

import com.spring.store.entity.Product;
import com.spring.store.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class ProductSeed {

    @Bean
    public CommandLineRunner seedProducts(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() == 0) {
                List<Product> products = List.of(
                        Product.builder().title("Nail gun").available(8).price(new BigDecimal("23.95")).build(),
                        Product.builder().title("Hammer").available(15).price(new BigDecimal("9.50")).build(),
                        Product.builder().title("Drill").available(5).price(new BigDecimal("79.99")).build()
                );
                productRepository.saveAll(products);
            }
        };
    }
}
