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
        send(to, "Verify Your Email",
                "Click the link to verify your account:\n\n"
                + "http://localhost:8080/api/auth/verify-email/" + token);
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        send(to, "Reset Your Password",
                "Click the link to reset your password:\n\n"
                + "http://localhost:8080/api/auth/reset-password?token=" + token);
    }

    @Override
    public void sendEventApprovalEmail(String to, String eventTitle, boolean approved) {
        String status = approved ? "Approved" : "Rejected";
        send(to, "Event " + status + ": " + eventTitle,
                "Your event \"" + eventTitle + "\" has been " + status.toLowerCase() + ".");
    }

    @Override
    public void sendRegistrationConfirmationEmail(String to, String eventTitle) {
        send(to, "Registration Confirmed: " + eventTitle,
                "You have successfully registered for \"" + eventTitle + "\".");
    }

    @Override
    public void sendEventCancellationEmail(String to, String eventTitle) {
        send(to, "Event Cancelled: " + eventTitle,
                "The event \"" + eventTitle + "\" has been cancelled.");
    }

    @Override
    public void sendGenericNotification(String to, String subject, String body) {
        send(to, subject, body);
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
