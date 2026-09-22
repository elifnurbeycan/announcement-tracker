package com.yasarbilgi.announcementtracker.repository;

import com.yasarbilgi.announcementtracker.entity.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

    Optional<Subscriber> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Subscriber> findByActiveTrue();
}
