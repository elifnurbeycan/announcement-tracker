package com.yasarbilgi.announcementtracker.notification.service;

import com.yasarbilgi.announcementtracker.entity.Announcement;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.service.impl.EmailServiceImpl;
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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailServiceImpl emailService;

    private Announcement announcement;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "mailFrom", "noreply@test.com");
        ReflectionTestUtils.setField(emailService, "appBaseUrl", "http://localhost:8080");

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
        emailService.sendAnnouncementNotification(List.of(), List.of("user@test.com"));

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

        emailService.sendAnnouncementNotification(List.of(announcement), List.of("user@test.com"));

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("Tekil duyuru bildirimi gönderme")
    void sendSingleAnnouncementNotification_ValidInput_ShouldSendEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendSingleAnnouncementNotification(announcement, List.of("user@test.com"));

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("Mail sunucu hatası meydana geldiğinde uygulama çökmemeli (Exception handled)")
    void sendAnnouncementNotification_MailException_ShouldCatchAndLog() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP Server Unreachable")).when(mailSender).send(mimeMessage);

        // Should not throw exception to caller
        emailService.sendAnnouncementNotification(List.of(announcement), List.of("user@test.com"));

        verify(mailSender).send(mimeMessage);
    }
}
