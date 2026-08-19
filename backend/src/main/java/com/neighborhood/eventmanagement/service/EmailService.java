package com.neighborhood.eventmanagement.service;

public interface EmailService {

    void sendVerificationEmail(String to, String token);

    void sendPasswordResetEmail(String to, String token);

    void sendEventApprovalEmail(String to, String eventTitle, boolean approved);

    void sendRegistrationConfirmationEmail(String to, String eventTitle);

    void sendEventCancellationEmail(String to, String eventTitle);

    void sendGenericNotification(String to, String subject, String body);
}
