package com.yasarbilgi.announcementtracker.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Duyuru listesi kimlik doğrulaması olmadan okunabilir")
    void announcements_GetWithoutAuthentication_IsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/announcements"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Manuel tarama kimlik doğrulaması olmadan tetiklenemez")
    void announcements_TriggerWithoutAuthentication_IsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/announcements/trigger"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Abone listesi kimlik doğrulaması olmadan okunamaz")
    void subscribers_GetWithoutAuthentication_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/subscribers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Halka açık kayıt endpoint'i güvenlik filtresinden geçer")
    void subscribers_RegisterWithoutAuthentication_ReachesValidation() throws Exception {
        mockMvc.perform(post("/api/v1/subscribers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sahte JWT benzeri token kullanıcı profilini açamaz")
    void userProfile_WithForgedJwtLikeToken_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/user/me")
                        .header("Authorization", "Bearer forged.header.payload"))
                .andExpect(status().isForbidden());
    }
}
