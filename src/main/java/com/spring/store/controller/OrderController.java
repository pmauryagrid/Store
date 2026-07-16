package com.spring.store.controller;

import com.spring.store.dto.OrderResponse;
import com.spring.store.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(@RequestHeader("X-Session-Id") String sessionId) {
        return ResponseEntity.ok(orderService.getOrders(sessionId));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelOrder(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestParam("id") Long orderId
    ) {
        orderService.cancelOrder(sessionId, orderId);
        return ResponseEntity.ok().build();
    }
}
