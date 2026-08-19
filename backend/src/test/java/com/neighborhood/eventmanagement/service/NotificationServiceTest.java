package com.neighborhood.eventmanagement.service;

import com.neighborhood.eventmanagement.entity.*;
import com.neighborhood.eventmanagement.repository.NotificationRepository;
import com.neighborhood.eventmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    private NotificationService notificationService;

    private User user;
    private Zone zone;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        notificationService = new NotificationService(notificationRepository, userRepository, emailService);

        zone = new Zone();
        zone.setName("North Zone");

        user = new User();
        user.setEmail("user@test.com");
        user.setFullName("Test User");
        user.setZone(zone);

        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── notifyUser ────────────────────────────────────────────────────

    @Test
    @DisplayName("notifyUser saves notification with correct priority")
    void notify_user_saves_notification() {
        notificationService.notifyUser(user, "Test Title", "Test message",
                Notification.Priority.HIGH, false);

        verify(notificationRepository).save(argThat(n ->
                n.getTitle().equals("Test Title") &&
                n.getPriority() == Notification.Priority.HIGH &&
                !n.getIsRead()
        ));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("notifyUser sends email when sendEmail=true")
    void notify_user_sends_email_when_flag_true() throws Exception {
        notificationService.notifyUser(user, "Subject", "Body",
                Notification.Priority.MEDIUM, true);

        verify(emailService).sendGenericNotification(user.getEmail(), "Subject", "Body");
    }

    @Test
    @DisplayName("notifyUser does not send email when sendEmail=false")
    void notify_user_no_email_when_flag_false() {
        notificationService.notifyUser(user, "Subject", "Body",
                Notification.Priority.LOW, false);

        verifyNoInteractions(emailService);
    }

    // ── notifyEventApproval ───────────────────────────────────────────

    @Test
    @DisplayName("notifyEventApproval sends exactly one approval email (no double-send)")
    void notify_approval_sends_single_email() throws Exception {
        notificationService.notifyEventApproval(user, "Summer Fest", true);

        // Must call sendEventApprovalEmail exactly once
        verify(emailService, times(1)).sendEventApprovalEmail(user.getEmail(), "Summer Fest", true);
        // Must NOT call sendGenericNotification (that would be a double-send)
        verify(emailService, never()).sendGenericNotification(anyString(), anyString(), anyString());
        // In-app notification must be saved
        verify(notificationRepository).save(argThat(n ->
                n.getPriority() == Notification.Priority.HIGH &&
                n.getTitle().contains("Approved")
        ));
    }

    @Test
    @DisplayName("notifyEventApproval uses REJECTED title when not approved")
    void notify_approval_rejected_title() {
        notificationService.notifyEventApproval(user, "Winter Gala", false);

        verify(notificationRepository).save(argThat(n ->
                n.getTitle().contains("Rejected")
        ));
    }

    // ── notifyRegistrationConfirmed ───────────────────────────────────

    @Test
    @DisplayName("notifyRegistrationConfirmed saves MEDIUM priority notification and sends email")
    void notify_registration_confirmed() throws Exception {
        notificationService.notifyRegistrationConfirmed(user, "Yoga Class");

        verify(notificationRepository).save(argThat(n ->
                n.getPriority() == Notification.Priority.MEDIUM &&
                n.getTitle().contains("Yoga Class")
        ));
        verify(emailService, times(1)).sendRegistrationConfirmationEmail(user.getEmail(), "Yoga Class");
        verify(emailService, never()).sendGenericNotification(anyString(), anyString(), anyString());
    }

    // ── notifyEventCancelled ──────────────────────────────────────────

    @Test
    @DisplayName("notifyEventCancelled saves HIGH priority notification and sends cancellation email")
    void notify_event_cancelled() throws Exception {
        notificationService.notifyEventCancelled(user, "Beach Cleanup");

        verify(notificationRepository).save(argThat(n ->
                n.getPriority() == Notification.Priority.HIGH &&
                n.getTitle().contains("Cancelled")
        ));
        verify(emailService, times(1)).sendEventCancellationEmail(user.getEmail(), "Beach Cleanup");
    }

    // ── broadcastToZone ───────────────────────────────────────────────

    @Test
    @DisplayName("broadcastToZone notifies all zone residents and sends announcement emails")
    void broadcast_to_zone_notifies_all_residents() throws Exception {
        User user2 = new User();
        user2.setEmail("user2@test.com");
        user2.setZone(zone);

        when(userRepository.findByZone(zone)).thenReturn(List.of(user, user2));

        notificationService.broadcastToZone(zone, "Road Closure", "Main St closed tomorrow",
                Notification.Priority.URGENT);

        // Two in-app notifications saved
        verify(notificationRepository, times(2)).save(argThat(n ->
                n.getPriority() == Notification.Priority.URGENT &&
                n.getTitle().equals("Road Closure")
        ));
        // Two announcement emails sent
        verify(emailService, times(1)).sendAnnouncementEmail(
                user.getEmail(), zone.getName(), "Road Closure", "Main St closed tomorrow");
        verify(emailService, times(1)).sendAnnouncementEmail(
                user2.getEmail(), zone.getName(), "Road Closure", "Main St closed tomorrow");
    }

    @Test
    @DisplayName("broadcastToZone with empty zone sends no notifications")
    void broadcast_to_empty_zone_sends_nothing() {
        when(userRepository.findByZone(zone)).thenReturn(List.of());

        notificationService.broadcastToZone(zone, "Test", "msg", Notification.Priority.LOW);

        verify(notificationRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    // ── countUnread ───────────────────────────────────────────────────

    @Test
    @DisplayName("countUnread delegates to DB count query")
    void count_unread_uses_db_query() {
        when(notificationRepository.countByUserAndIsReadFalse(user)).thenReturn(7L);

        long count = notificationService.countUnread(user);

        assertEquals(7L, count);
        verify(notificationRepository).countByUserAndIsReadFalse(user);
    }

    // ── Email failure resilience ──────────────────────────────────────

    @Test
    @DisplayName("Email failure does not propagate — notification is still saved")
    void email_failure_does_not_break_notification() throws Exception {
        doThrow(new RuntimeException("SMTP error"))
                .when(emailService).sendEventApprovalEmail(anyString(), anyString(), anyBoolean());

        assertDoesNotThrow(() ->
                notificationService.notifyEventApproval(user, "Art Fair", true));

        verify(notificationRepository).save(any());
    }
}
