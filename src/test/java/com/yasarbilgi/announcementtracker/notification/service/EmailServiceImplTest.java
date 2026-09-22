package com.yasarbilgi.announcementtracker.notification.service;

import com.yasarbilgi.announcementtracker.entity.Announcement;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.service.impl.EmailServiceImpl;
import com.yasarbilgi.announcementtracker.service.UnsubscribeTokenService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @Mock
    private UnsubscribeTokenService unsubscribeTokenService;

    @InjectMocks
    private EmailServiceImpl emailService;

    private Announcement announcement;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "mailFrom", "noreply@company.com");
        ReflectionTestUtils.setField(emailService, "appBaseUrl", "http://localhost:8080");
        lenient().when(unsubscribeTokenService.generate(anyString())).thenReturn("test-unsubscribe-token");

        announcement = Announcement.builder()
                .id(1L)
                .title("Test Duyurusu")
                .content("Test İçeriği")
                .sourceSite(SiteType.EBELGE_GIB)
                .announcementDate(LocalDate.now())
                .build();
    }

    @Test
    @DisplayName("Duyuru listesi boş olduğunda e-posta gönderimi tetiklenmemeli")
    void sendAnnouncementNotification_EmptyAnnouncements_ShouldNotSendEmail() {
        emailService.sendAnnouncementNotification(List.of(), List.of("user@realcompany.com"));

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Alıcı listesi boş olduğunda e-posta gönderimi tetiklenmemeli")
    void sendAnnouncementNotification_EmptyRecipients_ShouldNotSendEmail() {
        emailService.sendAnnouncementNotification(List.of(announcement), List.of());

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Geçerli duyuru ve alıcı e-postası ile MimeMessage oluşturulup gönderilmeli")
    void sendAnnouncementNotification_ValidInput_ShouldSendEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendAnnouncementNotification(List.of(announcement), List.of("user@realcompany.com"));

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("Tekil duyuru bildirimi gönderme")
    void sendSingleAnnouncementNotification_ValidInput_ShouldSendEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendSingleAnnouncementNotification(announcement, List.of("user@realcompany.com"));

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("Mail sunucu hatası meydana geldiğinde uygulama çökmemeli (Exception handled)")
    void sendAnnouncementNotification_MailException_ShouldCatchAndLog() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP Server Unreachable")).when(mailSender).send(mimeMessage);

        // Should not throw exception to caller
        emailService.sendAnnouncementNotification(List.of(announcement), List.of("user@realcompany.com"));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Sahte/Test domain e-postalarında SMTP gönderimi atlanmalı (Bounce önleme)")
    void sendAnnouncementNotification_DummyRecipient_ShouldSkipSmtpSend() {
        emailService.sendAnnouncementNotification(List.of(announcement), List.of("e2e-sub-12345@kurum.com", "user@example.com"));

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Sahte/Test hoş geldin e-postalarında SMTP gönderimi atlanmalı")
    void sendWelcomeAndActivationEmail_DummyRecipient_ShouldSkipSmtpSend() {
        emailService.sendWelcomeAndActivationEmail("e2e-sub-86fbf9d5@kurum.com", "Test User", "token123");

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Taklit/Tağşiş e-postası ham metin yerine okunabilir bilgi tablosu içermeli")
    void buildHtmlEmailBody_TaklitTagsis_ShouldRenderStructuredDetails() {
        Announcement taklitAnnouncement = Announcement.builder()
                .title("[Taklit/Tağşiş-2] Örnek uzun başlık")
                .content("Liste: Taklit/Tağşiş-2 | Firma: ÖRNEK GIDA A.Ş. | Marka: GÜVEN | "
                        + "Ürün: TAM YAĞLI BEYAZ PEYNİR | Uygunsuzluk: Yağ Oranının Düşük Olması | "
                        + "Parti/Seri No: P-123 | İl/İlçe: BURSA/NİLÜFER | Ürün Grubu: Süt ve Süt Ürünleri")
                .sourceSite(SiteType.TAKLIT_TAGSIS)
                .sourceUrl("https://guvenilirgida.tarimorman.gov.tr/ornek")
                .announcementDate(LocalDate.of(2026, 4, 2))
                .build();

        String html = ReflectionTestUtils.invokeMethod(
                emailService,
                "buildHtmlEmailBody",
                List.of(taklitAnnouncement),
                "user@realcompany.com"
        );

        assertThat(html)
                .contains("Tarım ve Orman Bakanlığı kamuoyu duyuruları")
                .contains("Kamuoyu Duyuru Tarihi", "02.04.2026")
                .contains("Firma Adı", "ÖRNEK GIDA A.Ş.")
                .contains("Ürün Adı", "TAM YAĞLI BEYAZ PEYNİR")
                .contains("Uygunsuzluk", "Yağ Oranının Düşük Olması")
                .contains("Parti / Seri No", "P-123")
                .contains("Resmî kaydı görüntüle")
                .doesNotContain("Liste: Taklit/Tağşiş-2 | Firma:");
    }

    @Test
    @DisplayName("Duyuru alanları HTML olarak çalıştırılmamalı")
    void buildHtmlEmailBody_ShouldEscapeAnnouncementValues() {
        Announcement unsafeAnnouncement = Announcement.builder()
                .title("<script>alert('x')</script>")
                .content("<b>İçerik</b>")
                .sourceSite(SiteType.EBELGE_GIB)
                .announcementDate(LocalDate.now())
                .build();

        String html = ReflectionTestUtils.invokeMethod(
                emailService,
                "buildHtmlEmailBody",
                List.of(unsafeAnnouncement),
                "user@realcompany.com"
        );

        assertThat(html)
                .contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;")
                .contains("&lt;b&gt;İçerik&lt;/b&gt;")
                .doesNotContain("<script>alert('x')</script>");
    }
}
