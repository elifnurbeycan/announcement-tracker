package com.yasarbilgi.announcementtracker.repository;

import com.yasarbilgi.announcementtracker.entity.EmailDelivery;
import com.yasarbilgi.announcementtracker.enums.EmailDeliveryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.QueryHint;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface EmailDeliveryRepository extends JpaRepository<EmailDelivery, Long> {

    boolean existsByAnnouncementIdAndRecipientEmailIgnoreCase(Long announcementId, String recipientEmail);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<EmailDelivery> findTop100ByStatusInAndNextAttemptAtLessThanEqualAndAttemptCountLessThanOrderByCreatedAtAsc(
            Collection<EmailDeliveryStatus> statuses, LocalDateTime dueAt, int maxAttempts);

    @EntityGraph(attributePaths = "announcement")
    @Query("select delivery from EmailDelivery delivery where delivery.id = :id")
    java.util.Optional<EmailDelivery> findWithAnnouncementById(@Param("id") Long id);

    long countByAnnouncementId(Long announcementId);

    long countByAnnouncementIdAndStatusNotIn(Long announcementId, Collection<EmailDeliveryStatus> statuses);

    @Modifying
    @Query("""
            update EmailDelivery delivery
               set delivery.status = :failed,
                   delivery.nextAttemptAt = :retryAt,
                   delivery.processingStartedAt = null,
                   delivery.lastError = :reason
             where delivery.status = :sending
               and delivery.processingStartedAt < :staleBefore
            """)
    int recoverStaleDeliveries(
            @Param("sending") EmailDeliveryStatus sending,
            @Param("failed") EmailDeliveryStatus failed,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("retryAt") LocalDateTime retryAt,
            @Param("reason") String reason);
}
