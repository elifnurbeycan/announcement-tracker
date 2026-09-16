package com.yasarbilgi.announcementtracker.entity;

import com.yasarbilgi.announcementtracker.enums.SiteType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @ElementCollection(targetClass = SiteType.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "department_sites", joinColumns = @JoinColumn(name = "department_id"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Enumerated(EnumType.STRING)
    @Column(name = "site_type")
    @Builder.Default
    private Set<SiteType> sites = new HashSet<>();

    @ManyToMany(mappedBy = "departments", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Subscriber> subscribers = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.sites == null) {
            this.sites = new HashSet<>();
        }
        if (this.subscribers == null) {
            this.subscribers = new HashSet<>();
        }
    }
}
