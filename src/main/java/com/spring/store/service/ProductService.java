package com.spring.store.service;

import com.spring.store.entity.Product;

import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();
    Product requireProduct(Long productId);
    void requirePrice(Product product);
    void requireStock(Product product, int quantity, String errorMessage);
    void saveAll(List<Product> products);
}
