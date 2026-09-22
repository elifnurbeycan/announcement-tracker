package com.yasarbilgi.announcementtracker.auth.controller;

import com.yasarbilgi.announcementtracker.controller.AuthController;
import com.yasarbilgi.announcementtracker.config.SessionCookieService;
import com.yasarbilgi.announcementtracker.dto.request.LoginRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.AdminUserDto;
import com.yasarbilgi.announcementtracker.dto.response.ApiResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.LoginResponseDto;
import com.yasarbilgi.announcementtracker.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private SessionCookieService sessionCookieService;

    @InjectMocks
    private AuthController authController;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Doğru bilgilerle giriş yapma HTTP 200")
    void login_ValidCredentials_ShouldReturnOk() {
        ReflectionTestUtils.setField(authController, "localLoginEnabled", true);
        LoginRequestDto requestDto = new LoginRequestDto("admin", "admin123");
        LoginResponseDto loginResponse = LoginResponseDto.builder()
                .token("SA-TOKEN-12345")
                .username("admin")
                .fullName("Super Admin")
                .build();

        when(authService.login(any(LoginRequestDto.class))).thenReturn(loginResponse);

        ResponseEntity<ApiResponseDto<LoginResponseDto>> res = authController.login(requestDto, response);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getData().getToken()).isEqualTo("SA-TOKEN-12345");
        org.mockito.Mockito.verify(sessionCookieService).setAdminSession(response, "SA-TOKEN-12345");
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Aktif oturum bilgilerini alma HTTP 200")
    void getProfile_ValidToken_ShouldReturnUserDto() {
        AdminUserDto dto = AdminUserDto.builder()
                .id(1L)
                .username("admin")
                .fullName("Super Admin")
                .build();

        ResponseEntity<ApiResponseDto<AdminUserDto>> res = authController.getProfile(dto);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getData().getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - Oturumu sonlandırma HTTP 200")
    void logout_ShouldReturnOk() {
        when(sessionCookieService.resolveAdminSession(request)).thenReturn("SA-TOKEN-12345");
        doNothing().when(authService).logout("SA-TOKEN-12345");

        ResponseEntity<ApiResponseDto<Void>> res = authController.logout(request, response);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        org.mockito.Mockito.verify(sessionCookieService).clearAdminSession(response);
    }
}
