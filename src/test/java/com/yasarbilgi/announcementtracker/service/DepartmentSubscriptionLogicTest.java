package com.yasarbilgi.announcementtracker.service;

import com.yasarbilgi.announcementtracker.entity.Department;
import com.yasarbilgi.announcementtracker.entity.Subscriber;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DepartmentSubscriptionLogicTest {

    private final Set<SiteType> allSites = new HashSet<>(Arrays.asList(SiteType.values()));

    @Test
    @DisplayName("Senaryo 1 — Tek departman dinamik güncelleme")
    void scenario1_singleDepartmentDynamicUpdate() {
        Department javaDept = Department.builder()
                .id(1L)
                .name("Java")
                .sites(new HashSet<>(Set.of(SiteType.EBELGE_GIB)))
                .build();

        Subscriber subscriber = Subscriber.builder()
                .id(100L)
                .email("subscriber@example.com")
                .departments(new HashSet<>(Set.of(javaDept)))
                .subscribedSites(new HashSet<>())
                .build();

        // 1. Initial calculation: Java -> EBELGE_GIB
        Set<SiteType> effective = subscriber.getEffectiveSites(allSites);
        assertEquals(Set.of(SiteType.EBELGE_GIB), effective);

        // 2. Admin updates Java -> EBELGE_GIB
        javaDept.setSites(new HashSet<>(Set.of(SiteType.EBELGE_GIB)));

        Set<SiteType> updatedEffective = subscriber.getEffectiveSites(allSites);
        assertEquals(Set.of(SiteType.EBELGE_GIB), updatedEffective);
    }

    @Test
    @DisplayName("Senaryo 2 — Birden fazla departman (Union)")
    void scenario2_multipleDepartmentsUnion() {
        Department javaDept = Department.builder()
                .id(1L)
                .name("Java")
                .sites(new HashSet<>(Set.of(SiteType.EBELGE_GIB)))
                .build();

        Department backendDept = Department.builder()
                .id(2L)
                .name("Backend")
                .sites(new HashSet<>(Set.of(SiteType.EBELGE_GIB)))
                .build();

        Subscriber subscriber = Subscriber.builder()
                .id(101L)
                .email("abone@kurum.com")
                .departments(new HashSet<>(Arrays.asList(javaDept, backendDept)))
                .subscribedSites(new HashSet<>())
                .build();

        Set<SiteType> effective = subscriber.getEffectiveSites(allSites);
        assertEquals(Set.of(SiteType.EBELGE_GIB), effective);
    }

    @Test
    @DisplayName("Senaryo 3 — Kişisel site tercihi ∪ Bağlı tüm departmanlar")
    void scenario3_personalPreferencesUnionDepartments() {
        Department javaDept = Department.builder()
                .id(1L)
                .name("Java")
                .sites(new HashSet<>(Set.of(SiteType.EBELGE_GIB)))
                .build();

        Subscriber subscriber = Subscriber.builder()
                .id(102L)
                .email("abone@kurum.com")
                .departments(new HashSet<>(Set.of(javaDept)))
                .subscribedSites(new HashSet<>(Set.of(SiteType.EBELGE_GIB)))
                .build();

        Set<SiteType> effective = subscriber.getEffectiveSites(allSites);
        assertEquals(Set.of(SiteType.EBELGE_GIB), effective);
    }

    @Test
    @DisplayName("Senaryo 4 — Duplicate sitelerin teke indirilmesi")
    void scenario4_duplicateSiteDeduplication() {
        Department javaDept = Department.builder()
                .id(1L)
                .name("Java")
                .sites(new HashSet<>(Set.of(SiteType.EBELGE_GIB)))
                .build();

        Department backendDept = Department.builder()
                .id(2L)
                .name("Backend")
                .sites(new HashSet<>(Set.of(SiteType.EBELGE_GIB)))
                .build();

        Subscriber subscriber = Subscriber.builder()
                .id(103L)
                .email("duplicate@example.com")
                .departments(new HashSet<>(Arrays.asList(javaDept, backendDept)))
                .subscribedSites(new HashSet<>(Set.of(SiteType.EBELGE_GIB)))
                .build();

        Set<SiteType> effective = subscriber.getEffectiveSites(allSites);
        assertEquals(1, effective.size());
        assertTrue(effective.contains(SiteType.EBELGE_GIB));
    }

    @Test
    @DisplayName("Senaryo 5 — Genel Çalışan (Tüm siteler & yeni site ekleme)")
    void scenario5_generalEmployeeAllSites() {
        Subscriber subscriber = Subscriber.builder()
                .id(104L)
                .email("general@example.com")
                .departments(new HashSet<>())
                .subscribedSites(new HashSet<>())
                .build();

        assertTrue(subscriber.isGeneralEmployee());
        Set<SiteType> effective = subscriber.getEffectiveSites(allSites);
        assertEquals(allSites, effective);
    }

    @Test
    @DisplayName("Senaryo 6 — Boş departman (0 site)")
    void scenario6_emptyDepartment() {
        Department emptyJavaDept = Department.builder()
                .id(1L)
                .name("Java")
                .sites(new HashSet<>())
                .build();

        Subscriber subscriber = Subscriber.builder()
                .id(105L)
                .email("emptydept@example.com")
                .departments(new HashSet<>(Set.of(emptyJavaDept)))
                .subscribedSites(new HashSet<>(Set.of(SiteType.EBELGE_GIB)))
                .build();

        Set<SiteType> effective = subscriber.getEffectiveSites(allSites);
        assertEquals(Set.of(SiteType.EBELGE_GIB), effective);

        emptyJavaDept.getSites().add(SiteType.EBELGE_GIB);

        Set<SiteType> updatedEffective = subscriber.getEffectiveSites(allSites);
        assertEquals(Set.of(SiteType.EBELGE_GIB), updatedEffective);
    }

    @Test
    @DisplayName("Senaryo 7 — Departman silme ve Genel Çalışan durumuna düşme")
    void scenario7_departmentDeletionFallbackToGeneralEmployee() {
        Department javaDept = Department.builder()
                .id(1L)
                .name("Java")
                .sites(new HashSet<>(Set.of(SiteType.EBELGE_GIB)))
                .build();

        Department backendDept = Department.builder()
                .id(2L)
                .name("Backend")
                .sites(new HashSet<>(Set.of(SiteType.EBELGE_GIB)))
                .build();

        Subscriber subscriber = Subscriber.builder()
                .id(106L)
                .email("delete@example.com")
                .departments(new HashSet<>(Arrays.asList(javaDept, backendDept)))
                .subscribedSites(new HashSet<>())
                .build();

        assertEquals(2, subscriber.getDepartments().size());
        assertFalse(subscriber.isGeneralEmployee());

        subscriber.getDepartments().remove(javaDept);
        assertEquals(1, subscriber.getDepartments().size());
        assertFalse(subscriber.isGeneralEmployee());
        assertEquals(Set.of(SiteType.EBELGE_GIB), subscriber.getEffectiveSites(allSites));

        subscriber.getDepartments().remove(backendDept);
        assertTrue(subscriber.getDepartments().isEmpty());
        assertTrue(subscriber.isGeneralEmployee());
        assertEquals(allSites, subscriber.getEffectiveSites(allSites));
    }
}
