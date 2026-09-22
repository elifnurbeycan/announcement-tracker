package com.yasarbilgi.announcementtracker.subscriber.service;

import com.yasarbilgi.announcementtracker.dto.request.SubscriberRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.SubscriberResponseDto;
import com.yasarbilgi.announcementtracker.entity.Announcement;
import com.yasarbilgi.announcementtracker.entity.Department;
import com.yasarbilgi.announcementtracker.entity.Subscriber;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.exception.ResourceNotFoundException;
import com.yasarbilgi.announcementtracker.exception.ScrapingException;
import com.yasarbilgi.announcementtracker.repository.AnnouncementRepository;
import com.yasarbilgi.announcementtracker.repository.DepartmentRepository;
import com.yasarbilgi.announcementtracker.repository.SubscriberRepository;
import com.yasarbilgi.announcementtracker.service.EmailService;
import com.yasarbilgi.announcementtracker.service.KeycloakAdminService;
import com.yasarbilgi.announcementtracker.service.UnsubscribeTokenService;
import com.yasarbilgi.announcementtracker.service.impl.SubscriberServiceImpl;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriberServiceImplTest {

    @Mock
    private SubscriberRepository subscriberRepository;

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private KeycloakAdminService keycloakAdminService;

    @Mock
    private UnsubscribeTokenService unsubscribeTokenService;

    @InjectMocks
    private SubscriberServiceImpl subscriberService;

    private Subscriber subscriber;

    @BeforeEach
    void setUp() {
        subscriber = Subscriber.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .active(true)
                .subscribedSites(Set.of(SiteType.EBELGE_GIB, SiteType.KOSGEB))
                .build();
    }

    @Test
    @DisplayName("Yeni e-posta abonesi ekleme ve karşılama bildirimi gönderme")
    void addSubscriber_NewEmail_ShouldSaveAndSendNotification() {
        SubscriberRequestDto dto = SubscriberRequestDto.builder()
                .email("test@example.com")
                .fullName("Test User")
                .subscribedSites(Set.of(SiteType.EBELGE_GIB))
                .build();

        when(subscriberRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(subscriberRepository.save(any(Subscriber.class))).thenReturn(subscriber);

        SubscriberResponseDto response = subscriberService.addSubscriber(dto);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");

        verify(subscriberRepository, times(1)).save(any(Subscriber.class));
        verify(keycloakAdminService).provisionSubscriber("test@example.com", "Test User", true);
        verify(emailService).sendWelcomeAndActivationEmail(eq("test@example.com"), eq("Test User"), anyString());
    }

    @Test
    @DisplayName("Null tercihlerle yeni abone eklendiğinde tüm sitelere varsayılan olarak abone olmalı")
    void addSubscriber_NullSites_ShouldDefaultToAllSites() {
        SubscriberRequestDto dto = SubscriberRequestDto.builder()
                .email("test@example.com")
                .fullName("Test User")
                .subscribedSites(null)
                .build();

        when(subscriberRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(subscriberRepository.save(any(Subscriber.class))).thenAnswer(i -> i.getArgument(0));

        SubscriberResponseDto response = subscriberService.addSubscriber(dto);

        assertThat(response.getSubscribedSites()).contains(SiteType.EBELGE_GIB, SiteType.KOSGEB);
    }

    @Test
    @DisplayName("Departmanlı yeni abone eklenirken ek site seçilmediğinde tüm siteler değil sadece departman kaynakları efektif olmalı")
    void addSubscriber_DepartmentAssigned_NullExtraSites_ShouldNotDefaultToAllSites() {
        Department dept = Department.builder().id(10L).name("IT").sites(Set.of(SiteType.EBELGE_GIB)).build();
        SubscriberRequestDto dto = SubscriberRequestDto.builder()
                .email("deptuser@example.com")
                .fullName("Dept User")
                .departmentIds(Set.of(10L))
                .subscribedSites(null)
                .build();

        when(subscriberRepository.existsByEmail("deptuser@example.com")).thenReturn(false);
        when(departmentRepository.findAllById(Set.of(10L))).thenReturn(List.of(dept));
        when(subscriberRepository.save(any(Subscriber.class))).thenAnswer(i -> i.getArgument(0));

        SubscriberResponseDto response = subscriberService.addSubscriber(dto);

        assertThat(response.getSubscribedSites()).isEmpty();
        assertThat(response.getEffectiveSites()).containsExactly(SiteType.EBELGE_GIB);
    }

    @Test
    @DisplayName("Karşılama e-postası atılırken hata çıkarsa işlem çökmemeli")
    void addSubscriber_EmailException_ShouldNotFailRegistration() {
        SubscriberRequestDto dto = SubscriberRequestDto.builder()
                .email("test@example.com")
                .fullName("Test User")
                .subscribedSites(Set.of(SiteType.EBELGE_GIB))
                .build();

        when(subscriberRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(subscriberRepository.save(any(Subscriber.class))).thenReturn(subscriber);
        doThrow(new RuntimeException("Mail server down"))
                .when(emailService).sendWelcomeAndActivationEmail(anyString(), anyString(), anyString());

        SubscriberResponseDto response = subscriberService.addSubscriber(dto);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Var olan abonenin site tercihlerini güncelleme")
    void addSubscriber_ExistingEmail_ShouldUpdatePreferencesAndReactivate() {
        subscriber.setActive(false);

        SubscriberRequestDto dto = SubscriberRequestDto.builder()
                .email("test@example.com")
                .fullName("Test User")
                .subscribedSites(Set.of(SiteType.KOSGEB))
                .build();

        when(subscriberRepository.existsByEmail("test@example.com")).thenReturn(true);
        when(subscriberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(subscriber));
        when(subscriberRepository.save(any(Subscriber.class))).thenReturn(subscriber);

        SubscriberResponseDto response = subscriberService.addSubscriber(dto);

        assertThat(response).isNotNull();
        assertThat(subscriber.isActive()).isTrue();
        assertThat(subscriber.getSubscribedSites()).contains(SiteType.KOSGEB);
        verify(subscriberRepository, times(1)).save(subscriber);
    }

    @Test
    @DisplayName("Tüm aboneleri listeleme")
    void getAllSubscribers_ShouldReturnSubscriberList() {
        when(subscriberRepository.findAll()).thenReturn(List.of(subscriber));

        List<SubscriberResponseDto> list = subscriberService.getAllSubscribers();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Abone site bildirim tercihlerini güncelleme")
    void updateSitePreferences_ShouldModifyPreferences() {
        when(subscriberRepository.findById(1L)).thenReturn(Optional.of(subscriber));
        when(subscriberRepository.save(any(Subscriber.class))).thenReturn(subscriber);

        Set<SiteType> newSites = Set.of(SiteType.KOSGEB);
        SubscriberResponseDto dto = subscriberService.updateSitePreferences(1L, newSites);

        assertThat(dto).isNotNull();
        verify(subscriberRepository).save(subscriber);
        assertThat(subscriber.getSubscribedSites()).isEqualTo(newSites);
    }

    @Test
    @DisplayName("Kişisel ek tercihler boşaltılsa bile departman siteleri efektif kapsamda kalmalı")
    void updateSitePreferences_EmptyAdditionalSites_ShouldKeepDepartmentSitesEffective() {
        Department department = Department.builder()
                .id(10L)
                .name("Java")
                .sites(Set.of(SiteType.EBELGE_GIB))
                .build();
        subscriber.setDepartments(Set.of(department));

        when(subscriberRepository.findById(1L)).thenReturn(Optional.of(subscriber));
        when(subscriberRepository.save(any(Subscriber.class))).thenReturn(subscriber);

        SubscriberResponseDto dto = subscriberService.updateSitePreferences(1L, Set.of());

        assertThat(subscriber.getSubscribedSites()).isEmpty();
        assertThat(dto.getDepartmentSites()).containsExactly(SiteType.EBELGE_GIB);
        assertThat(dto.getEffectiveSites()).containsExactly(SiteType.EBELGE_GIB);
    }

    @Test
    @DisplayName("Olmayan abone ID'si ile tercih güncellenirse ResourceNotFoundException fırlatmalı")
    void updateSitePreferences_NotFound_ShouldThrowException() {
        when(subscriberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriberService.updateSitePreferences(999L, Set.of(SiteType.KOSGEB)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Subscriber not found with ID: 999");
    }

    @Test
    @DisplayName("Abone durumunu aktif/pasif (iptal) yapma")
    void toggleSubscriberStatus_ShouldUpdateStatus() {
        when(subscriberRepository.findById(1L)).thenReturn(Optional.of(subscriber));

        subscriberService.toggleSubscriberStatus(1L, false);

        assertThat(subscriber.isActive()).isFalse();
        verify(subscriberRepository).save(subscriber);
        verify(keycloakAdminService).provisionSubscriber("test@example.com", "Test User", false);
    }

    @Test
    @DisplayName("Olmayan abone ID'si ile durum değiştirilirse ResourceNotFoundException fırlatmalı")
    void toggleSubscriberStatus_NotFound_ShouldThrowException() {
        when(subscriberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriberService.toggleSubscriberStatus(999L, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Aboneyi veritabanından tamamen silme")
    void deleteSubscriber_ShouldDeleteEntity() {
        when(subscriberRepository.findById(1L)).thenReturn(Optional.of(subscriber));

        subscriberService.deleteSubscriber(1L);

        verify(keycloakAdminService).deleteUserInKeycloak("test@example.com");
        verify(subscriberRepository).delete(subscriber);
    }

    @Test
    @DisplayName("Olmayan abone ID'si ile silme denenirse ResourceNotFoundException fırlatmalı")
    void deleteSubscriber_NotFound_ShouldThrowException() {
        when(subscriberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriberService.deleteSubscriber(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("E-posta ile abonelik iptal etme")
    void unsubscribeByEmail_ValidEmail_ShouldDeactivate() {
        when(subscriberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(subscriber));

        boolean result = subscriberService.unsubscribeByEmail("test@example.com");

        assertThat(result).isTrue();
        assertThat(subscriber.isActive()).isFalse();
        verify(subscriberRepository).save(subscriber);
    }

    @Test
    @DisplayName("Aktif aboneye güvenli Keycloak şifre bağlantısı gönderme")
    void sendPasswordSetupEmail_ActiveSubscriber_ShouldProvisionAndTriggerEmail() {
        when(subscriberRepository.findById(1L)).thenReturn(Optional.of(subscriber));

        subscriberService.sendPasswordSetupEmail(1L);

        verify(keycloakAdminService).provisionSubscriber("test@example.com", "Test User", true);
        verify(keycloakAdminService).triggerKeycloakResetPasswordEmail("test@example.com");
    }

    @Test
    @DisplayName("Token ile güvenli abonelik iptal etme")
    void unsubscribeByToken_ValidToken_ShouldDeactivate() {
        when(subscriberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(subscriber));
        when(unsubscribeTokenService.generate("test@example.com")).thenReturn("valid-token");
        when(unsubscribeTokenService.verifyAndExtractEmail("valid-token")).thenReturn(Optional.of("test@example.com"));

        String token = subscriberService.generateUnsubscribeToken("test@example.com");
        assertThat(token).isNotBlank();

        boolean result = subscriberService.unsubscribeByToken(token);

        assertThat(result).isTrue();
        assertThat(subscriber.isActive()).isFalse();
    }

    @Test
    @DisplayName("Geçersiz token ile abonelik iptali false dönmeli")
    void unsubscribeByToken_InvalidToken_ShouldReturnFalse() {
        when(unsubscribeTokenService.verifyAndExtractEmail("invalid-token-123")).thenReturn(Optional.empty());
        boolean result = subscriberService.unsubscribeByToken("invalid-token-123");
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Geçersiz e-posta ile abonelik iptali false dönmeli")
    void unsubscribeByEmail_InvalidEmail_ShouldReturnFalse() {
        boolean result = subscriberService.unsubscribeByEmail("nonexistent@example.com");
        assertThat(result).isFalse();

        boolean nullResult = subscriberService.unsubscribeByEmail(null);
        assertThat(nullResult).isFalse();
    }

    @Test
    @DisplayName("CSV dosyasından aboneleri başarıyla içe aktarma")
    void importSubscribersFromExcel_ValidCsv_ShouldImportSubscribers() {
        String csvContent = "E-Posta;Ad Soyad\nabone1@kurum.com;Test Abone 1\nabone2@kurum.com;Test Abone 2";
        MockMultipartFile file = new MockMultipartFile("file", "subscribers.csv", "text/csv", csvContent.getBytes());

        when(subscriberRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        int importedCount = subscriberService.importSubscribersFromExcel(file, Set.of(SiteType.EBELGE_GIB));

        assertThat(importedCount).isEqualTo(2);
        verify(subscriberRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Gerçek Excel (.xlsx) dosyasından aboneleri ayrıştırıp kaydetme")
    void importSubscribersFromExcel_ValidXlsx_ShouldParseAndSave() throws IOException {
        org.apache.poi.ss.usermodel.Workbook workbook = new XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet();

        // Header
        org.apache.poi.ss.usermodel.Row row0 = sheet.createRow(0);
        row0.createCell(0).setCellValue("E-posta");
        row0.createCell(1).setCellValue("Ad Soyad");

        // Row 1
        org.apache.poi.ss.usermodel.Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("exceluser@test.com");
        row1.createCell(1).setCellValue("Excel Abonesi");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();

        MockMultipartFile excelFile = new MockMultipartFile(
                "file",
                "subscribers.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                baos.toByteArray()
        );

        when(subscriberRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        int count = subscriberService.importSubscribersFromExcel(excelFile);

        assertThat(count).isEqualTo(1);
        verify(subscriberRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Boş dosya yüklendiğinde IllegalArgumentException fırlatmalı")
    void importSubscribersFromExcel_EmptyFile_ShouldThrowException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "text/plain", new byte[0]);

        assertThatThrownBy(() -> subscriberService.importSubscribersFromExcel(emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boş olamaz");
    }

    @Test
    @DisplayName("E-posta başka Keycloak kimliğine bağlıysa kullanıcı eşlemesi değiştirilmemeli")
    void createOidcSession_ExistingSubscriberWithDifferentSubject_ShouldRejectLogin() {
        subscriber.setKeycloakSubject("original-subject");
        when(subscriberRepository.findByKeycloakSubject("different-subject")).thenReturn(Optional.empty());
        when(subscriberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(subscriber));

        assertThatThrownBy(() -> subscriberService.createOidcSession("different-subject", "test@example.com"))
                .isInstanceOf(ScrapingException.class)
                .hasMessageContaining("farklı bir Keycloak kimliği");
    }
}
