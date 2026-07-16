package com.spring.store.service;

import com.spring.store.dto.CartItemChangeRequest;
import com.spring.store.dto.CartResponse;
import com.spring.store.dto.CheckoutResponse;
import com.spring.store.entity.CartItem;
import com.spring.store.entity.Product;
import com.spring.store.entity.Session;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@AllArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final SessionService sessionService;
    private final CartItemService cartItemService;
    private final ProductService productService;
    private final CartSummaryService cartSummaryService;
    private final OrderService orderService;

    @Override
    public CartResponse getCart(String sessionId) {
        Session session = sessionService.requireActiveSession(sessionId);
        List<CartItem> items = cartItemService.getItems(session);
        return cartSummaryService.buildResponse(items);
    }

    @Override
    public void addToCart(String sessionId, CartItemChangeRequest request) {
        Session session = sessionService.requireActiveSession(sessionId);
        int quantity = requirePositiveQuantity(request.getQuantity());
        Product product = productService.requireProduct(request.getId());

        CartItem existing = cartItemService.findItem(session, product.getId()).orElse(null);

        int newQuantity = quantity + (existing != null ? existing.getQuantity() : 0);
        productService.requireStock(product, newQuantity, "Insufficient stock");

        CartItem toSave = existing != null ? existing : CartItem.builder()
                .session(session)
                .product(product)
                .build();
        toSave.setQuantity(newQuantity);

        cartItemService.saveItem(toSave);
    }

    @Override
    public void updateItem(String sessionId, CartItemChangeRequest request) {
        Session session = sessionService.requireActiveSession(sessionId);
        int quantity = requirePositiveQuantity(request.getQuantity());
        CartItem existing = cartItemService.requireItem(session, request.getId());

        Product product = productService.requireProduct(existing.getProduct().getId());
        productService.requireStock(product, quantity, "Insufficient stock");

        existing.setQuantity(quantity);
        cartItemService.saveItem(existing);
    }

    @Override
    public void removeItem(String sessionId, Long productId) {
        Session session = sessionService.requireActiveSession(sessionId);
        CartItem existing = cartItemService.requireItem(session, productId);
        cartItemService.deleteItem(existing);
    }

    @Override
    public CheckoutResponse checkout(String sessionId) {
        return orderService.checkout(sessionId);
    }

    private int requirePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than 0");
        }
        return quantity;
    }
}
