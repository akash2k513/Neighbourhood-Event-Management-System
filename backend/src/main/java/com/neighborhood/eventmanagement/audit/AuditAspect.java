package com.neighborhood.eventmanagement.audit;

import com.neighborhood.eventmanagement.entity.AuditLog;
import com.neighborhood.eventmanagement.repository.AuditLogRepository;
import com.neighborhood.eventmanagement.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            userRepository.findByEmail(auth.getName()).ifPresent(log::setUser);
        }

        // Capture IP and User-Agent from current HTTP request
        String ipAddress = "unknown";
        String userAgent = "unknown";
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                ipAddress = request.getRemoteAddr();
                userAgent = request.getHeader("User-Agent");
                if (userAgent == null) userAgent = "unknown";
            }
        } catch (Exception ignored) {}

        String args = Arrays.stream(joinPoint.getArgs())
                .map(arg -> arg == null ? "null" : arg.toString())
                .collect(Collectors.joining(", "));

        String details = "method=" + joinPoint.getSignature().getName()
                + " | args=[" + args + "]"
                + " | result=" + (result != null ? result.toString() : "void")
                + " | ip=" + ipAddress
                + " | ua=" + userAgent;

        log.setDetails(details.length() > 1000 ? details.substring(0, 1000) : details);

        auditLogRepository.save(log);
    }
}
