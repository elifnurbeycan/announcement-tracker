package com.yasarbilgi.announcementtracker.service;

import com.yasarbilgi.announcementtracker.entity.Announcement;
import com.yasarbilgi.announcementtracker.entity.EmailDelivery;
import com.yasarbilgi.announcementtracker.enums.EmailDeliveryStatus;
import com.yasarbilgi.announcementtracker.repository.AnnouncementRepository;
import com.yasarbilgi.announcementtracker.repository.EmailDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxService {

    private final EmailDeliveryRepository deliveryRepository;
    private final AnnouncementRepository announcementRepository;

    @Value("${announcement.tracker.email.outbox.max-attempts:8}")
    private int maxAttempts;

    @Transactional
    public boolean enqueue(Announcement announcement, String recipientEmail) {
        String normalizedEmail = recipientEmail.trim().toLowerCase(Locale.ROOT);
        if (deliveryRepository.existsByAnnouncementIdAndRecipientEmailIgnoreCase(
                announcement.getId(), normalizedEmail)) {
            return false;
        }

        deliveryRepository.save(EmailDelivery.builder()
                .announcement(announcement)
                .recipientEmail(normalizedEmail)
                .status(EmailDeliveryStatus.PENDING)
                .nextAttemptAt(LocalDateTime.now())
                .build());
        return true;
    }

    @Transactional
    public List<Long> claimDueDeliveries() {
        LocalDateTime now = LocalDateTime.now();
        List<EmailDelivery> due = deliveryRepository
                .findTop100ByStatusInAndNextAttemptAtLessThanEqualAndAttemptCountLessThanOrderByCreatedAtAsc(
                        List.of(EmailDeliveryStatus.PENDING, EmailDeliveryStatus.FAILED), now, maxAttempts);
        due.forEach(delivery -> {
            delivery.setStatus(EmailDeliveryStatus.SENDING);
            delivery.setAttemptCount(delivery.getAttemptCount() + 1);
            delivery.setProcessingStartedAt(now);
            delivery.setNextAttemptAt(null);
        });
        deliveryRepository.saveAll(due);
        return due.stream().map(EmailDelivery::getId).toList();
    }

    @Transactional(readOnly = true)
    public DeliveryPayload getPayload(Long deliveryId) {
        EmailDelivery delivery = deliveryRepository.findWithAnnouncementById(deliveryId)
                .orElseThrow(() -> new IllegalStateException("Outbox delivery not found: " + deliveryId));
        return new DeliveryPayload(
                delivery.getId(), delivery.getAnnouncement(), delivery.getRecipientEmail());
    }

    @Transactional
    public void markSent(Long deliveryId) {
        EmailDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalStateException("Outbox delivery not found: " + deliveryId));
        delivery.setStatus(EmailDeliveryStatus.SENT);
        delivery.setSentAt(LocalDateTime.now());
        delivery.setProcessingStartedAt(null);
        delivery.setLastError(null);
        deliveryRepository.save(delivery);
        updateAnnouncementCompletion(delivery.getAnnouncement().getId());
    }

    @Transactional
    public void markFailed(Long deliveryId, Exception exception) {
        EmailDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalStateException("Outbox delivery not found: " + deliveryId));
        delivery.setStatus(EmailDeliveryStatus.FAILED);
        delivery.setProcessingStartedAt(null);
        delivery.setLastError(safeError(exception));
        if (delivery.getAttemptCount() < maxAttempts) {
            long delayMinutes = Math.min(360, 1L << Math.min(delivery.getAttemptCount(), 8));
            delivery.setNextAttemptAt(LocalDateTime.now().plusMinutes(delayMinutes));
        } else {
            delivery.setStatus(EmailDeliveryStatus.DEAD);
            delivery.setNextAttemptAt(null);
            log.error("E-posta teslimatı kalıcı olarak başarısız oldu. deliveryId={}, recipient={}, attempts={}",
                    delivery.getId(), delivery.getRecipientEmail(), delivery.getAttemptCount());
        }
        deliveryRepository.save(delivery);
        if (delivery.getStatus() == EmailDeliveryStatus.DEAD) {
            updateAnnouncementCompletion(delivery.getAnnouncement().getId());
        }
    }

    @Transactional
    public int recoverStaleDeliveries() {
        LocalDateTime now = LocalDateTime.now();
        return deliveryRepository.recoverStaleDeliveries(
                EmailDeliveryStatus.SENDING,
                EmailDeliveryStatus.FAILED,
                now.minusMinutes(10),
                now,
                "Önceki gönderim işlemi tamamlanmadan kesildi; yeniden denenecek.");
    }

    private void updateAnnouncementCompletion(Long announcementId) {
        long deliveryCount = deliveryRepository.countByAnnouncementId(announcementId);
        long unfinishedCount = deliveryRepository.countByAnnouncementIdAndStatusNotIn(
                announcementId, List.of(EmailDeliveryStatus.SENT, EmailDeliveryStatus.DEAD));
        if (deliveryCount == 0 || unfinishedCount != 0) {
            return;
        }

        announcementRepository.findById(announcementId).ifPresent(announcement -> {
            announcement.setNotified(true);
            announcement.setNotifiedAt(LocalDateTime.now());
            announcementRepository.save(announcement);
        });
    }

    private String safeError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        message = message
                .replaceAll("(?i)(password|secret|token|authorization)\\s*[:=]\\s*[^\\s,;]+", "$1=[REDACTED]")
                .replaceAll("(?i)(smtps?://)[^/@\\s]+@", "$1[REDACTED]@");
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    public record DeliveryPayload(Long deliveryId, Announcement announcement, String recipientEmail) {
    }
}
