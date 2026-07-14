package com.neighborhood.eventmanagement.repository;

import com.neighborhood.eventmanagement.entity.PasswordResetToken;
import com.neighborhood.eventmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUser(User user);

}