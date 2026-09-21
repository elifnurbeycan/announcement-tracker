package com.yasarbilgi.announcementtracker.auth.service;

import com.yasarbilgi.announcementtracker.dto.request.LoginRequestDto;
import com.yasarbilgi.announcementtracker.exception.ScrapingException;
import com.yasarbilgi.announcementtracker.repository.AdminUserRepository;
import com.yasarbilgi.announcementtracker.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("Keycloak sunucusuna ulaşılamadığında veya yanlış şifre girildiğinde ScrapingException fırlatılmalı")
    void login_InvalidPasswordOrServerDown_ShouldThrowException() {
        LoginRequestDto request = new LoginRequestDto("admin", "wrongpassword");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ScrapingException.class)
                .hasMessageContaining("Geçersiz kullanıcı adı veya şifre");
    }

    @Test
    @DisplayName("Geçersiz token doğrulamasında ScrapingException fırlatılmalı")
    void validateToken_InvalidToken_ShouldThrowException() {
        assertThatThrownBy(() -> authService.validateToken("Bearer INVALID_TOKEN_123"))
                .isInstanceOf(ScrapingException.class)
                .hasMessageContaining("Oturum süreniz doldu");
    }

    @Test
    @DisplayName("İmzası doğrulanmamış JWT benzeri token admin oturumu olarak kabul edilmemeli")
    void validateToken_ForgedJwtLikeToken_ShouldThrowException() {
        assertThatThrownBy(() -> authService.validateToken("Bearer forged.header.payload"))
                .isInstanceOf(ScrapingException.class)
                .hasMessageContaining("Oturum süreniz doldu");
    }
}
