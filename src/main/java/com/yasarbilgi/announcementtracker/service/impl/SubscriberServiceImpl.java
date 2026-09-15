package com.yasarbilgi.announcementtracker.service.impl;

import com.yasarbilgi.announcementtracker.dto.request.SubscriberRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.SubscriberResponseDto;
import com.yasarbilgi.announcementtracker.entity.Subscriber;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.exception.ResourceNotFoundException;
import com.yasarbilgi.announcementtracker.repository.AnnouncementRepository;
import com.yasarbilgi.announcementtracker.repository.SubscriberRepository;
import com.yasarbilgi.announcementtracker.service.EmailService;
import com.yasarbilgi.announcementtracker.service.SubscriberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriberServiceImpl implements SubscriberService {

    private final SubscriberRepository subscriberRepository;
    private final AnnouncementRepository announcementRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public SubscriberResponseDto addSubscriber(SubscriberRequestDto dto) {
        Subscriber saved;
        Set<SiteType> preferredSites = (dto.getSubscribedSites() != null && !dto.getSubscribedSites().isEmpty())
                ? dto.getSubscribedSites()
                : new HashSet<>(Arrays.asList(SiteType.values()));

        if (subscriberRepository.existsByEmail(dto.getEmail())) {
            log.info("Subscriber email already exists: {}", dto.getEmail());
            saved = subscriberRepository.findByEmail(dto.getEmail()).orElseThrow();
            saved.setSubscribedSites(preferredSites);
            if (!saved.isActive()) {
                saved.setActive(true);
            }
            subscriberRepository.save(saved);
        } else {
            Subscriber subscriber = Subscriber.builder()
                    .email(dto.getEmail())
                    .fullName(dto.getFullName())
                    .subscribedSites(preferredSites)
                    .active(true)
                    .build();
            saved = subscriberRepository.save(subscriber);
            log.info("New subscriber registered: {} with preferences: {}", saved.getEmail(), preferredSites);
        }

        // Instantly dispatch the latest announcement to the new subscriber as a welcome notification if subscribed to its site
        try {
            var latest = announcementRepository.findAll().stream().findFirst();
            latest.ifPresent(announcement -> {
                if (saved.getSubscribedSites().contains(announcement.getSourceSite())) {
                    emailService.sendSingleAnnouncementNotification(announcement, List.of(saved.getEmail()));
                }
            });
        } catch (Exception e) {
            log.error("Failed to send welcome email notification to {}: {}", saved.getEmail(), e.getMessage());
        }

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriberResponseDto> getAllSubscribers() {
        return subscriberRepository.findAll().stream().map(this::mapToDto).toList();
    }

    @Override
    @Transactional
    public void deleteSubscriber(Long id) {
        Subscriber subscriber = subscriberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found with ID: " + id));
        subscriberRepository.delete(subscriber);
        log.info("Deleted subscriber with ID: {}", id);
    }

    @Override
    @Transactional
    public void toggleSubscriberStatus(Long id, boolean active) {
        Subscriber subscriber = subscriberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found with ID: " + id));
        subscriber.setActive(active);
        subscriberRepository.save(subscriber);
        log.info("Updated subscriber status ID: {} active: {}", id, active);
    }

    @Override
    @Transactional
    public SubscriberResponseDto updateSitePreferences(Long id, Set<SiteType> siteTypes) {
        Subscriber subscriber = subscriberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found with ID: " + id));

        Set<SiteType> newPreferences = (siteTypes != null && !siteTypes.isEmpty())
                ? siteTypes
                : new HashSet<>(Arrays.asList(SiteType.values()));

        subscriber.setSubscribedSites(newPreferences);
        Subscriber updated = subscriberRepository.save(subscriber);
        log.info("Updated site preferences for subscriber ID: {} -> {}", id, newPreferences);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public boolean unsubscribeByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String cleaned = email.trim().toLowerCase();
        var opt = subscriberRepository.findByEmail(cleaned);
        if (opt.isPresent()) {
            Subscriber sub = opt.get();
            sub.setActive(false);
            subscriberRepository.save(sub);
            log.info("Abonelik başarıyla iptal edildi: {}", cleaned);
            return true;
        }
        log.warn("Abonelik iptali için e-posta veritabanında bulunamadı: {}", cleaned);
        return false;
    }

    @Override
    @Transactional
    public int importSubscribersFromExcel(org.springframework.web.multipart.MultipartFile file) {
        return importSubscribersFromExcel(file, null);
    }

    @Override
    @Transactional
    public int importSubscribersFromExcel(org.springframework.web.multipart.MultipartFile file, Set<SiteType> targetSites) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Yüklenen dosya boş olamaz.");
        }

        Set<SiteType> sitesToAssign = (targetSites != null && !targetSites.isEmpty())
                ? targetSites
                : new HashSet<>(Arrays.asList(SiteType.values()));

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        List<Subscriber> importedList = new java.util.ArrayList<>();

        try {
            if (originalFilename.endsWith(".csv") || originalFilename.endsWith(".txt")) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) continue;
                        String[] parts = line.split("[,;\t]");
                        if (parts.length > 0) {
                            String email = parts[0].trim();
                            String fullName = parts.length > 1 ? parts[1].trim() : "";
                            processAndAddSubscriber(email, fullName, sitesToAssign, importedList);
                        }
                    }
                }
            } else {
                try (org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream())) {
                    org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
                    for (org.apache.poi.ss.usermodel.Row row : sheet) {
                        if (row == null) continue;
                        org.apache.poi.ss.usermodel.Cell cell0 = row.getCell(0);
                        org.apache.poi.ss.usermodel.Cell cell1 = row.getCell(1);

                        String val0 = cell0 != null ? cell0.toString().trim() : "";
                        String val1 = cell1 != null ? cell1.toString().trim() : "";

                        if (val0.toLowerCase().contains("email") || val0.toLowerCase().contains("e-posta") || val0.toLowerCase().contains("posta")) {
                            continue;
                        }

                        String email = val0;
                        String fullName = val1;
                        if (!email.contains("@") && val1.contains("@")) {
                            email = val1;
                            fullName = val0;
                        }

                        processAndAddSubscriber(email, fullName, sitesToAssign, importedList);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Excel/CSV içe aktarma hatası: {}", e.getMessage(), e);
            throw new RuntimeException("Excel dosyası ayrıştırılırken hata oluştu: " + e.getMessage(), e);
        }

        if (!importedList.isEmpty()) {
            subscriberRepository.saveAll(importedList);
            log.info("Toplu yükleme ile {} yeni/güncel abone kaydedildi. Atanan siteler: {}", importedList.size(), sitesToAssign);
        }

        return importedList.size();
    }

    private void processAndAddSubscriber(String email, String fullName, Set<SiteType> sitesToAssign, List<Subscriber> importedList) {
        if (email == null || !email.contains("@") || email.length() < 5) {
            return;
        }
        String cleanEmail = email.trim().toLowerCase();

        var existingOpt = subscriberRepository.findByEmail(cleanEmail);
        if (existingOpt.isPresent()) {
            Subscriber existing = existingOpt.get();
            existing.setSubscribedSites(new HashSet<>(sitesToAssign));
            if (!existing.isActive()) {
                existing.setActive(true);
            }
            importedList.add(existing);
        } else {
            Subscriber newSub = Subscriber.builder()
                    .email(cleanEmail)
                    .fullName(fullName.isBlank() ? cleanEmail.split("@")[0] : fullName)
                    .subscribedSites(new HashSet<>(sitesToAssign))
                    .active(true)
                    .build();
            importedList.add(newSub);
        }
    }

    private SubscriberResponseDto mapToDto(Subscriber entity) {
        return SubscriberResponseDto.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .fullName(entity.getFullName())
                .active(entity.isActive())
                .subscribedSites(entity.getSubscribedSites())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
