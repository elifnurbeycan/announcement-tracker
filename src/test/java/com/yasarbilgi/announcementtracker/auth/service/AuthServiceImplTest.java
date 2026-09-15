package com.yasarbilgi.announcementtracker.auth.service;

import com.yasarbilgi.announcementtracker.dto.request.LoginRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.AdminUserDto;
import com.yasarbilgi.announcementtracker.dto.response.LoginResponseDto;
import com.yasarbilgi.announcementtracker.entity.AdminUser;
import com.yasarbilgi.announcementtracker.exception.ScrapingException;
import com.yasarbilgi.announcementtracker.repository.AdminUserRepository;
import com.yasarbilgi.announcementtracker.service.impl.AuthServiceImpl;
import com.yasarbilgi.announcementtracker.util.PasswordEncoderHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private PasswordEncoderHelper passwordEncoderHelper;

    @InjectMocks
    private AuthServiceImpl authService;

    private AdminUser adminUser;

    @BeforeEach
    void setUp() {
        adminUser = AdminUser.builder()
                .id(1L)
                .username("admin")
                .passwordHash("hashed_pass")
                .fullName("Super Admin")
                .build();
    }

    @Test
    @DisplayName("Doğru kullanıcı adı ve şifre ile başarılı giriş yapılmalı ve token üretilmeli")
    void login_ValidCredentials_ShouldReturnLoginResponse() {
        LoginRequestDto request = new LoginRequestDto("admin", "admin123");

        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoderHelper.matches("admin123", "hashed_pass")).thenReturn(true);

        LoginResponseDto response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("admin");
        assertThat(response.getFullName()).isEqualTo("Super Admin");
        assertThat(response.getToken()).startsWith("SA-TOKEN-");

        verify(adminUserRepository).save(adminUser);
    }

    @Test
    @DisplayName("Yanlış şifre girildiğinde ScrapingException fırlatılmalı")
    void login_InvalidPassword_ShouldThrowException() {
        LoginRequestDto request = new LoginRequestDto("admin", "wrongpassword");

        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoderHelper.matches("wrongpassword", "hashed_pass")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ScrapingException.class)
                .hasMessageContaining("Geçersiz kullanıcı adı veya şifre");
    }

    @Test
    @DisplayName("Olmayan kullanıcı adı ile giriş yapılırsa ScrapingException fırlatılmalı")
    void login_UserNotFound_ShouldThrowException() {
        LoginRequestDto request = new LoginRequestDto("unknown", "admin123");

        when(adminUserRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ScrapingException.class)
                .hasMessageContaining("Geçersiz kullanıcı adı veya şifre");
    }

    @Test
    @DisplayName("Giriş yapılmış oturumun token'ı doğrulanabilmeli")
    void validateToken_ValidSession_ShouldReturnUserDto() {
        LoginRequestDto request = new LoginRequestDto("admin", "admin123");
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoderHelper.matches("admin123", "hashed_pass")).thenReturn(true);

        LoginResponseDto loginRes = authService.login(request);

        AdminUserDto dto = authService.validateToken("Bearer " + loginRes.getToken());

        assertThat(dto).isNotNull();
        assertThat(dto.getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("Geçersiz token doğrulamasında ScrapingException fırlatılmalı")
    void validateToken_InvalidToken_ShouldThrowException() {
        assertThatThrownBy(() -> authService.validateToken("Bearer INVALID_TOKEN_123"))
                .isInstanceOf(ScrapingException.class)
                .hasMessageContaining("Oturum süreniz doldu");
    }

    @Test
    @DisplayName("Çıkış yapıldığında token geçersiz hale gelmeli")
    void logout_ShouldRemoveSessionToken() {
        LoginRequestDto request = new LoginRequestDto("admin", "admin123");
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(passwordEncoderHelper.matches("admin123", "hashed_pass")).thenReturn(true);

        LoginResponseDto loginRes = authService.login(request);
        String token = loginRes.getToken();

        authService.logout("Bearer " + token);

        assertThatThrownBy(() -> authService.validateToken(token))
                .isInstanceOf(ScrapingException.class);
    }
}
