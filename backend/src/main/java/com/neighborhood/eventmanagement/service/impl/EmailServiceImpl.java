package com.neighborhood.eventmanagement.service.impl;

import com.neighborhood.eventmanagement.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendVerificationEmail(String to, String token) {
        send(to, "Verify Your Email — NeighborHub",
                "Hello,\n\nClick the link below to verify your account:\n\n"
                + "http://localhost:8080/api/auth/verify-email/" + token
                + "\n\nThis link expires in 24 hours.\n\nNeighborHub Team");
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        send(to, "Reset Your Password — NeighborHub",
                "Hello,\n\nClick the link below to reset your password:\n\n"
                + "http://localhost:8080/api/auth/reset-password?token=" + token
                + "\n\nThis link expires in 1 hour.\n\nNeighborHub Team");
    }

    @Override
    public void sendEventApprovalEmail(String to, String eventTitle, boolean approved) {
        String status = approved ? "Approved ✓" : "Rejected ✗";
        send(to, "Event " + status + ": " + eventTitle,
                "Hello,\n\nYour event \"" + eventTitle + "\" has been "
                + (approved ? "approved and is now visible to residents." : "rejected by the reviewer.")
                + "\n\nNeighborHub Team");
    }

    @Override
    public void sendRegistrationConfirmationEmail(String to, String eventTitle) {
        send(to, "Registration Confirmed: " + eventTitle,
                "Hello,\n\nYou have successfully registered for \"" + eventTitle + "\".\n\n"
                + "We look forward to seeing you there!\n\nNeighborHub Team");
    }

    @Override
    public void sendEventCancellationEmail(String to, String eventTitle) {
        send(to, "Event Cancelled: " + eventTitle,
                "Hello,\n\nWe regret to inform you that the event \"" + eventTitle
                + "\" has been cancelled.\n\nNeighborHub Team");
    }

    @Override
    public void sendGenericNotification(String to, String subject, String body) {
        send(to, subject, body);
    }

    @Override
    public void sendAnnouncementEmail(String to, String zoneName, String title, String message) {
        send(to, "[" + zoneName + "] " + title,
                "Community Announcement for " + zoneName + "\n\n"
                + title + "\n\n" + message + "\n\nNeighborHub Team");
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        mailSender.send(msg);
    }
}
