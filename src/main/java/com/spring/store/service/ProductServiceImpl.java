package com.spring.store.service;

import com.spring.store.entity.Product;
import com.spring.store.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@AllArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product requireProduct(Long productId) {
        if (productId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    @Override
    public void requirePrice(Product product) {
        if (product == null || product.getPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing price for product");
        }
    }

    @Override
    public void requireStock(Product product, int quantity, String errorMessage) {
        if (product == null || quantity > product.getAvailable()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }
    }

    @Override
    public void saveAll(List<Product> products) {
        productRepository.saveAll(products);
    }
}
