package com.yasarbilgi.announcementtracker.model;

import com.yasarbilgi.announcementtracker.dto.ScrapedAnnouncementDto;
import com.yasarbilgi.announcementtracker.dto.request.SubscriberRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.*;
import com.yasarbilgi.announcementtracker.entity.AdminUser;
import com.yasarbilgi.announcementtracker.entity.Announcement;
import com.yasarbilgi.announcementtracker.entity.Subscriber;
import com.yasarbilgi.announcementtracker.entity.SystemSetting;
import com.yasarbilgi.announcementtracker.enums.ScrapingStatus;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.exception.ResourceNotFoundException;
import com.yasarbilgi.announcementtracker.exception.ScrapingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ModelAndEnumTest {

    @Test
    @DisplayName("SiteType ve ScrapingStatus enum testleri")
    void enum_Tests() {
        assertThat(SiteType.EBELGE_GIB.getDisplayName()).isEqualTo("e-Belge GİB");
        assertThat(SiteType.EBELGE_GIB.getBaseUrl()).contains("ebelge.gib.gov.tr");
        assertThat(SiteType.KOSGEB.getDisplayName()).isEqualTo("KOSGEB Duyuruları");
        assertThat(SiteType.valueOf("EBELGE_GIB")).isEqualTo(SiteType.EBELGE_GIB);

        assertThat(ScrapingStatus.SUCCESS.name()).isEqualTo("SUCCESS");
        assertThat(ScrapingStatus.valueOf("FAILED")).isEqualTo(ScrapingStatus.FAILED);
    }

    @Test
    @DisplayName("ResourceNotFoundException ve ScrapingException özel istisna sınıf testleri")
    void exception_Tests() {
        ResourceNotFoundException rnf = new ResourceNotFoundException("Not found");
        assertThat(rnf.getMessage()).isEqualTo("Not found");

        ScrapingException se1 = new ScrapingException("Scrape error");
        assertThat(se1.getMessage()).isEqualTo("Scrape error");

        RuntimeException cause = new RuntimeException("DB fail");
        ScrapingException se2 = new ScrapingException("Scrape error with cause", cause);
        assertThat(se2.getMessage()).isEqualTo("Scrape error with cause");
        assertThat(se2.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("DTO getter, setter ve builder testleri")
    void dto_Tests() {
        // ScrapedAnnouncementDto
        ScrapedAnnouncementDto scraped = ScrapedAnnouncementDto.builder()
                .title("Title")
                .content("Content")
                .announcementDate(LocalDate.now())
                .sourceUrl("url")
                .attachmentUrl("att")
                .imageUrl("img")
                .contentHash("hash")
                .sourceSite(SiteType.EBELGE_GIB)
                .build();
        assertThat(scraped.getTitle()).isEqualTo("Title");

        // SubscriberRequestDto
        SubscriberRequestDto subReq = SubscriberRequestDto.builder()
                .email("a@b.com")
                .fullName("Full")
                .subscribedSites(Set.of(SiteType.KOSGEB))
                .build();
        assertThat(subReq.getEmail()).isEqualTo("a@b.com");

        // ApiResponseDto
        ApiResponseDto<String> successResp = ApiResponseDto.ok("OK", "Data");
        assertThat(successResp.isSuccess()).isTrue();
        assertThat(successResp.getMessage()).isEqualTo("OK");
        assertThat(successResp.getData()).isEqualTo("Data");

        ApiResponseDto<String> okNoData = ApiResponseDto.ok("Success");
        assertThat(okNoData.isSuccess()).isTrue();

        ApiResponseDto<String> errResp = ApiResponseDto.error("Fail");
        assertThat(errResp.isSuccess()).isFalse();

        // LoginResponseDto
        LoginResponseDto loginRes = LoginResponseDto.builder()
                .token("tok")
                .username("admin")
                .fullName("Super Admin")
                .build();
        assertThat(loginRes.getToken()).isEqualTo("tok");

        // ScrapeSettingsDto
        ScrapeSettingsDto settingsDto = ScrapeSettingsDto.builder()
                .enabled(true)
                .intervalHours(2)
                .intervalMinutes(120)
                .build();
        assertThat(settingsDto.isEnabled()).isTrue();
        assertThat(settingsDto.getIntervalHours()).isEqualTo(2);

        // SiteDto
        SiteDto siteDto = SiteDto.builder()
                .name("EBELGE_GIB")
                .displayName("GİB")
                .baseUrl("http://gib")
                .build();
        assertThat(siteDto.getName()).isEqualTo("EBELGE_GIB");

        // SubscriberResponseDto
        SubscriberResponseDto subRes = SubscriberResponseDto.builder()
                .id(1L)
                .email("test@mail.com")
                .active(true)
                .build();
        assertThat(subRes.getId()).isEqualTo(1L);

        // AdminUserDto
        AdminUserDto adminUserDto = AdminUserDto.builder()
                .id(1L)
                .username("admin")
                .build();
        assertThat(adminUserDto.getUsername()).isEqualTo("admin");

        // AnnouncementResponseDto
        AnnouncementResponseDto annRes = AnnouncementResponseDto.builder()
                .id(5L)
                .title("T")
                .build();
        assertThat(annRes.getId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Varlık (Entity) sınıflarının getter ve setter testleri")
    void entity_Tests() {
        // AdminUser
        AdminUser admin = AdminUser.builder()
                .id(1L)
                .username("admin")
                .fullName("Admin")
                .lastLoginAt(LocalDateTime.now())
                .build();
        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getLastLoginAt()).isNotNull();

        // Announcement
        Announcement ann = Announcement.builder()
                .id(10L)
                .title("Title")
                .content("Content")
                .sourceSite(SiteType.EBELGE_GIB)
                .createdAt(LocalDateTime.now())
                .build();
        assertThat(ann.getId()).isEqualTo(10L);
        assertThat(ann.getCreatedAt()).isNotNull();

        // Subscriber
        Subscriber sub = Subscriber.builder()
                .id(2L)
                .email("e@m.com")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        assertThat(sub.getEmail()).isEqualTo("e@m.com");
        assertThat(sub.getCreatedAt()).isNotNull();

        // SystemSetting
        SystemSetting setting = SystemSetting.builder()
                .settingKey("key")
                .settingValue("val")
                .updatedAt(LocalDateTime.now())
                .build();
        assertThat(setting.getSettingKey()).isEqualTo("key");
        assertThat(setting.getUpdatedAt()).isNotNull();
    }
}
