package com.spring.store.controller;

import com.spring.store.dto.CartItemChangeRequest;
import com.spring.store.dto.CartResponse;
import com.spring.store.dto.CheckoutResponse;
import com.spring.store.service.CartService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@RequestHeader("X-Session-Id") String sessionId) {
        return ResponseEntity.ok(cartService.getCart(sessionId));
    }

    @PostMapping("/add")
    public ResponseEntity<Void> addToCart(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestBody CartItemChangeRequest request
    ) {
        cartService.addToCart(sessionId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/item")
    public ResponseEntity<Void> updateCartItem(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestBody CartItemChangeRequest request
    ) {
        cartService.updateItem(sessionId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/item")
    public ResponseEntity<Void> removeCartItem(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestParam("id") Long productId
    ) {
        cartService.removeItem(sessionId, productId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@RequestHeader("X-Session-Id") String sessionId) {
        return ResponseEntity.ok(cartService.checkout(sessionId));
    }
}
