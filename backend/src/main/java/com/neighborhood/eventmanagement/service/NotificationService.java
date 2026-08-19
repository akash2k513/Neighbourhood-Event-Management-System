package com.neighborhood.eventmanagement.service;

import com.neighborhood.eventmanagement.entity.Notification;
import com.neighborhood.eventmanagement.entity.User;
import com.neighborhood.eventmanagement.entity.Zone;
import com.neighborhood.eventmanagement.repository.NotificationRepository;
import com.neighborhood.eventmanagement.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public NotificationService(NotificationRepository notificationRepository,
                                UserRepository userRepository,
                                EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    // ── Core notify ───────────────────────────────────────────────────

    public void notifyUser(User user, String title, String message,
                           Notification.Priority priority, boolean sendEmail) {
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        n.setPriority(priority);
        notificationRepository.save(n);

        if (sendEmail) {
            try {
                emailService.sendGenericNotification(user.getEmail(), title, message);
            } catch (Exception ignored) {}
        }
    }

    // ── Domain-specific notifications ─────────────────────────────────

    /**
     * Saves in-app notification AND sends one approval email.
     * notifyUser is called with sendEmail=false to avoid a second generic email.
     */
    public void notifyEventApproval(User organizer, String eventTitle, boolean approved) {
        String title = "Event " + (approved ? "Approved" : "Rejected") + ": " + eventTitle;
        String msg   = "Your event \"" + eventTitle + "\" has been "
                     + (approved ? "approved." : "rejected.");
        notifyUser(organizer, title, msg, Notification.Priority.HIGH, false);
        try {
            emailService.sendEventApprovalEmail(organizer.getEmail(), eventTitle, approved);
        } catch (Exception ignored) {}
    }

    public void notifyRegistrationConfirmed(User user, String eventTitle) {
        notifyUser(user,
                "Registration Confirmed: " + eventTitle,
                "You are registered for \"" + eventTitle + "\".",
                Notification.Priority.MEDIUM, false);
        try {
            emailService.sendRegistrationConfirmationEmail(user.getEmail(), eventTitle);
        } catch (Exception ignored) {}
    }

    public void notifyEventCancelled(User user, String eventTitle) {
        notifyUser(user,
                "Event Cancelled: " + eventTitle,
                "The event \"" + eventTitle + "\" has been cancelled.",
                Notification.Priority.HIGH, false);
        try {
            emailService.sendEventCancellationEmail(user.getEmail(), eventTitle);
        } catch (Exception ignored) {}
    }

    /**
     * Sends in-app notification + email to every resident in the zone.
     */
    public void broadcastToZone(Zone zone, String title, String message,
                                Notification.Priority priority) {
        List<User> residents = userRepository.findByZone(zone);
        for (User u : residents) {
            notifyUser(u, title, message, priority, false);
            try {
                emailService.sendAnnouncementEmail(u.getEmail(), zone.getName(), title, message);
            } catch (Exception ignored) {}
        }
    }

    // ── Count ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public long countUnread(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }
}
