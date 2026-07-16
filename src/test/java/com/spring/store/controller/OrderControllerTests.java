package com.spring.store.controller;

import com.spring.store.entity.*;
import com.spring.store.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class OrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private static final String TEST_EMAIL = "orderuser@example.com";
    private static final String OTHER_EMAIL = "other@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String SESSION_HEADER = "X-Session-Id";

    private Person testPerson;
    private String testSessionId;
    private Product drillProduct;

    @BeforeEach
    public void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        sessionRepository.deleteAll();
        personRepository.deleteAll();

        testPerson = Person.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();
        personRepository.save(testPerson);

        testSessionId = UUID.randomUUID().toString();
        Session session = Session.builder()
                .sessionId(testSessionId)
                .person(testPerson)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusHours(1))
                .build();
        sessionRepository.save(session);

        drillProduct = productRepository.findAll().stream()
                .filter(p -> "Drill".equals(p.getTitle()))
                .findFirst()
                .orElseGet(() -> productRepository.save(Product.builder()
                        .title("Drill")
                        .available(5)
                        .price(new BigDecimal("79.99"))
                        .build()));
    }

    @Test
    public void getOrders_whenNoOrdersPlaced_shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/orders")
                        .header(SESSION_HEADER, testSessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    public void getOrders_whenOrdersPlaced_shouldReturnOrdersOrderedByDateDesc() throws Exception {
        Order order = Order.builder()
                .person(testPerson)
                .createdAt(LocalDateTime.now())
                .total(new BigDecimal("79.99"))
                .status("COMPLETED")
                .build();
        orderRepository.save(order);

        mockMvc.perform(get("/orders")
                        .header(SESSION_HEADER, testSessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(order.getId()))
                .andExpect(jsonPath("$[0].total").value("79.99"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    public void cancelOrder_whenOwnerCancels_shouldRestoreStockAndMarkCancelled() throws Exception {
        int initialStock = drillProduct.getAvailable();

        Order order = Order.builder()
                .person(testPerson)
                .createdAt(LocalDateTime.now())
                .total(new BigDecimal("79.99"))
                .status("COMPLETED")
                .build();
        OrderItem item = OrderItem.builder()
                .order(order)
                .product(drillProduct)
                .quantity(2)
                .price(drillProduct.getPrice())
                .build();
        order.setItems(Collections.singletonList(item));
        orderRepository.save(order);

        mockMvc.perform(post("/orders/cancel")
                        .header(SESSION_HEADER, testSessionId)
                        .param("id", order.getId().toString()))
                .andExpect(status().isOk());

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals("CANCELLED", updatedOrder.getStatus());

        Product updatedProduct = productRepository.findById(drillProduct.getId()).orElseThrow();
        assertEquals(initialStock + 2, updatedProduct.getAvailable());
    }

    @Test
    public void cancelOrder_whenNonOwnerCancels_shouldReturnForbidden() throws Exception {
        Person otherPerson = Person.builder()
                .email(OTHER_EMAIL)
                .password(TEST_PASSWORD)
                .build();
        personRepository.save(otherPerson);

        String otherSessionId = UUID.randomUUID().toString();
        Session session = Session.builder()
                .sessionId(otherSessionId)
                .person(otherPerson)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusHours(1))
                .build();
        sessionRepository.save(session);

        Order order = Order.builder()
                .person(testPerson)
                .createdAt(LocalDateTime.now())
                .total(new BigDecimal("79.99"))
                .status("COMPLETED")
                .build();
        orderRepository.save(order);

        mockMvc.perform(post("/orders/cancel")
                        .header(SESSION_HEADER, otherSessionId)
                        .param("id", order.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void cancelOrder_whenAlreadyCancelled_shouldReturnBadRequest() throws Exception {
        Order order = Order.builder()
                .person(testPerson)
                .createdAt(LocalDateTime.now())
                .total(new BigDecimal("79.99"))
                .status("CANCELLED")
                .build();
        orderRepository.save(order);

        mockMvc.perform(post("/orders/cancel")
                        .header(SESSION_HEADER, testSessionId)
                        .param("id", order.getId().toString()))
                .andExpect(status().isBadRequest());
    }
}
