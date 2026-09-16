package com.yasarbilgi.announcementtracker.announcement.controller;

import com.yasarbilgi.announcementtracker.controller.AnnouncementController;
import com.yasarbilgi.announcementtracker.dto.response.ApiResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.AnnouncementResponseDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.service.AnnouncementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementControllerTest {

    @Mock
    private AnnouncementService announcementService;

    @InjectMocks
    private AnnouncementController announcementController;

    private AnnouncementResponseDto sampleDto;

    @BeforeEach
    void setUp() {
        sampleDto = AnnouncementResponseDto.builder()
                .id(1L)
                .title("Test Duyuru")
                .content("İçerik")
                .sourceSite(SiteType.EBELGE_GIB)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/announcements - Sayfalı duyuruları çekme HTTP 200")
    void getAnnouncements_ShouldReturnPagedAnnouncements() {
        Page<AnnouncementResponseDto> pageDto = new PageImpl<>(List.of(sampleDto));
        when(announcementService.getAllAnnouncements(any(), any())).thenReturn(pageDto);

        ResponseEntity<ApiResponseDto<Page<AnnouncementResponseDto>>> response = 
                announcementController.getAnnouncements(SiteType.EBELGE_GIB, PageRequest.of(0, 10));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getContent()).hasSize(1);
        assertThat(response.getBody().getData().getContent().get(0).getTitle()).isEqualTo("Test Duyuru");// Bunu bu şekilde uzun yazmaya gerek var mı araştır
    }

    @Test
    @DisplayName("POST /api/v1/announcements/trigger - Tüm kaynakları tarama HTTP 200")
    void triggerScrape_ShouldReturnScrapedAnnouncements() {
        when(announcementService.triggerScrapeAll()).thenReturn(List.of(sampleDto));

        ResponseEntity<ApiResponseDto<List<AnnouncementResponseDto>>> response = 
                announcementController.triggerScrape(null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).hasSize(1);
    }
}
