package com.neighborhood.eventmanagement.service.impl;

import com.neighborhood.eventmanagement.dto.*;
import com.neighborhood.eventmanagement.entity.*;
import com.neighborhood.eventmanagement.exception.ResourceNotFoundException;
import com.neighborhood.eventmanagement.exception.UnauthorizedAccessException;
import com.neighborhood.eventmanagement.exception.ValidationException;
import com.neighborhood.eventmanagement.repository.*;
import com.neighborhood.eventmanagement.security.jwt.JwtTokenProvider;
import com.neighborhood.eventmanagement.security.service.CustomUserDetailsService;
import com.neighborhood.eventmanagement.service.AuthService;
import com.neighborhood.eventmanagement.service.EmailService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final ZoneRepository zoneRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthServiceImpl(
            UserRepository userRepository,
            ZoneRepository zoneRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            EmailService emailService,
            CustomUserDetailsService customUserDetailsService) {

        this.userRepository = userRepository;
        this.zoneRepository = zoneRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already registered.");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setEnabled(false);
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(LocalDateTime.now());

        // Zone assignment — optional for GUEST
        if (request.getZoneId() != null) {
            Zone zone = zoneRepository.findById(request.getZoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + request.getZoneId()));
            user.setZone(zone);
        }

        userRepository.save(user);

        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(user);
        verificationToken.setToken(UUID.randomUUID().toString());
        verificationToken.setExpiryDate(LocalDateTime.now().plusDays(1));
        emailVerificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), verificationToken.getToken());

        return new RegisterResponse(
                "Registration successful. Please verify your email before logging in."
        );
    }

    @Override
    public JwtAuthenticationResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new ValidationException("Please verify your email before logging in.");
        }

        if (user.isAccountLocked()) {
            if (user.getLockTime() != null &&
                    user.getLockTime().plusMinutes(15).isBefore(LocalDateTime.now())) {
                // Auto-unlock after 15 minutes
                user.setAccountLocked(false);
                user.setFailedLoginAttempts(0);
                user.setLockTime(null);
                userRepository.save(user);
            } else {
                throw new LockedException("Account is locked. Try again after 15 minutes.");
            }
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            user.setFailedLoginAttempts(0);
            user.setLockTime(null);
            userRepository.save(user);

            String accessToken = jwtTokenProvider.generateToken(authentication);

            Optional<RefreshToken> existing = refreshTokenRepository.findByUser(user);
            RefreshToken refreshToken = existing.orElseGet(RefreshToken::new);
            refreshToken.setUser(user);
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
            refreshTokenRepository.save(refreshToken);

            return new JwtAuthenticationResponse(accessToken, refreshToken.getToken());

        } catch (BadCredentialsException ex) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= 5) {
                user.setAccountLocked(true);
                user.setLockTime(LocalDateTime.now());
            }

            userRepository.save(user);
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    @Override
    public JwtAuthenticationResponse refreshToken(RefreshTokenRequest request) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new UnauthorizedAccessException("Refresh token expired. Please log in again.");
        }

        UserDetails userDetails = customUserDetailsService
                .loadUserByUsername(refreshToken.getUser().getEmail());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        String accessToken = jwtTokenProvider.generateToken(authentication);

        return new JwtAuthenticationResponse(accessToken, refreshToken.getToken());
    }

    @Override
    public String logout(RefreshTokenRequest request) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));

        refreshTokenRepository.delete(refreshToken);
        return "Logout successful";
    }

    @Override
    public String forgotPassword(ForgotPasswordRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found for that email."));

        passwordResetTokenRepository.findByUser(user).ifPresent(existing -> {
            passwordResetTokenRepository.delete(existing);
            passwordResetTokenRepository.flush();
        });

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(LocalDateTime.now().plusHours(1));
        passwordResetTokenRepository.save(token);

        emailService.sendPasswordResetEmail(user.getEmail(), token.getToken());

        return "Password reset email sent.";
    }

    @Override
    public String resetPassword(ResetPasswordRequest request) {

        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired reset token."));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(token);
            throw new UnauthorizedAccessException("Reset token has expired.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        passwordResetTokenRepository.delete(token);
        return "Password updated successfully.";
    }

    @Override
    public String verifyEmail(String tokenValue) {

        EmailVerificationToken token = emailVerificationTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid verification token."));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            emailVerificationTokenRepository.delete(token);
            throw new UnauthorizedAccessException("Verification token has expired.");
        }

        User user = token.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        emailVerificationTokenRepository.delete(token);
        return "Email verified successfully. You can now log in.";
    }
}
