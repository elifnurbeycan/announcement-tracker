package com.yasarbilgi.announcementtracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxWorker {

    private final NotificationOutboxService outboxService;
    private final EmailService emailService;

    @Scheduled(
            initialDelayString = "${announcement.tracker.email.outbox.initial-delay-ms:10000}",
            fixedDelayString = "${announcement.tracker.email.outbox.poll-interval-ms:30000}")
    public void deliverPendingEmails() {
        int recovered = outboxService.recoverStaleDeliveries();
        if (recovered > 0) {
            log.warn("{} adet yarım kalmış e-posta teslimatı yeniden kuyruğa alındı.", recovered);
        }

        List<Long> deliveryIds = outboxService.claimDueDeliveries();
        for (Long deliveryId : deliveryIds) {
            try {
                NotificationOutboxService.DeliveryPayload payload = outboxService.getPayload(deliveryId);
                emailService.sendAnnouncementNotificationNow(
                        List.of(payload.announcement()), payload.recipientEmail());
                outboxService.markSent(deliveryId);
            } catch (Exception exception) {
                log.error("Outbox e-posta teslimatı başarısız oldu. deliveryId={}: {}",
                        deliveryId, exception.getMessage());
                outboxService.markFailed(deliveryId, exception);
            }
        }
    }
}
