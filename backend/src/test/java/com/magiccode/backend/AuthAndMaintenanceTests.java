package com.magiccode.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndMaintenanceTests extends TestDataSupport {

    @BeforeEach
    void setUp() {
        clearData();
        createAdmin();
        createMaintenance(false);
    }

    @Test
    void loginAndMeEndpointEnforceTokenBoundaries() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", ADMIN_USERNAME, "password", ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value(ADMIN_USERNAME));

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", ADMIN_USERNAME, "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.token").doesNotExist());

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "missing-user", "password", ADMIN_PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());

        mockMvc.perform(get("/api/admin/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));

        mockMvc.perform(get("/api/admin/auth/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/auth/me")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(ADMIN_USERNAME))
                .andExpect(jsonPath("$.role").value("ROOT"));
    }

    @Test
    void noDefaultWeakAdminIsCreatedWithoutBootstrapEnvironment() {
        assertThat(userRepository.findAll())
                .extracting("username")
                .containsExactly(ADMIN_USERNAME);
    }

    @Test
    void maintenanceStatusDefaultsAndRequiresAuthForUpdates() throws Exception {
        mockMvc.perform(get("/api/maintenance/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(put("/api/maintenance/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "enabled", true,
                                "mode", "maintenance",
                                "username", ADMIN_USERNAME,
                                "password", ADMIN_PASSWORD
                        ))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/maintenance/update")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "enabled", true,
                                "mode", "maintenance",
                                "username", ADMIN_USERNAME,
                                "password", ADMIN_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(put("/api/maintenance/update")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "enabled", false,
                                "mode", "maintenance",
                                "username", ADMIN_USERNAME,
                                "password", ADMIN_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void maintenanceModeDoesNotBlockAuthOpenApiOrActuator() throws Exception {
        maintenanceConfigRepository.deleteAll();
        createMaintenance(true);

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", ADMIN_USERNAME, "password", ADMIN_PASSWORD))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}
