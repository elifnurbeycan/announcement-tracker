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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Subscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String fullName;

    @Builder.Default
    private boolean active = true;

    @ElementCollection(targetClass = SiteType.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "subscriber_site_preferences", joinColumns = @JoinColumn(name = "subscriber_id"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Enumerated(EnumType.STRING)
    @Column(name = "site_type")
    @Builder.Default
    private Set<SiteType> subscribedSites = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
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
}
