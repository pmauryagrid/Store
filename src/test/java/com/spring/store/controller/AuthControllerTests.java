package com.spring.store.controller;

import tools.jackson.databind.ObjectMapper;
import com.spring.store.dto.ResetPasswordDto;
import com.spring.store.dto.UserRegisterAndLoginRequest;
import com.spring.store.entity.Person;
import com.spring.store.repository.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_EMAIL = "user@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String NEW_PASSWORD = "newPassword123";

    @BeforeEach
    public void setUp() {
        personRepository.deleteAll();
    }

    @Test
    public void registerUser_whenUserDoesNotExist_shouldReturnOk() throws Exception {
        UserRegisterAndLoginRequest request = new UserRegisterAndLoginRequest(TEST_EMAIL, TEST_PASSWORD);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertTrue(personRepository.existsByEmail(TEST_EMAIL));
    }

    @Test
    public void registerUser_whenUserExists_shouldReturnConflict() throws Exception {
        Person person = Person.builder()
                .email(TEST_EMAIL)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .build();
        personRepository.save(person);

        UserRegisterAndLoginRequest request = new UserRegisterAndLoginRequest(TEST_EMAIL, TEST_PASSWORD);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User already exists"))
                .andExpect(jsonPath("$.path").value("/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    public void loginUser_whenCredentialsValid_shouldReturnSessionId() throws Exception {
        Person person = Person.builder()
                .email(TEST_EMAIL)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .build();
        personRepository.save(person);

        UserRegisterAndLoginRequest request = new UserRegisterAndLoginRequest(TEST_EMAIL, TEST_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").exists());
    }

    @Test
    public void loginUser_whenPasswordInvalid_shouldReturnUnauthorized() throws Exception {
        Person person = Person.builder()
                .email(TEST_EMAIL)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .build();
        personRepository.save(person);

        UserRegisterAndLoginRequest request = new UserRegisterAndLoginRequest(TEST_EMAIL, "wrongpassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void resetPassword_whenCredentialsValid_shouldReturnOkAndInvalidateSessions() throws Exception {
        Person person = Person.builder()
                .email(TEST_EMAIL)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .build();
        personRepository.save(person);

        ResetPasswordDto request = new ResetPasswordDto();
        request.setEmail(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);
        request.setNewPassword(NEW_PASSWORD);

        mockMvc.perform(post("/auth/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Person updated = personRepository.findByEmail(TEST_EMAIL).orElse(null);
        assertNotNull(updated);
        assertTrue(passwordEncoder.matches(NEW_PASSWORD, updated.getPassword()));
    }

    @Test
    public void loginUser_whenPasswordFailsFiveTimes_shouldLockAccount() throws Exception {
        Person person = Person.builder()
                .email(TEST_EMAIL)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .failedAttempt(0)
                .locked(false)
                .build();
        personRepository.save(person);

        UserRegisterAndLoginRequest wrongRequest = new UserRegisterAndLoginRequest(TEST_EMAIL, "wrongpassword");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongRequest)))
                    .andExpect(status().isUnauthorized());
        }

        Person updated = personRepository.findByEmail(TEST_EMAIL).orElse(null);
        assertNotNull(updated);
        assertTrue(updated.isLocked());
        assertEquals(5, updated.getFailedAttempt());
    }
}
