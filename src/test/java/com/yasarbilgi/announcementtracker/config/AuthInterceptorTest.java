package com.yasarbilgi.announcementtracker.config;

import com.yasarbilgi.announcementtracker.dto.response.AdminUserDto;
import com.yasarbilgi.announcementtracker.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthInterceptor authInterceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("OPTIONS (CORS preflight) isteklerine izin verilmeli")
    void preHandle_OptionsRequest_ShouldAllow() throws Exception {
        request.setMethod("OPTIONS");
        request.setRequestURI("/api/v1/subscribers");

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Public (Genel) URL'lere (login.html) doğrudan izin verilmeli")
    void preHandle_PublicUri_ShouldAllowWithoutToken() throws Exception {
        request.setMethod("GET");
        request.setRequestURI("/login.html");

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Geçerli token içeren korumalı API isteğine izin verilmeli")
    void preHandle_ValidTokenHeader_ShouldAllow() throws Exception {
        request.setMethod("GET");
        request.setRequestURI("/api/v1/subscribers");
        request.addHeader("Authorization", "Bearer VALID_TOKEN");

        when(authService.validateToken("Bearer VALID_TOKEN")).thenReturn(AdminUserDto.builder().username("admin").build());

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Token bulunmayan API isteği HTTP 401 Unauthorized dönmeli")
    void preHandle_MissingTokenApiUri_ShouldReturn401() throws Exception {
        request.setMethod("GET");
        request.setRequestURI("/api/v1/subscribers");

        doThrow(new RuntimeException("Token missing")).when(authService).validateToken(null);

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Token bulunmayan sayfa (dashboard.html) isteği admin-login.html sayfasına yönlendirmeli")
    void preHandle_MissingTokenHtmlUri_ShouldRedirectToLogin() throws Exception {
        request.setMethod("GET");
        request.setRequestURI("/dashboard.html");

        doThrow(new RuntimeException("Token missing")).when(authService).validateToken(null);

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/admin-login.html");
    }
}
