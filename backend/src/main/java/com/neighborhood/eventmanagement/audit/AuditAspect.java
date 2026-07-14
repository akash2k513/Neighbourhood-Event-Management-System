package com.neighborhood.eventmanagement.audit;

import com.neighborhood.eventmanagement.entity.AuditLog;
import com.neighborhood.eventmanagement.entity.User;
import com.neighborhood.eventmanagement.repository.AuditLogRepository;
import com.neighborhood.eventmanagement.repository.UserRepository;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditAspect(AuditLogRepository auditLogRepository,
                       UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void logAction(JoinPoint joinPoint, Auditable auditable, Object result) {

        AuditLog log = new AuditLog();
        log.setAction(auditable.action());
        log.setCreatedAt(LocalDateTime.now());

        // Capture acting user from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            userRepository.findByEmail(auth.getName()).ifPresent(log::setUser);
        }

        // Build details: method args as old values, return value as new value
        String args = Arrays.stream(joinPoint.getArgs())
                .map(arg -> arg == null ? "null" : arg.toString())
                .collect(Collectors.joining(", "));

        String details = "method=" + joinPoint.getSignature().getName()
                + " | args=[" + args + "]"
                + " | result=" + (result != null ? result.toString() : "void");

        log.setDetails(details.length() > 1000 ? details.substring(0, 1000) : details);

        auditLogRepository.save(log);
    }
}
