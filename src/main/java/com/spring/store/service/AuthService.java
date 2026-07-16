package com.spring.store.service;

import com.spring.store.dto.ResetPasswordDto;
import com.spring.store.dto.UserRegisterAndLoginRequest;

public interface AuthService {
    void registerUser(UserRegisterAndLoginRequest userRegisterRequest);
    String loginUser(UserRegisterAndLoginRequest request);
    void resetPassword(ResetPasswordDto resetPasswordDto);
}
