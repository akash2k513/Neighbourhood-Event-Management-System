package com.neighborhood.eventmanagement.security;

import com.neighborhood.eventmanagement.entity.Role;
import com.neighborhood.eventmanagement.entity.User;
import com.neighborhood.eventmanagement.security.service.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC permission matrix test — SRS Section 9.
 * Verifies each of the 6 roles can/cannot access protected route groups.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacPermissionMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    private CustomUserDetails userWith(Role role) {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setRole(role);
        user.setEnabled(true);
        user.setAccountLocked(false);
        return new CustomUserDetails(user);
    }

    // ------------------------------------------------------------------
    // /api/admin/** — ADMIN only
    // ------------------------------------------------------------------

    @Test
    @DisplayName("ADMIN can access /api/admin/**")
    void adminCanAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.ADMIN))))
                .andExpect(status().isNotFound()); // 404 = route exists, no handler yet — NOT 403
    }

    @Test
    @DisplayName("COMMUNITY_MANAGER cannot access /api/admin/**")
    void communityManagerCannotAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.COMMUNITY_MANAGER))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ZONE_COORDINATOR cannot access /api/admin/**")
    void zoneCoordinatorCannotAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.ZONE_COORDINATOR))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EVENT_ORGANIZER cannot access /api/admin/**")
    void organizerCannotAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.EVENT_ORGANIZER))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RESIDENT cannot access /api/admin/**")
    void residentCannotAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.RESIDENT))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GUEST cannot access /api/admin/**")
    void guestCannotAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.GUEST))))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // /api/events/manage/** — COMMUNITY_MANAGER + ADMIN
    // ------------------------------------------------------------------

    @Test
    @DisplayName("COMMUNITY_MANAGER can access /api/events/manage/**")
    void communityManagerCanAccessEventManage() throws Exception {
        mockMvc.perform(get("/api/events/manage/pending")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.COMMUNITY_MANAGER))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("ADMIN can access /api/events/manage/**")
    void adminCanAccessEventManage() throws Exception {
        mockMvc.perform(get("/api/events/manage/pending")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.ADMIN))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("EVENT_ORGANIZER cannot access /api/events/manage/**")
    void organizerCannotAccessEventManage() throws Exception {
        mockMvc.perform(get("/api/events/manage/pending")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.EVENT_ORGANIZER))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RESIDENT cannot access /api/events/manage/**")
    void residentCannotAccessEventManage() throws Exception {
        mockMvc.perform(get("/api/events/manage/pending")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.RESIDENT))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GUEST cannot access /api/events/manage/**")
    void guestCannotAccessEventManage() throws Exception {
        mockMvc.perform(get("/api/events/manage/pending")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.GUEST))))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // /api/zones/manage/** — ZONE_COORDINATOR + COMMUNITY_MANAGER + ADMIN
    // ------------------------------------------------------------------

    @Test
    @DisplayName("ZONE_COORDINATOR can access /api/zones/manage/**")
    void zoneCoordinatorCanAccessZoneManage() throws Exception {
        mockMvc.perform(get("/api/zones/manage/list")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.ZONE_COORDINATOR))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("COMMUNITY_MANAGER can access /api/zones/manage/**")
    void communityManagerCanAccessZoneManage() throws Exception {
        mockMvc.perform(get("/api/zones/manage/list")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.COMMUNITY_MANAGER))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("ADMIN can access /api/zones/manage/**")
    void adminCanAccessZoneManage() throws Exception {
        mockMvc.perform(get("/api/zones/manage/list")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.ADMIN))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("EVENT_ORGANIZER cannot access /api/zones/manage/**")
    void organizerCannotAccessZoneManage() throws Exception {
        mockMvc.perform(get("/api/zones/manage/list")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.EVENT_ORGANIZER))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RESIDENT cannot access /api/zones/manage/**")
    void residentCannotAccessZoneManage() throws Exception {
        mockMvc.perform(get("/api/zones/manage/list")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.RESIDENT))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GUEST cannot access /api/zones/manage/**")
    void guestCannotAccessZoneManage() throws Exception {
        mockMvc.perform(get("/api/zones/manage/list")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.GUEST))))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // /api/events/organizer/** — EVENT_ORGANIZER + COMMUNITY_MANAGER + ADMIN
    // ------------------------------------------------------------------

    @Test
    @DisplayName("EVENT_ORGANIZER can access /api/events/organizer/**")
    void organizerCanAccessOrganizerRoutes() throws Exception {
        mockMvc.perform(get("/api/events/organizer/my-events")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.EVENT_ORGANIZER))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("COMMUNITY_MANAGER can access /api/events/organizer/**")
    void communityManagerCanAccessOrganizerRoutes() throws Exception {
        mockMvc.perform(get("/api/events/organizer/my-events")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.COMMUNITY_MANAGER))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("RESIDENT cannot access /api/events/organizer/**")
    void residentCannotAccessOrganizerRoutes() throws Exception {
        mockMvc.perform(get("/api/events/organizer/my-events")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.RESIDENT))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GUEST cannot access /api/events/organizer/**")
    void guestCannotAccessOrganizerRoutes() throws Exception {
        mockMvc.perform(get("/api/events/organizer/my-events")
                .with(SecurityMockMvcRequestPostProcessors.user(userWith(Role.GUEST))))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Unauthenticated — must get 401
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Unauthenticated request to protected route returns 401")
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Public auth endpoints — no token needed
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/login is publicly accessible")
    void loginEndpointIsPublic() throws Exception {
        // GET on a POST-only endpoint returns 405 (not 401/403) — proves it's public
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed());
    }
}
