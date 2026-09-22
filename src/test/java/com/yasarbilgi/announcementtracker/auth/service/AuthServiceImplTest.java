package com.yasarbilgi.announcementtracker.auth.service;

import com.yasarbilgi.announcementtracker.entity.AdminUser;
import com.yasarbilgi.announcementtracker.exception.ScrapingException;
import com.yasarbilgi.announcementtracker.repository.AdminUserRepository;
import com.yasarbilgi.announcementtracker.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("Keycloak yöneticisi ilk girişte yerel profil kaydıyla eşlenmeli")
    void createOidcSession_NewAdmin_ShouldCreateLocalProfile() {
        when(adminUserRepository.findByUsername("keycloak-admin")).thenReturn(Optional.empty());
        when(adminUserRepository.save(any(AdminUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.createOidcSession("keycloak-admin");

        assertThat(response.getUsername()).isEqualTo("keycloak-admin");
        verify(adminUserRepository).save(any(AdminUser.class));
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
