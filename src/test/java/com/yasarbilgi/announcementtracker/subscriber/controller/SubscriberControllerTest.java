package com.yasarbilgi.announcementtracker.subscriber.controller;

import com.yasarbilgi.announcementtracker.controller.SubscriberController;
import com.yasarbilgi.announcementtracker.dto.request.SubscriberRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.ApiResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.SubscriberResponseDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.service.SubscriberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriberControllerTest {

    @Mock
    private SubscriberService subscriberService;

    @InjectMocks
    private SubscriberController subscriberController;

    private SubscriberResponseDto sampleDto;

    @BeforeEach
    void setUp() {
        sampleDto = SubscriberResponseDto.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .active(true)
                .subscribedSites(Set.of(SiteType.EBELGE_GIB))
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/subscribers - Tüm aboneleri listeleme HTTP 200")
    void getAllSubscribers_ShouldReturnSubscriberList() {
        when(subscriberService.getAllSubscribers()).thenReturn(List.of(sampleDto));

        ResponseEntity<ApiResponseDto<List<SubscriberResponseDto>>> response = subscriberController.getAllSubscribers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    @DisplayName("POST /api/v1/subscribers - Yeni abone kaydetme HTTP 201")
    void addSubscriber_ValidRequest_ShouldReturnCreated() {
        SubscriberRequestDto requestDto = SubscriberRequestDto.builder()
                .email("test@example.com")
                .fullName("Test User")
                .subscribedSites(Set.of(SiteType.EBELGE_GIB))
                .build();

        when(subscriberService.addSubscriber(any(SubscriberRequestDto.class))).thenReturn(sampleDto);

        ResponseEntity<ApiResponseDto<SubscriberResponseDto>> response = subscriberController.addSubscriber(requestDto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("DELETE /api/v1/subscribers/{id} - Abone silme HTTP 200")
    void deleteSubscriber_ShouldReturnOk() {
        doNothing().when(subscriberService).deleteSubscriber(1L);

        ResponseEntity<ApiResponseDto<Void>> response = subscriberController.deleteSubscriber(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(subscriberService).deleteSubscriber(1L);
    }

    @Test
    @DisplayName("PATCH /api/v1/subscribers/{id}/status - Durum güncelleme HTTP 200")
    void toggleStatus_ShouldReturnOk() {
        doNothing().when(subscriberService).toggleSubscriberStatus(1L, false);

        ResponseEntity<ApiResponseDto<Void>> response = subscriberController.toggleStatus(1L, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(subscriberService).toggleSubscriberStatus(1L, false);
    }

    @Test
    @DisplayName("PATCH /api/v1/subscribers/{id}/sites - Site tercihleri güncelleme HTTP 200")
    void updateSitePreferences_ShouldReturnOk() {
        Set<SiteType> sites = Set.of(SiteType.KOSGEB);
        when(subscriberService.updateSitePreferences(eq(1L), anySet())).thenReturn(sampleDto);

        ResponseEntity<ApiResponseDto<SubscriberResponseDto>> response = subscriberController.updateSitePreferences(1L, sites);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("POST /api/v1/subscribers/upload-excel - Excel yükleme HTTP 200")
    void uploadExcelSubscribers_ShouldReturnImportCount() {
        MockMultipartFile file = new MockMultipartFile("file", "subscribers.csv", "text/csv", "test".getBytes());
        com.yasarbilgi.announcementtracker.dto.response.ExcelImportResultDto resultDto = com.yasarbilgi.announcementtracker.dto.response.ExcelImportResultDto.builder()
                .totalRows(5).successCount(5).errorCount(0).errors(List.of()).build();
        when(subscriberService.importSubscribersFromExcelDetailed(eq(file), any())).thenReturn(resultDto);

        ResponseEntity<ApiResponseDto<com.yasarbilgi.announcementtracker.dto.response.ExcelImportResultDto>> response = subscriberController.uploadExcelSubscribers(file, List.of(SiteType.EBELGE_GIB));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getSuccessCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("GET /api/v1/subscribers/template-excel - Örnek Excel indir HTTP 200")
    void downloadExcelTemplate_ShouldReturnExcelBytes() {
        ResponseEntity<byte[]> response = subscriberController.downloadExcelTemplate();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isGreaterThan(0);
    }

    @Test
    @DisplayName("POST /api/v1/subscribers/unsubscribe - Abonelik iptal API")
    void unsubscribeApi_ValidEmail_ShouldReturnSuccess() {
        when(subscriberService.unsubscribeByEmail("test@example.com")).thenReturn(true);

        ResponseEntity<ApiResponseDto<Boolean>> response = subscriberController.unsubscribeApi("test@example.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isTrue();
    }

    @Test
    @DisplayName("GET /api/v1/subscribers/unsubscribe - Yalnızca güvenli onay sayfasını gösterir")
    void unsubscribeHtml_ShouldNotChangeSubscription() {
        DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "csrf-value");

        ResponseEntity<String> response = subscriberController.unsubscribeHtml("test@example.com", csrfToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Abonelikten Çık", "csrf-value", "method='post'");
        verify(subscriberService, never()).unsubscribeByEmail(anyString());
    }

    @Test
    @DisplayName("POST /api/v1/subscribers/unsubscribe/confirm - Onay sonrası aboneliği iptal eder")
    void unsubscribeConfirm_ShouldDeactivateSubscriber() {
        when(subscriberService.unsubscribeByEmail("test@example.com")).thenReturn(true);

        ResponseEntity<String> response = subscriberController.unsubscribeConfirm("test@example.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Abonelik İptal Edildi");
        verify(subscriberService).unsubscribeByEmail("test@example.com");
    }
}
