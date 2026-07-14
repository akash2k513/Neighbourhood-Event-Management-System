-- Neighborhood Event Management System — MySQL Schema
-- SRS Appendix A: All 10 core tables

CREATE DATABASE IF NOT EXISTS neighborhood_events CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE neighborhood_events;

-- 1. Zones
CREATE TABLE IF NOT EXISTS zones (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500)
);

-- 2. Users
CREATE TABLE IF NOT EXISTS users (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name             VARCHAR(100) NOT NULL,
    email                 VARCHAR(150) NOT NULL UNIQUE,
    password              VARCHAR(255) NOT NULL,
    role                  ENUM('GUEST','RESIDENT','EVENT_ORGANIZER','ZONE_COORDINATOR','COMMUNITY_MANAGER','ADMIN') NOT NULL,
    zone_id               BIGINT,
    enabled               TINYINT(1) NOT NULL DEFAULT 0,
    account_locked        TINYINT(1) NOT NULL DEFAULT 0,
    failed_login_attempts INT         NOT NULL DEFAULT 0,
    lock_time             DATETIME,
    created_at            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_zone FOREIGN KEY (zone_id) REFERENCES zones(id) ON DELETE SET NULL,
    INDEX idx_users_email  (email),
    INDEX idx_users_role   (role),
    INDEX idx_users_zone   (zone_id)
);

-- 3. Venues
CREATE TABLE IF NOT EXISTS venues (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(255) NOT NULL,
    address  VARCHAR(500),
    capacity INT,
    zone_id  BIGINT,
    CONSTRAINT fk_venues_zone FOREIGN KEY (zone_id) REFERENCES zones(id) ON DELETE SET NULL,
    INDEX idx_venues_zone (zone_id)
);

-- 4. Events
CREATE TABLE IF NOT EXISTS events (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    title                VARCHAR(255) NOT NULL,
    description          VARCHAR(2000),
    category             ENUM('SOCIAL','SPORTS','CULTURAL','EDUCATIONAL','HEALTH','ENVIRONMENT','OTHER') NOT NULL,
    status               ENUM('DRAFT','PENDING_APPROVAL','APPROVED','REJECTED','PUBLISHED','CANCELLED','COMPLETED') NOT NULL DEFAULT 'DRAFT',
    start_time           DATETIME     NOT NULL,
    end_time             DATETIME     NOT NULL,
    capacity             INT          NOT NULL,
    registered_count     INT          NOT NULL DEFAULT 0,
    location             VARCHAR(255) NOT NULL,
    recurring            TINYINT(1)   NOT NULL DEFAULT 0,
    recurrence_type      VARCHAR(50),
    recurrence_end_date  DATE,
    organizer_id         BIGINT,
    zone_id              BIGINT,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_events_organizer FOREIGN KEY (organizer_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_events_zone      FOREIGN KEY (zone_id)      REFERENCES zones(id) ON DELETE SET NULL,
    INDEX idx_events_status     (status),
    INDEX idx_events_organizer  (organizer_id),
    INDEX idx_events_zone       (zone_id),
    INDEX idx_events_start_time (start_time)
);

-- 5. EventRegistrations
CREATE TABLE IF NOT EXISTS event_registrations (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id      BIGINT   NOT NULL,
    user_id       BIGINT   NOT NULL,
    status        ENUM('REGISTERED','CANCELLED','ATTENDED') NOT NULL DEFAULT 'REGISTERED',
    registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ereg_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_ereg_user  FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
    UNIQUE INDEX uq_ereg_event_user (event_id, user_id),
    INDEX idx_ereg_user  (user_id),
    INDEX idx_ereg_event (event_id)
);

-- 6. EventApprovals
CREATE TABLE IF NOT EXISTS event_approvals (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id    BIGINT   NOT NULL UNIQUE,
    approved_by BIGINT,
    status      ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    remarks     VARCHAR(500),
    approved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_eapp_event    FOREIGN KEY (event_id)    REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_eapp_approver FOREIGN KEY (approved_by) REFERENCES users(id)  ON DELETE SET NULL,
    INDEX idx_eapp_event    (event_id),
    INDEX idx_eapp_approver (approved_by)
);

-- 7. Resources
CREATE TABLE IF NOT EXISTS resources (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    quantity    INT          NOT NULL
);

-- 8. ResourceBookings
CREATE TABLE IF NOT EXISTS resource_bookings (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id      BIGINT   NOT NULL,
    event_id         BIGINT   NOT NULL,
    quantity_booked  INT      NOT NULL,
    booked_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rbooking_resource FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE CASCADE,
    CONSTRAINT fk_rbooking_event    FOREIGN KEY (event_id)    REFERENCES events(id)    ON DELETE CASCADE,
    INDEX idx_rbooking_resource (resource_id),
    INDEX idx_rbooking_event    (event_id)
);

-- 9. Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(255) NOT NULL,
    message    VARCHAR(1000),
    is_read    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_user    (user_id),
    INDEX idx_notif_is_read (is_read)
);

-- 11. RefreshTokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     BIGINT       NOT NULL,
    expiry_date DATETIME     NOT NULL,
    CONSTRAINT fk_rtoken_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 12. PasswordResetTokens
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     BIGINT       NOT NULL,
    expiry_date DATETIME     NOT NULL,
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 13. EmailVerificationTokens
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     BIGINT       NOT NULL,
    expiry_date DATETIME     NOT NULL,
    CONSTRAINT fk_evt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 10. AuditLogs
CREATE TABLE IF NOT EXISTS audit_logs (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    action     VARCHAR(255) NOT NULL,
    user_id    BIGINT,
    details    VARCHAR(1000),
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_audit_user   (user_id),
    INDEX idx_audit_action (action)
);
