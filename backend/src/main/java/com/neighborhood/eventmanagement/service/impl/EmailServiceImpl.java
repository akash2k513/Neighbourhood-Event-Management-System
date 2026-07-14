package com.neighborhood.eventmanagement.service.impl;

import com.neighborhood.eventmanagement.service.EmailService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendVerificationEmail(String to, String token) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Verify Your Email");

        message.setText(
                "Click the link below to verify your account:\n\n"
                        + "http://localhost:8080/api/auth/verify-email/"
                        + token
        );

        mailSender.send(message);
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Reset Your Password");

        message.setText(
                "Click the link below to reset your password:\n\n"
                        + "http://localhost:8080/api/auth/reset-password?token="
                        + token
        );

        mailSender.send(message);
    }
}