package com.yasarbilgi.announcementtracker.entity;

import com.yasarbilgi.announcementtracker.enums.SiteType;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.Hibernate;
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

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(name = "keycloak_subject", unique = true, length = 255)
    private String keycloakSubject;

    @Builder.Default
    private boolean active = true;

    @ElementCollection(targetClass = SiteType.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "subscriber_site_preferences", joinColumns = @JoinColumn(name = "subscriber_id"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Enumerated(EnumType.STRING)
    @Column(name = "site_type")
    @Builder.Default
    private Set<SiteType> subscribedSites = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "subscriber_departments",
        joinColumns = @JoinColumn(name = "subscriber_id"),
        inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    @Builder.Default
    private Set<Department> departments = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.subscribedSites == null) {
            this.subscribedSites = new HashSet<>(Arrays.asList(SiteType.values()));
        }
        if (this.departments == null) {
            this.departments = new HashSet<>();
        }
    }

    public boolean isGeneralEmployee() {
        return this.departments == null || this.departments.isEmpty();
    }

    public Set<SiteType> getEffectiveSites(Set<SiteType> allAvailableSites) {
        if (isGeneralEmployee()) {
            return new HashSet<>(allAvailableSites != null ? allAvailableSites : Arrays.asList(SiteType.values()));
        }
        Set<SiteType> effective = new HashSet<>();
        if (this.subscribedSites != null) {
            effective.addAll(this.subscribedSites);
        }
        for (Department dept : this.departments) {
            if (dept != null && dept.getSites() != null) {
                effective.addAll(dept.getSites());
            }
        }
        return effective;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        Subscriber that = (Subscriber) other;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
