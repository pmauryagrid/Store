package com.spring.store.controller;

import tools.jackson.databind.ObjectMapper;
import com.spring.store.dto.CartItemChangeRequest;
import com.spring.store.entity.CartItem;
import com.spring.store.entity.Person;
import com.spring.store.entity.Product;
import com.spring.store.entity.Session;
import com.spring.store.repository.CartItemRepository;
import com.spring.store.repository.PersonRepository;
import com.spring.store.repository.ProductRepository;
import com.spring.store.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CartControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    private static final String TEST_EMAIL = "cartuser@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String SESSION_HEADER = "X-Session-Id";

    private String sessionId;
    private Product hammerProduct;

    @BeforeEach
    public void setUp() {
        cartItemRepository.deleteAll();
        sessionRepository.deleteAll();
        personRepository.deleteAll();

        Person person = Person.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();
        personRepository.save(person);

        sessionId = UUID.randomUUID().toString();
        Session session = Session.builder()
                .sessionId(sessionId)
                .person(person)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusHours(1))
                .build();
        sessionRepository.save(session);

        hammerProduct = productRepository.findAll().stream()
                .filter(p -> "Hammer".equals(p.getTitle()))
                .findFirst()
                .orElseGet(() -> productRepository.save(Product.builder()
                        .title("Hammer")
                        .available(15)
                        .price(new BigDecimal("9.50"))
                        .build()));
    }

    @Test
    public void getCart_whenEmpty_shouldReturnZeroSubtotal() throws Exception {
        mockMvc.perform(get("/cart")
                        .header(SESSION_HEADER, sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.subtotal").value("0.00"));
    }

    @Test
    public void addToCart_whenQuantityValid_shouldSaveAndIncreaseSubtotal() throws Exception {
        CartItemChangeRequest request = new CartItemChangeRequest(hammerProduct.getId(), 2);

        mockMvc.perform(post("/cart/add")
                        .header(SESSION_HEADER, sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cart")
                        .header(SESSION_HEADER, sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].title").value("Hammer"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].ordinal").value(1))
                .andExpect(jsonPath("$.subtotal").value("19.00"));
    }

    @Test
    public void addToCart_whenQuantityExceedsStock_shouldReturnBadRequest() throws Exception {
        CartItemChangeRequest request = new CartItemChangeRequest(hammerProduct.getId(), 100);

        mockMvc.perform(post("/cart/add")
                        .header(SESSION_HEADER, sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateCartItem_whenQuantityValid_shouldModifyQuantityAndSubtotal() throws Exception {
        Session session = sessionRepository.findById(sessionId).orElseThrow();
        CartItem cartItem = CartItem.builder()
                .session(session)
                .product(hammerProduct)
                .quantity(2)
                .build();
        cartItemRepository.save(cartItem);

        CartItemChangeRequest request = new CartItemChangeRequest(hammerProduct.getId(), 5);

        mockMvc.perform(put("/cart/item")
                        .header(SESSION_HEADER, sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cart")
                        .header(SESSION_HEADER, sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.subtotal").value("47.50"));
    }

    @Test
    public void removeCartItem_whenItemExists_shouldRemoveAndRecalculate() throws Exception {
        Session session = sessionRepository.findById(sessionId).orElseThrow();
        CartItem cartItem = CartItem.builder()
                .session(session)
                .product(hammerProduct)
                .quantity(2)
                .build();
        cartItemRepository.save(cartItem);

        mockMvc.perform(delete("/cart/item")
                        .header(SESSION_HEADER, sessionId)
                        .param("id", hammerProduct.getId().toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cart")
                        .header(SESSION_HEADER, sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.subtotal").value("0.00"));
    }

    @Test
    public void checkout_whenCartNotEmpty_shouldDecreaseStockAndClearCart() throws Exception {
        Session session = sessionRepository.findById(sessionId).orElseThrow();
        CartItem cartItem = CartItem.builder()
                .session(session)
                .product(hammerProduct)
                .quantity(3)
                .build();
        cartItemRepository.save(cartItem);

        int stockBefore = hammerProduct.getAvailable();

        mockMvc.perform(post("/cart/checkout")
                        .header(SESSION_HEADER, sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order placed successfully"));

        Product updatedProduct = productRepository.findById(hammerProduct.getId()).orElseThrow();
        assertEquals(stockBefore - 3, updatedProduct.getAvailable());

        assertTrue(cartItemRepository.findBySessionOrderByIdAsc(session).isEmpty());
    }

    @Test
    public void checkout_whenCartIsEmpty_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/cart/checkout")
                        .header(SESSION_HEADER, sessionId))
                .andExpect(status().isBadRequest());
    }
}
