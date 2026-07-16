package com.spring.store.controller;

import com.spring.store.dto.LoginResponse;
import com.spring.store.dto.ResetPasswordDto;
import com.spring.store.dto.UserRegisterAndLoginRequest;
import com.spring.store.service.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Void> registerUser(@RequestBody UserRegisterAndLoginRequest userRegisterRequest){
        authService.registerUser(userRegisterRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@RequestBody UserRegisterAndLoginRequest userLoginRequest) {
        String sessionId = authService.loginUser(userLoginRequest);
        return ResponseEntity.ok(new LoginResponse(sessionId));
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordDto resetPasswordDto) {
        authService.resetPassword(resetPasswordDto);
        return ResponseEntity.ok().build();
    }

}
