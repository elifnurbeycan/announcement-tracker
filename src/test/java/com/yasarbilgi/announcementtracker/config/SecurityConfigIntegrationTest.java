package com.yasarbilgi.announcementtracker.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Ortak giriş adresi sağlayıcı seçimi göstermeden Keycloak'a yönlendirir")
    void login_RedirectsDirectlyToKeycloak() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oauth2/authorization/keycloak"));
    }

    @Test
    @DisplayName("Duyuru listesi kimlik doğrulaması olmadan okunabilir")
    void announcements_GetWithoutAuthentication_IsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/announcements"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Manuel tarama kimlik doğrulaması olmadan tetiklenemez")
    void announcements_TriggerWithoutAuthentication_IsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/announcements/trigger").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Abone listesi kimlik doğrulaması olmadan okunamaz")
    void subscribers_GetWithoutAuthentication_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/subscribers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Abone kayıt endpoint'i kimlik doğrulaması olmadan kullanılamaz")
    void subscribers_RegisterWithoutAuthentication_IsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/subscribers/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Değişiklik yapan istek CSRF token olmadan reddedilir")
    void subscribers_RegisterWithoutCsrf_IsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/subscribers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sahte JWT benzeri token kullanıcı profilini açamaz")
    void userProfile_WithForgedJwtLikeToken_IsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/user/me")
                        .cookie(new Cookie(SessionCookieService.USER_COOKIE, "forged.header.payload")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Kaldırılan kullanıcı parola girişi endpoint'i anonim kullanıma açık değildir")
    void removedUserPasswordLogin_IsNotPublic() throws Exception {
        mockMvc.perform(post("/api/v1/user/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
