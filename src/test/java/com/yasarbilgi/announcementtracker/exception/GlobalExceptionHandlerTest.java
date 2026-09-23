package com.yasarbilgi.announcementtracker.exception;

import com.yasarbilgi.announcementtracker.dto.response.ApiResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private static class ValidationTarget {
        @SuppressWarnings("unused")
        private String email;
    }

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("ResourceNotFoundException yakalama testi (HTTP 404)")
    void handleResourceNotFound_ShouldReturn404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Abone bulunamadı");
        ResponseEntity<ApiResponseDto<Void>> response = globalExceptionHandler.handleResourceNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("Abone bulunamadı");
    }

    @Test
    @DisplayName("ScrapingException yakalama testi (HTTP 500)")
    void handleScrapingException_ShouldReturn500() {
        ScrapingException ex = new ScrapingException("https://internal.example scraper bağlantısı reddedildi");
        ResponseEntity<ApiResponseDto<Void>> response = globalExceptionHandler.handleScrapingException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Duyuru kaynağı taranırken bir hata oluştu. Lütfen daha sonra tekrar deneyin.")
                .doesNotContain("internal.example");
    }

    @Test
    @DisplayName("AlreadyExistsException yakalama testi (HTTP 409)")
    void handleAlreadyExists_ShouldReturn409() {
        ResponseEntity<ApiResponseDto<Void>> response = globalExceptionHandler.handleAlreadyExists(
                new AlreadyExistsException("Kayıt zaten mevcut."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Kayıt zaten mevcut.");
    }

    @Test
    @DisplayName("UnauthorizedException yakalama testi (HTTP 401)")
    void handleUnauthorized_ShouldReturn401() {
        ResponseEntity<ApiResponseDto<Void>> response = globalExceptionHandler.handleUnauthorized(
                new UnauthorizedException("Oturum süresi doldu."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Oturum süresi doldu.");
    }

    @Test
    @DisplayName("InvalidTokenException yakalama testi (HTTP 400)")
    void handleBadRequest_ShouldReturn400() {
        ResponseEntity<ApiResponseDto<Void>> response = globalExceptionHandler.handleBadRequest(
                new InvalidTokenException("Token geçersiz."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Token geçersiz.");
    }

    @Test
    @DisplayName("AccessDeniedException yakalama testi (HTTP 403)")
    void handleAccessDenied_ShouldReturn403() {
        ResponseEntity<ApiResponseDto<Void>> response = globalExceptionHandler.handleAccessDenied(
                new AccessDeniedException("Rol yetersiz."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("gerekli yetkiye sahip değilsiniz");
    }

    @Test
    @DisplayName("IdentityProviderException yakalama testi (HTTP 503)")
    void handleIdentityProviderException_ShouldReturn503() {
        ResponseEntity<ApiResponseDto<Void>> response = globalExceptionHandler.handleIdentityProviderException(
                new IdentityProviderException("Keycloak kullanılamıyor."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Keycloak kullanılamıyor.");
    }

    @Test
    @DisplayName("Validation hatası alan adıyla birlikte dönmeli")
    void handleValidationException_ShouldIncludeFieldName() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                new ValidationTarget(), "subscriberRequest");
        bindingResult.addError(new FieldError(
                "subscriberRequest", "email", "Geçerli bir e-posta adresi giriniz."));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiResponseDto<Void>> response =
                globalExceptionHandler.handleValidationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("email: Geçerli bir e-posta adresi giriniz.");
    }

    @Test
    @DisplayName("Genel Exception yakalama testi (HTTP 500)")
    void handleGeneralException_ShouldReturn500() {
        Exception ex = new RuntimeException("Veritabanı bağlantı hatası");
        ResponseEntity<ApiResponseDto<Void>> response = globalExceptionHandler.handleGeneralException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("İşlem gerçekleştirilemedi.");
    }
}
