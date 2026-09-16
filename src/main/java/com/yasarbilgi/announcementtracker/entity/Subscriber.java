package com.yasarbilgi.announcementtracker.entity;

import com.yasarbilgi.announcementtracker.enums.SiteType;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "subscribers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String fullName;

    private String passwordHash;

    private String activationToken;

    private LocalDateTime tokenExpiry;

    @Builder.Default
    private boolean active = true;

    @ElementCollection(targetClass = SiteType.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "subscriber_site_preferences", joinColumns = @JoinColumn(name = "subscriber_id"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Enumerated(EnumType.STRING)
    @Column(name = "site_type")
    @Builder.Default
    private Set<SiteType> subscribedSites = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.subscribedSites == null || this.subscribedSites.isEmpty()) {
            this.subscribedSites = new HashSet<>(Arrays.asList(SiteType.values()));
        }
    }
}
