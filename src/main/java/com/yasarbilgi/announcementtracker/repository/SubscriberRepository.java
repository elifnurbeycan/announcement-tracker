package com.yasarbilgi.announcementtracker.repository;

import com.yasarbilgi.announcementtracker.entity.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

    @EntityGraph(attributePaths = {"subscribedSites", "departments", "departments.sites"})
    Optional<Subscriber> findByEmail(String email);

    @EntityGraph(attributePaths = {"subscribedSites", "departments", "departments.sites"})
    Optional<Subscriber> findByKeycloakSubject(String keycloakSubject);

    @Override
    @EntityGraph(attributePaths = {"subscribedSites", "departments", "departments.sites"})
    Optional<Subscriber> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"subscribedSites", "departments", "departments.sites"})
    List<Subscriber> findAll();

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"subscribedSites", "departments", "departments.sites"})
    List<Subscriber> findByActiveTrue();
}
