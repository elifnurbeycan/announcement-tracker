package com.yasarbilgi.announcementtracker.service;

import com.yasarbilgi.announcementtracker.entity.Announcement;
import com.yasarbilgi.announcementtracker.entity.EmailDelivery;
import com.yasarbilgi.announcementtracker.enums.EmailDeliveryStatus;
import com.yasarbilgi.announcementtracker.repository.AnnouncementRepository;
import com.yasarbilgi.announcementtracker.repository.EmailDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxServiceTest {

    @Mock private EmailDeliveryRepository deliveryRepository;
    @Mock private AnnouncementRepository announcementRepository;
    @InjectMocks private NotificationOutboxService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
    }

    @Test
    void finalFailure_BecomesDeadAndCompletesAnnouncement() {
        Announcement announcement = Announcement.builder().id(10L).isNotified(false).build();
        EmailDelivery delivery = EmailDelivery.builder()
                .id(1L)
                .announcement(announcement)
                .recipientEmail("employee@example.com")
                .status(EmailDeliveryStatus.SENDING)
                .attemptCount(3)
                .build();

        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.countByAnnouncementId(10L)).thenReturn(1L);
        when(deliveryRepository.countByAnnouncementIdAndStatusNotIn(
                10L, List.of(EmailDeliveryStatus.SENT, EmailDeliveryStatus.DEAD))).thenReturn(0L);
        when(announcementRepository.findById(10L)).thenReturn(Optional.of(announcement));

        service.markFailed(1L, new RuntimeException("password=secret token=abc smtp://user:pass@mail.example"));

        assertThat(delivery.getStatus()).isEqualTo(EmailDeliveryStatus.DEAD);
        assertThat(delivery.getNextAttemptAt()).isNull();
        assertThat(delivery.getLastError()).contains("[REDACTED]").doesNotContain("secret", "abc", "user:pass");
        assertThat(announcement.isNotified()).isTrue();
        verify(announcementRepository).save(announcement);
    }

    @Test
    void payload_LoadsAnnouncementExplicitlyForLazyRelationship() {
        Announcement announcement = Announcement.builder().id(10L).title("Duyuru").build();
        EmailDelivery delivery = EmailDelivery.builder()
                .id(1L)
                .announcement(announcement)
                .recipientEmail("employee@example.com")
                .build();
        when(deliveryRepository.findWithAnnouncementById(1L)).thenReturn(Optional.of(delivery));

        NotificationOutboxService.DeliveryPayload payload = service.getPayload(1L);

        assertThat(payload.announcement().getTitle()).isEqualTo("Duyuru");
    }
}
