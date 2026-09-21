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
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import com.yasarbilgi.announcementtracker.dto.request.SetPasswordRequestDto;
import com.yasarbilgi.announcementtracker.dto.request.UserLoginRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.UserLoginResponseDto;
import com.yasarbilgi.announcementtracker.exception.ScrapingException;
import com.yasarbilgi.announcementtracker.util.PasswordEncoderHelper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.yasarbilgi.announcementtracker.dto.response.DepartmentSummaryDto;
import com.yasarbilgi.announcementtracker.entity.Department;
import com.yasarbilgi.announcementtracker.repository.DepartmentRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriberServiceImpl implements SubscriberService {

    private final SubscriberRepository subscriberRepository;
    private final DepartmentRepository departmentRepository;
    private final AnnouncementRepository announcementRepository;
    private final EmailService emailService;
    private final PasswordEncoderHelper passwordEncoderHelper;
    private final com.yasarbilgi.announcementtracker.service.KeycloakAdminService keycloakAdminService;
    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    @org.springframework.beans.factory.annotation.Value("${keycloak.auth-server-url:http://localhost:8180}")
    private String keycloakServerUrl;

    @org.springframework.beans.factory.annotation.Value("${keycloak.realm:announcement-tracker-realm}")
    private String keycloakRealm;

    @org.springframework.beans.factory.annotation.Value("${keycloak.client-id:announcement-tracker-app}")
    private String keycloakClientId;

    private final Map<String, UserSessionInfo> activeUserSessions = new ConcurrentHashMap<>();

    private record UserSessionInfo(Long subscriberId, LocalDateTime expiresAt) {}

    @Override
    @Transactional
    public SubscriberResponseDto addSubscriber(SubscriberRequestDto dto) {
        Subscriber saved;
        Set<SiteType> preferredSites = (dto.getSubscribedSites() != null && !dto.getSubscribedSites().isEmpty())
                ? dto.getSubscribedSites()
                : new HashSet<>(Arrays.asList(SiteType.values()));

        Set<Department> assignedDepts = new HashSet<>();
        if (dto.getDepartmentIds() != null && !dto.getDepartmentIds().isEmpty()) {
            assignedDepts = new HashSet<>(departmentRepository.findAllById(dto.getDepartmentIds()));
        }

        if (subscriberRepository.existsByEmail(dto.getEmail())) {
            log.info("Subscriber email already exists: {}", dto.getEmail());
            saved = subscriberRepository.findByEmail(dto.getEmail()).orElseThrow();
            saved.setSubscribedSites(preferredSites);
            saved.setDepartments(assignedDepts);
            if (!saved.isActive()) {
                saved.setActive(true);
            }
            if (saved.getActivationToken() == null) {
                saved.setActivationToken(UUID.randomUUID().toString());
                saved.setTokenExpiry(LocalDateTime.now().plusDays(7));
            }
            subscriberRepository.save(saved);
        } else {
            String token = UUID.randomUUID().toString();
            Subscriber subscriber = Subscriber.builder()
                    .email(dto.getEmail())
                    .fullName(dto.getFullName())
                    .subscribedSites(preferredSites)
                    .departments(assignedDepts)
                    .activationToken(token)
                    .tokenExpiry(LocalDateTime.now().plusDays(7))
                    .active(true)
                    .build();
            saved = subscriberRepository.save(subscriber);
            log.info("New subscriber registered: {} with preferences: {}", saved.getEmail(), preferredSites);
        }

        // Send welcome email with activation token for setting user portal password
        try {
            if (saved.getActivationToken() != null) {
                emailService.sendWelcomeAndActivationEmail(saved.getEmail(), saved.getFullName(), saved.getActivationToken());
            }
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
    @Transactional
    public SubscriberResponseDto updateSubscriber(Long id, SubscriberRequestDto dto) {
        Subscriber subscriber = subscriberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found with ID: " + id));

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            String newEmail = dto.getEmail().trim().toLowerCase();
            if (!newEmail.equalsIgnoreCase(subscriber.getEmail()) && subscriberRepository.existsByEmail(newEmail)) {
                throw new ScrapingException("'" + newEmail + "' adında bir e-posta adresi zaten kullanımda.");
            }
            subscriber.setEmail(newEmail);
        }

        if (dto.getFullName() != null) {
            subscriber.setFullName(dto.getFullName().trim());
        }

        if (dto.getSubscribedSites() != null) {
            subscriber.setSubscribedSites(new HashSet<>(dto.getSubscribedSites()));
        }

        if (dto.getDepartmentIds() != null) {
            Set<Department> newDepts = new HashSet<>();
            if (!dto.getDepartmentIds().isEmpty()) {
                newDepts = new HashSet<>(departmentRepository.findAllById(dto.getDepartmentIds()));
            }
            subscriber.setDepartments(newDepts);
        }

        Subscriber updated = subscriberRepository.save(subscriber);
        log.info("Subscriber ID: {} updated successfully.", id);
        return mapToDto(updated);
    }

    @Override
    public List<SubscriberResponseDto> getAllSubscribers() {
        return subscriberRepository.findAll().stream().map(this::mapToDto).toList();
    }

    @Override
    @Transactional
    public void deleteSubscriber(Long id) {
        Subscriber subscriber = subscriberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found with ID: " + id));

        try {
            if (keycloakAdminService != null && subscriber.getEmail() != null) {
                keycloakAdminService.deleteUserInKeycloak(subscriber.getEmail());
            }
        } catch (Exception e) {
            log.warn("Could not delete user '{}' from Keycloak: {}", subscriber.getEmail(), e.getMessage());
        }

        subscriberRepository.delete(subscriber);
        log.info("Deleted subscriber with ID: {} and email: {}", id, subscriber.getEmail());
    }

    @Override
    @Transactional
    public void deleteSubscribersBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            try {
                deleteSubscriber(id);
            } catch (Exception e) {
                log.warn("Toplu silme sırasında abone ID: {} silinemedi: {}", id, e.getMessage());
            }
        }
        log.info("Toplu abone silme tamamlandı. Toplam talep edilen: {}", ids.size());
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

        // Kişisel tercihler boş olabilir. Departman siteleri getEffectiveSites içinde
        // her zaman birleştirildiği için kullanıcı zorunlu departman kapsamını kaldıramaz.
        Set<SiteType> newPreferences = siteTypes != null
                ? new HashSet<>(siteTypes)
                : new HashSet<>();

        subscriber.setSubscribedSites(newPreferences);
        Subscriber updated = subscriberRepository.save(subscriber);
        log.info("Updated site preferences for subscriber ID: {} -> {}", id, newPreferences);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public SubscriberResponseDto updateSubscriberDepartments(Long id, Set<Long> departmentIds) {
        Subscriber subscriber = subscriberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found with ID: " + id));

        Set<Department> newDepts = new HashSet<>();
        if (departmentIds != null && !departmentIds.isEmpty()) {
            newDepts = new HashSet<>(departmentRepository.findAllById(departmentIds));
        }

        subscriber.setDepartments(newDepts);
        Subscriber updated = subscriberRepository.save(subscriber);
        log.info("Updated departments for subscriber ID: {} -> {}", id, newDepts.stream().map(Department::getName).toList());
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
    public int importSubscribersFromExcel(MultipartFile file) {
        return importSubscribersFromExcelDetailed(file, null).getSuccessCount();
    }

    @Override
    @Transactional
    public int importSubscribersFromExcel(MultipartFile file, Set<SiteType> targetSites) {
        return importSubscribersFromExcelDetailed(file, targetSites).getSuccessCount();
    }

    @Override
    @Transactional
    public com.yasarbilgi.announcementtracker.dto.response.ExcelImportResultDto importSubscribersFromExcelDetailed(MultipartFile file, Set<SiteType> targetSites) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Yüklenen dosya boş olamaz.");
        }

        Set<SiteType> sitesToAssign = (targetSites != null && !targetSites.isEmpty())
                ? targetSites
                : new HashSet<>(Arrays.asList(SiteType.values()));

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        List<Subscriber> importedList = new ArrayList<>();
        List<String> errorsList = new ArrayList<>();
        int totalRows = 0;

        try {
            if (originalFilename.endsWith(".csv") || originalFilename.endsWith(".txt")) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    int rowNum = 0;
                    while ((line = reader.readLine()) != null) {
                        rowNum++;
                        if (line.isBlank()) continue;
                        if (line.toLowerCase().contains("email") || line.toLowerCase().contains("e-posta") || line.toLowerCase().contains("posta")) {
                            continue;
                        }
                        totalRows++;
                        String[] parts = line.split("[,;\t]");
                        if (parts.length > 0) {
                            String email = parts[0].trim();
                            String fullName = parts.length > 1 ? parts[1].trim() : "";
                            String rawDepts = parts.length > 2 ? String.join(",", Arrays.copyOfRange(parts, 2, parts.length)).trim() : "";
                            processExcelRow(rowNum, email, fullName, rawDepts, sitesToAssign, importedList, errorsList);
                        }
                    }
                }
            } else {
                try (org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream())) {
                    org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
                    int rowNum = 0;
                    for (org.apache.poi.ss.usermodel.Row row : sheet) {
                        rowNum++;
                        if (row == null) continue;
                        org.apache.poi.ss.usermodel.Cell cell0 = row.getCell(0);
                        org.apache.poi.ss.usermodel.Cell cell1 = row.getCell(1);
                        org.apache.poi.ss.usermodel.Cell cell2 = row.getCell(2);

                        String val0 = cell0 != null ? cell0.toString().trim() : "";
                        String val1 = cell1 != null ? cell1.toString().trim() : "";
                        String val2 = cell2 != null ? cell2.toString().trim() : "";

                        if (val0.toLowerCase().contains("email") || val0.toLowerCase().contains("e-posta") || val0.toLowerCase().contains("posta")) {
                            continue;
                        }

                        if (val0.isBlank() && val1.isBlank() && val2.isBlank()) {
                            continue;
                        }

                        totalRows++;

                        String email = val0;
                        String fullName = val1;
                        String rawDepts = val2;

                        if (!email.contains("@") && val1.contains("@")) {
                            email = val1;
                            fullName = val0;
                        }

                        processExcelRow(rowNum, email, fullName, rawDepts, sitesToAssign, importedList, errorsList);
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

        return com.yasarbilgi.announcementtracker.dto.response.ExcelImportResultDto.builder()
                .totalRows(totalRows)
                .successCount(importedList.size())
                .errorCount(errorsList.size())
                .errors(errorsList)
                .build();
    }

    private void processExcelRow(int rowNum, String email, String fullName, String rawDepts, Set<SiteType> sitesToAssign, List<Subscriber> importedList, List<String> errorsList) {
        if (email == null || email.isBlank() || !email.contains("@") || email.length() < 5) {
            errorsList.add("Satır " + rowNum + ": Geçersiz veya eksik e-posta adresi ('" + email + "').");
            return;
        }
        String cleanEmail = email.trim().toLowerCase();

        Set<Department> departments = new HashSet<>();
        if (rawDepts != null && !rawDepts.isBlank()) {
            String[] deptTokens = rawDepts.split("[,;]");
            Set<String> cleanNames = new HashSet<>();
            for (String t : deptTokens) {
                String name = t.trim();
                if (!name.isEmpty()) {
                    cleanNames.add(name);
                }
            }

            for (String deptName : cleanNames) {
                Optional<Department> deptOpt = departmentRepository.findByNameIgnoreCase(deptName);
                if (deptOpt.isEmpty()) {
                    errorsList.add("Satır " + rowNum + ": '" + deptName + "' isimli departman sistemde bulunamadı.");
                    return;
                } else {
                    departments.add(deptOpt.get());
                }
            }
        }

        var existingOpt = subscriberRepository.findByEmail(cleanEmail);
        if (existingOpt.isPresent()) {
            Subscriber existing = existingOpt.get();
            existing.setSubscribedSites(new HashSet<>(sitesToAssign));
            existing.setDepartments(departments);
            if (!existing.isActive()) {
                existing.setActive(true);
            }
            if (existing.getActivationToken() == null && existing.getPasswordHash() == null) {
                existing.setActivationToken(UUID.randomUUID().toString());
                existing.setTokenExpiry(LocalDateTime.now().plusDays(7));
            }
            importedList.add(existing);
        } else {
            Subscriber newSub = Subscriber.builder()
                    .email(cleanEmail)
                    .fullName(fullName.isBlank() ? cleanEmail.split("@")[0] : fullName)
                    .subscribedSites(new HashSet<>(sitesToAssign))
                    .departments(departments)
                    .activationToken(UUID.randomUUID().toString())
                    .tokenExpiry(LocalDateTime.now().plusDays(7))
                    .active(true)
                    .build();
            importedList.add(newSub);
        }
    }

    @Override
    @Transactional
    public SubscriberResponseDto setPasswordWithToken(SetPasswordRequestDto dto) {
        if (dto.getToken() == null || dto.getToken().isBlank()) {
            throw new ScrapingException("Aktivasyon jetonu gereklidir.");
        }
        Subscriber subscriber = subscriberRepository.findByActivationToken(dto.getToken().trim())
                .orElseThrow(() -> new ScrapingException("Geçersiz veya süresi dolmuş aktivasyon jetonu."));

        if (subscriber.getTokenExpiry() != null && subscriber.getTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ScrapingException("Aktivasyon jetonunun süresi dolmuş. Lütfen yeni şifre sıfırlama talep edin.");
        }

        subscriber.setPasswordHash(passwordEncoderHelper.encode(dto.getPassword()));
        subscriber.setActivationToken(null);
        subscriber.setTokenExpiry(null);
        Subscriber updated = subscriberRepository.save(subscriber);

        // Auto-sync user identity and credentials to Keycloak
        try {
            if (keycloakAdminService != null) {
                keycloakAdminService.createOrUpdateUserInKeycloak(updated.getEmail(), dto.getPassword(), updated.getFullName());
            }
        } catch (Exception e) {
            log.warn("Could not auto-sync user '{}' to Keycloak during password set: {}", updated.getEmail(), e.getMessage());
        }

        log.info("User portal password successfully set and synced to Keycloak for subscriber: {}", updated.getEmail());
        return mapToDto(updated);
    }

    @Override
    public UserLoginResponseDto loginUser(UserLoginRequestDto dto) {
        String cleanEmail = dto.getEmail().trim().toLowerCase();

        // 1. Try Keycloak Direct Access Grant Token endpoint if Keycloak is reachable
        try {
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakServerUrl, keycloakRealm);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

            org.springframework.util.MultiValueMap<String, String> params = new org.springframework.util.LinkedMultiValueMap<>();
            params.add("grant_type", "password");
            params.add("client_id", keycloakClientId);
            params.add("username", cleanEmail);
            params.add("password", dto.getPassword());

            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> entity = new org.springframework.http.HttpEntity<>(params, headers);
            org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().containsKey("access_token")) {
                String token = (String) response.getBody().get("access_token");
                log.info("Successfully authenticated Subscriber '{}' via Keycloak OAuth2.", cleanEmail);

                Subscriber subscriber = subscriberRepository.findByEmail(cleanEmail).orElse(null);
                Long subId = subscriber != null ? subscriber.getId() : 1L;
                String fullName = subscriber != null ? subscriber.getFullName() : cleanEmail;
                Set<SiteType> sites = subscriber != null ? subscriber.getSubscribedSites() : new HashSet<>(Arrays.asList(SiteType.values()));

                activeUserSessions.put(token, new UserSessionInfo(subId, LocalDateTime.now().plusDays(7)));

                return UserLoginResponseDto.builder()
                        .token(token)
                        .id(subId)
                        .email(cleanEmail)
                        .fullName(fullName)
                        .subscribedSites(sites)
                        .build();
            }
        } catch (Exception e) {
            log.debug("Keycloak subscriber authentication server unavailable or failed, falling back to local database auth: {}", e.getMessage());
        }

        Subscriber subscriber = subscriberRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new ScrapingException("Geçersiz e-posta veya şifre."));

        if (subscriber.getPasswordHash() == null || !passwordEncoderHelper.matches(dto.getPassword(), subscriber.getPasswordHash())) {
            throw new ScrapingException("Geçersiz e-posta veya şifre.");
        }

        if (!subscriber.isActive()) {
            throw new ScrapingException("Aboneliğiniz pasif durumdadır. Lütfen müşteri hizmetleri ile iletişime geçin.");
        }

        // Auto-migrate existing subscriber to Keycloak upon successful local login
        try {
            if (keycloakAdminService != null) {
                keycloakAdminService.createOrUpdateUserInKeycloak(subscriber.getEmail(), dto.getPassword(), subscriber.getFullName());
                log.info("Auto-migrated existing subscriber '{}' to Keycloak upon login.", subscriber.getEmail());
            }
        } catch (Exception e) {
            log.warn("Could not auto-migrate user '{}' to Keycloak during login: {}", subscriber.getEmail(), e.getMessage());
        }

        String userToken = "USER-TOKEN-" + UUID.randomUUID();
        activeUserSessions.put(userToken, new UserSessionInfo(subscriber.getId(), LocalDateTime.now().plusDays(7)));

        log.info("User portal login successful for: {}", cleanEmail);

        return UserLoginResponseDto.builder()
                .token(userToken)
                .id(subscriber.getId())
                .email(subscriber.getEmail())
                .fullName(subscriber.getFullName())
                .subscribedSites(subscriber.getSubscribedSites())
                .build();
    }

    @Override
    public SubscriberResponseDto validateUserToken(String userToken) {
        if (userToken == null || userToken.isBlank()) {
            throw new ScrapingException("Oturum jetonu bulunamadı.");
        }
        if (userToken.startsWith("Bearer ")) {
            userToken = userToken.substring(7);
        }

        UserSessionInfo session = activeUserSessions.get(userToken);
        if (session != null && session.expiresAt().isAfter(LocalDateTime.now())) {
            Subscriber subscriber = subscriberRepository.findById(session.subscriberId()).orElse(null);
            if (subscriber != null) {
                return mapToDto(subscriber);
            }
        }

        if (session != null) {
            activeUserSessions.remove(userToken);
        }
        throw new ScrapingException("Oturum süreniz doldu. Lütfen tekrar giriş yapın.");
    }

    private SubscriberResponseDto mapToDto(Subscriber entity) {
        Set<SiteType> allSites = new HashSet<>(Arrays.asList(SiteType.values()));
        List<DepartmentSummaryDto> deptSummaries = entity.getDepartments() != null
                ? entity.getDepartments().stream()
                .map(d -> DepartmentSummaryDto.builder()
                        .id(d.getId())
                        .name(d.getName())
                        .sites(d.getSites() != null ? new HashSet<>(d.getSites()) : new HashSet<>())
                        .build())
                .toList()
                : List.of();

        Set<SiteType> deptSites = entity.getDepartments() != null
                ? entity.getDepartments().stream()
                .filter(d -> d.getSites() != null)
                .flatMap(d -> d.getSites().stream())
                .collect(java.util.stream.Collectors.toSet())
                : Set.of();

        return SubscriberResponseDto.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .fullName(entity.getFullName())
                .active(entity.isActive())
                .hasPasswordSet(entity.getPasswordHash() != null)
                .subscribedSites(entity.getSubscribedSites())
                .departments(deptSummaries)
                .isGeneralEmployee(entity.isGeneralEmployee())
                .departmentSites(deptSites)
                .effectiveSites(entity.getEffectiveSites(allSites))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
