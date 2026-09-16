package com.yasarbilgi.announcementtracker.exception;

import com.yasarbilgi.announcementtracker.dto.response.ApiResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

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
        ScrapingException ex = new ScrapingException("Geçersiz kullanıcı adı veya şifre");
        ResponseEntity<ApiResponseDto<Void>> response = globalExceptionHandler.handleScrapingException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("Geçersiz kullanıcı adı veya şifre");
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
