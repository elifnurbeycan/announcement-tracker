package com.yasarbilgi.announcementtracker.exception;

import com.yasarbilgi.announcementtracker.dto.response.ApiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // HTTP 404 - Kaynak Bulunamadı
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Kaynak bulunamadı: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.error(ex.getMessage()));
    }

    // Eksik statik dosyalar bir sunucu hatası değildir. Özellikle tarayıcıların
    // otomatik favicon isteği logları 500 hata yığınıyla doldurmamalıdır.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleStaticResourceNotFound(NoResourceFoundException ex) {
        log.debug("Statik kaynak bulunamadı: {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.error("Kaynak bulunamadı."));
    }

    // HTTP 409 - Zaten Var Olan Kayıt veya DB Kısıt İhlali
    @ExceptionHandler({AlreadyExistsException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ApiResponseDto<Void>> handleAlreadyExists(Exception ex) {
        log.warn("Çakışan kayıt / DB kısıt ihlali: {}", ex.getMessage());
        String message = ex instanceof AlreadyExistsException ? ex.getMessage() : "Bu kayıt zaten sistemde mevcut veya veri kısıtı ihlal edildi.";
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponseDto.error(message));
    }

    // HTTP 401 - Yanlış Şifre veya Yetkisiz Oturum
    @ExceptionHandler({UnauthorizedException.class, BadCredentialsException.class})
    public ResponseEntity<ApiResponseDto<Void>> handleUnauthorized(Exception ex) {
        log.warn("Kimlik doğrulama başarısız: {}", ex.getMessage());
        String message = ex instanceof BadCredentialsException ? "Kullanıcı adı veya şifre hatalı." : ex.getMessage();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponseDto.error(message));
    }

    // HTTP 403 - Yetki Yetersiz (Role Yetkisizliği)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Yetkisiz erişim denemesi: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponseDto.error("Bu işlemi gerçekleştirmek için gerekli yetkiye sahip değilsiniz."));
    }

    // HTTP 400 - DTO Validation (@Valid) Hataları
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Doğrulama (Validation) hatası: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.error(errors));
    }

    // HTTP 400 - Geçersiz Token / İş Kuralı İhlali / Hatalı Parametre
    @ExceptionHandler({InvalidTokenException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponseDto<Void>> handleBadRequest(Exception ex) {
        log.warn("Geçersiz istek / parametre hatası: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.error(ex.getMessage()));
    }

    // HTTP 400 - Bozuk JSON veya Geçersiz Enum Değeri
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Okunamayan istek gövdesi / geçersiz enum: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.error("İstek gövdesi (JSON) biçimi veya enum değeri geçersiz."));
    }

    // HTTP 400 - URL Parametresi Veri Tipi Uyuşmazlığı (Örn: /departments/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Parametre tip uyuşmazlığı: {}", ex.getMessage());
        String message = String.format("'%s' parametresi için verilen '%s' değeri geçersiz bir veri tipindedir.", ex.getName(), ex.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.error(message));
    }

    // HTTP 400 - Excel veya Dosya Yükleme Boyut Sınırı İhlali
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        log.warn("Yüklenen dosya boyutu sınırı aştı: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.error("Yüklenmeye çalışılan dosya boyutu izin verilen maksimum sınırı aşıyor."));
    }

    // HTTP 500 - Scraper (Web Kazıma) Hataları
    @ExceptionHandler(ScrapingException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleScrapingException(ScrapingException ex) {
        log.error("Scraping hatası: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(ex.getMessage()));
    }

    @ExceptionHandler(IdentityProviderException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleIdentityProviderException(IdentityProviderException ex) {
        log.error("Kimlik sağlayıcı işlemi başarısız: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponseDto.error(ex.getMessage() + " Abone kaydı değiştirilmedi; lütfen tekrar deneyin."));
    }

    // HTTP 500 - Yakalanmayan Genel Tüm Çökme Hataları
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Void>> handleGeneralException(Exception ex) {
        log.error("Sistemde beklenmeyen hata oluştu: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error("İşlem gerçekleştirilemedi."));
    }
}
