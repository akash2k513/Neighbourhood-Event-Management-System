package com.neighborhood.eventmanagement.service;

import com.neighborhood.eventmanagement.dto.*;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    JwtAuthenticationResponse login(LoginRequest request);

    JwtAuthenticationResponse refreshToken(RefreshTokenRequest request);

    String logout(RefreshTokenRequest request);

    String forgotPassword(ForgotPasswordRequest request);

    String resetPassword(ResetPasswordRequest request);

    String verifyEmail(String token);
}