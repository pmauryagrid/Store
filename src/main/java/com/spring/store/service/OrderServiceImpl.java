package com.spring.store.service;

import com.spring.store.dto.CheckoutResponse;
import com.spring.store.dto.OrderResponse;
import com.spring.store.entity.*;
import com.spring.store.repository.OrderRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final SessionService sessionService;
    private final CartItemService cartItemService;
    private final ProductService productService;
    private final OrderRepository orderRepository;

    @Override
    public CheckoutResponse checkout(String sessionId) {
        Session session = sessionService.requireActiveSession(sessionId);
        List<CartItem> cartItems = cartItemService.getItems(session);
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        BigDecimal total = BigDecimal.ZERO;
        List<Product> productsToUpdate = new ArrayList<>();
        List<OrderItem> orderItems = new ArrayList<>();

        Order order = Order.builder()
                .person(session.getPerson())
                .createdAt(LocalDateTime.now())
                .status("COMPLETED")
                .build();

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            productService.requirePrice(product);
            productService.requireStock(product, cartItem.getQuantity(), "Insufficient stock for " + product.getTitle());

            product.setAvailable(product.getAvailable() - cartItem.getQuantity());
            productsToUpdate.add(product);

            BigDecimal itemPrice = product.getPrice();
            BigDecimal lineTotal = itemPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            total = total.add(lineTotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(itemPrice)
                    .build();
            orderItems.add(orderItem);
        }

        order.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        order.setItems(orderItems);

        orderRepository.save(order);
        productService.saveAll(productsToUpdate);
        cartItemService.deleteItems(cartItems);

        return new CheckoutResponse("Order placed successfully");
    }

    @Override
    public List<OrderResponse> getOrders(String sessionId) {
        Session session = sessionService.requireActiveSession(sessionId);
        Person person = session.getPerson();
        if (person == null) {
            return new ArrayList<>();
        }

        List<Order> orders = orderRepository.findByPersonOrderByCreatedAtDesc(person);
        List<OrderResponse> responses = new ArrayList<>();

        for (Order order : orders) {
            responses.add(OrderResponse.builder()
                    .id(order.getId())
                    .date(order.getCreatedAt().toString())
                    .total(order.getTotal().toPlainString())
                    .status(order.getStatus())
                    .build());
        }

        return responses;
    }

    @Override
    public void cancelOrder(String sessionId, Long orderId) {
        Session session = sessionService.requireActiveSession(sessionId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getPerson() == null || !order.getPerson().getId().equals(session.getPerson().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        if ("CANCELLED".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is already cancelled");
        }

        List<Product> productsToUpdate = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setAvailable(product.getAvailable() + item.getQuantity());
            productsToUpdate.add(product);
        }

        order.setStatus("CANCELLED");

        orderRepository.save(order);
        productService.saveAll(productsToUpdate);
    }
}
