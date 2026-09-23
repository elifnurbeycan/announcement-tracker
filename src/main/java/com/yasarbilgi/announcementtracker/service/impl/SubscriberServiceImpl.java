package com.yasarbilgi.announcementtracker.service.impl;

import com.yasarbilgi.announcementtracker.dto.request.SubscriberRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.DepartmentSummaryDto;
import com.yasarbilgi.announcementtracker.dto.response.ExcelImportResultDto;
import com.yasarbilgi.announcementtracker.dto.response.SubscriberResponseDto;
import com.yasarbilgi.announcementtracker.dto.session.SessionToken;
import com.yasarbilgi.announcementtracker.entity.Department;
import com.yasarbilgi.announcementtracker.entity.Subscriber;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.exception.AlreadyExistsException;
import com.yasarbilgi.announcementtracker.exception.InvalidTokenException;
import com.yasarbilgi.announcementtracker.exception.ResourceNotFoundException;
import com.yasarbilgi.announcementtracker.exception.UnauthorizedException;
import com.yasarbilgi.announcementtracker.repository.DepartmentRepository;
import com.yasarbilgi.announcementtracker.repository.SubscriberRepository;
import com.yasarbilgi.announcementtracker.service.EmailService;
import com.yasarbilgi.announcementtracker.service.KeycloakAdminService;
import com.yasarbilgi.announcementtracker.service.SubscriberService;
import com.yasarbilgi.announcementtracker.service.UnsubscribeTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriberServiceImpl implements SubscriberService {

    private final SubscriberRepository subscriberRepository;
    private final DepartmentRepository departmentRepository;
    private final EmailService emailService;
    private final KeycloakAdminService keycloakAdminService;
    private final UnsubscribeTokenService unsubscribeTokenService;

    @org.springframework.beans.factory.annotation.Value("${announcement.tracker.app-base-url:http://localhost:8080}")
    private String appBaseUrl = "http://localhost:8080";

    private final Map<String, UserSessionInfo> activeUserSessions = new ConcurrentHashMap<>();

    private record UserSessionInfo(Long subscriberId, LocalDateTime expiresAt) {}

    @Override
    @Transactional
    public SubscriberResponseDto addSubscriber(SubscriberRequestDto dto) {
        String normalizedEmail = dto.getEmail().trim().toLowerCase(Locale.ROOT);
        Subscriber saved;
        Set<Department> assignedDepts = resolveDepartments(dto.getDepartmentIds());

        Set<SiteType> preferredSites;
        if (dto.getSubscribedSites() != null && !dto.getSubscribedSites().isEmpty()) {
            preferredSites = dto.getSubscribedSites();
        } else if (!assignedDepts.isEmpty()) {
            preferredSites = new HashSet<>();
        } else {
            preferredSites = new HashSet<>(Arrays.asList(SiteType.values()));
        }

        if (subscriberRepository.existsByEmail(normalizedEmail)) {
            log.info("Subscriber email already exists: {}", normalizedEmail);
            saved = subscriberRepository.findByEmail(normalizedEmail).orElseThrow();
            saved.setFullName(dto.getFullName() != null ? dto.getFullName().trim() : saved.getFullName());
            saved.setSubscribedSites(preferredSites);
            saved.setDepartments(assignedDepts);
            if (!saved.isActive()) {
                saved.setActive(true);
            }
            saved = subscriberRepository.saveAndFlush(saved);
        } else {
            Subscriber subscriber = Subscriber.builder()
                    .email(normalizedEmail)
                    .fullName(dto.getFullName().trim())
                    .subscribedSites(preferredSites)
                    .departments(assignedDepts)
                    .active(true)
                    .build();
            saved = subscriberRepository.saveAndFlush(subscriber);
            log.info("New subscriber registered: {} with preferences: {}", saved.getEmail(), preferredSites);
        }

        String keycloakSubject = keycloakAdminService.provisionSubscriber(
                saved.getEmail(), saved.getFullName(), saved.isActive());
        if (keycloakSubject != null && !keycloakSubject.isBlank()) {
            saved.setKeycloakSubject(keycloakSubject);
            saved = subscriberRepository.save(saved);
        }
        if (keycloakAdminService.isEnabled()) {
            try {
                keycloakAdminService.triggerKeycloakResetPasswordEmail(saved.getEmail());
            } catch (Exception e) {
                log.warn("Keycloak reset password email trigger failed for {}: {}", saved.getEmail(), e.getMessage());
            }
        }

        try {
            emailService.sendWelcomeAndActivationEmail(
                    saved.getEmail(), saved.getFullName(), appBaseUrl + "/login");
        } catch (Exception e) {
            log.error("Failed to send welcome email notification to {}: {}", saved.getEmail(), e.getMessage());
        }

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public SubscriberResponseDto updateSubscriber(Long id, SubscriberRequestDto dto) {
        Subscriber subscriber = subscriberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Abone bulunamadı, ID: " + id));
        String previousEmail = subscriber.getEmail();

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            String newEmail = dto.getEmail().trim().toLowerCase();
            if (!newEmail.equalsIgnoreCase(subscriber.getEmail()) && subscriberRepository.existsByEmail(newEmail)) {
                throw new AlreadyExistsException("'" + newEmail + "' e-posta adresi zaten kullanımda.");
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
            subscriber.setDepartments(resolveDepartments(dto.getDepartmentIds()));
        }

        String keycloakSubject = keycloakAdminService.updateSubscriber(
                previousEmail, subscriber.getEmail(), subscriber.getFullName(), subscriber.isActive());
        if (keycloakSubject != null && !keycloakSubject.isBlank()) {
            subscriber.setKeycloakSubject(keycloakSubject);
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
                .orElseThrow(() -> new ResourceNotFoundException("Abone bulunamadı, ID: " + id));

        subscriberRepository.delete(subscriber);
        subscriberRepository.flush();
        keycloakAdminService.deleteUserInKeycloak(subscriber.getEmail());
        log.info("Deleted subscriber with ID: {} and email: {}", id, subscriber.getEmail());
    }

    @Override
    @Transactional
    public void deleteSubscribersBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            deleteSubscriber(id);
        }
        log.info("Toplu abone silme tamamlandı. Toplam talep edilen: {}", ids.size());
    }

    @Override
    @Transactional
    public void toggleSubscriberStatus(Long id, boolean active) {
        Subscriber subscriber = subscriberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Abone bulunamadı, ID: " + id));
        String keycloakSubject = keycloakAdminService.provisionSubscriber(
                subscriber.getEmail(), subscriber.getFullName(), active);
        if (keycloakSubject != null && !keycloakSubject.isBlank()) {
            subscriber.setKeycloakSubject(keycloakSubject);
        }
        subscriber.setActive(active);
        subscriberRepository.save(subscriber);
        log.info("Updated subscriber status ID: {} active: {}", id, active);
    }

    @Override
    public void sendPasswordSetupEmail(Long id) {
        Subscriber subscriber = subscriberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Abone bulunamadı, ID: " + id));
        if (!subscriber.isActive()) {
            throw new IllegalArgumentException("Pasif abonelere şifre belirleme bağlantısı gönderilemez.");
        }
        String keycloakSubject = keycloakAdminService.provisionSubscriber(
                subscriber.getEmail(), subscriber.getFullName(), true);
        if (keycloakSubject != null && !keycloakSubject.isBlank()) {
            subscriber.setKeycloakSubject(keycloakSubject);
            subscriberRepository.save(subscriber);
        }
        keycloakAdminService.triggerKeycloakResetPasswordEmail(subscriber.getEmail());
    }

    @Override
    @Transactional
    public SubscriberResponseDto updateSitePreferences(Long id, Set<SiteType> siteTypes) {
        Subscriber subscriber = subscriberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Abone bulunamadı, ID: " + id));

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
                .orElseThrow(() -> new ResourceNotFoundException("Abone bulunamadı, ID: " + id));

        Set<Department> newDepts = resolveDepartments(departmentIds);
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
            String keycloakSubject = keycloakAdminService.provisionSubscriber(
                    sub.getEmail(), sub.getFullName(), false);
            if (keycloakSubject != null && !keycloakSubject.isBlank()) {
                sub.setKeycloakSubject(keycloakSubject);
            }
            sub.setActive(false);
            subscriberRepository.save(sub);
            log.info("Abonelik başarıyla iptal edildi: {}", cleaned);
            return true;
        }
        log.warn("Abonelik iptali için e-posta veritabanında bulunamadı: {}", cleaned);
        return false;
    }

    @Override
    public String generateUnsubscribeToken(String email) {
        return unsubscribeTokenService.generate(email);
    }

    @Override
    @Transactional
    public boolean unsubscribeByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Abonelikten çıkma bağlantısı geçersiz veya süresi dolmuş.");
        }
        String email = unsubscribeTokenService.verifyAndExtractEmail(token)
                .orElseThrow(() -> new InvalidTokenException(
                        "Abonelikten çıkma bağlantısı geçersiz veya süresi dolmuş."));
        if (!unsubscribeByEmail(email)) {
            throw new InvalidTokenException("Bu bağlantıyla eşleşen aktif bir abonelik bulunamadı.");
        }
        return true;
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
    public ExcelImportResultDto importSubscribersFromExcelDetailed(MultipartFile file, Set<SiteType> targetSites) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Yüklenen dosya boş olamaz.");
        }

        Set<SiteType> sitesToAssign = targetSites;

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
                    for (org.apache.poi.ss.usermodel.Row row : sheet) {
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

                        processExcelRow(totalRows, email, fullName, rawDepts, sitesToAssign, importedList, errorsList);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Excel/CSV içe aktarma hatası: {}", e.getMessage(), e);
            throw new RuntimeException("Excel dosyası ayrıştırılırken hata oluştu: " + e.getMessage(), e);
        }

        if (!importedList.isEmpty()) {
            importedList.forEach(subscriber -> {
                String keycloakSubject = keycloakAdminService.provisionSubscriber(
                        subscriber.getEmail(), subscriber.getFullName(), subscriber.isActive());
                if (keycloakSubject != null && !keycloakSubject.isBlank()) {
                    subscriber.setKeycloakSubject(keycloakSubject);
                }
            });
            subscriberRepository.saveAll(importedList);
            log.info("Toplu yükleme ile {} yeni/güncel abone kaydedildi. Atanan siteler: {}", importedList.size(), sitesToAssign);
        }

        return ExcelImportResultDto.builder()
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
        String cleanFullName = fullName == null ? "" : fullName.trim();
        if (cleanFullName.isBlank()) {
            cleanFullName = cleanEmail.substring(0, cleanEmail.indexOf('@'));
        }
        if (cleanFullName.length() > 100) {
            errorsList.add("Satır " + rowNum + ": Ad Soyad en fazla 100 karakter olabilir.");
            return;
        }

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

        Set<SiteType> effectiveSubscribedSites;
        if (sitesToAssign != null && !sitesToAssign.isEmpty()) {
            effectiveSubscribedSites = new HashSet<>(sitesToAssign);
        } else if (!departments.isEmpty()) {
            effectiveSubscribedSites = new HashSet<>();
        } else {
            effectiveSubscribedSites = new HashSet<>(Arrays.asList(SiteType.values()));
        }

        var existingOpt = subscriberRepository.findByEmail(cleanEmail);
        if (existingOpt.isPresent()) {
            Subscriber existing = existingOpt.get();
            existing.setFullName(cleanFullName);
            existing.setSubscribedSites(effectiveSubscribedSites);
            existing.setDepartments(departments);
            if (!existing.isActive()) {
                existing.setActive(true);
            }
            importedList.add(existing);
        } else {
            Subscriber newSub = Subscriber.builder()
                    .email(cleanEmail)
                    .fullName(cleanFullName)
                    .subscribedSites(effectiveSubscribedSites)
                    .departments(departments)
                    .active(true)
                    .build();
            importedList.add(newSub);
        }
    }

    private Set<Department> resolveDepartments(Set<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return new HashSet<>();
        }
        if (departmentIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Departman ID değeri boş olamaz.");
        }

        List<Department> foundDepartments = departmentRepository.findAllById(departmentIds);
        Set<Long> foundIds = foundDepartments.stream()
                .map(Department::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> missingIds = new TreeSet<>(departmentIds);
        missingIds.removeAll(foundIds);
        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException("Departman bulunamadı, ID: " + missingIds);
        }
        return new HashSet<>(foundDepartments);
    }


    @Override
    public SubscriberResponseDto validateUserToken(String userToken) {
        if (userToken == null || userToken.isBlank()) {
            throw new UnauthorizedException("Oturum jetonu bulunamadı. Lütfen giriş yapın.");
        }
        if (userToken.startsWith("Bearer ")) {
            userToken = userToken.substring(7);
        }

        UserSessionInfo session = activeUserSessions.get(userToken);
        if (session != null && session.expiresAt().isAfter(LocalDateTime.now())) {
            Subscriber subscriber = subscriberRepository.findById(session.subscriberId()).orElse(null);
            if (subscriber != null) {
                if (!subscriber.isActive()) {
                    activeUserSessions.remove(userToken);
                    throw new UnauthorizedException("Aboneliğiniz pasif durumdadır. Lütfen yönetici ile iletişime geçin.");
                }
                return mapToDto(subscriber);
            }
        }

        if (session != null) {
            activeUserSessions.remove(userToken);
        }
        throw new UnauthorizedException("Oturum süreniz doldu. Lütfen tekrar giriş yapın.");
    }

    @Override
    @Transactional
    public SessionToken createOidcSession(String keycloakSubject, String email) {
        if (keycloakSubject == null || keycloakSubject.isBlank()) {
            throw new UnauthorizedException("Keycloak kullanıcı kimliği alınamadı.");
        }
        String cleanEmail = email == null ? "" : email.trim().toLowerCase();
        Subscriber subscriber = subscriberRepository.findByKeycloakSubject(keycloakSubject)
                .orElseGet(() -> subscriberRepository.findByEmail(cleanEmail)
                        .map(existing -> {
                            if (existing.getKeycloakSubject() != null
                                    && !existing.getKeycloakSubject().isBlank()
                                    && !existing.getKeycloakSubject().equals(keycloakSubject)) {
                                throw new UnauthorizedException("Bu çalışan hesabı farklı bir Keycloak kimliğiyle eşleştirilmiş.");
                            }
                            return existing;
                        })
                        .orElseThrow(() -> new UnauthorizedException(
                                "OIDC kullanıcısı yerel çalışan kaydıyla eşleşmiyor.")));
        if (!subscriber.isActive()) {
            throw new UnauthorizedException("Aboneliğiniz pasif durumdadır.");
        }
        subscriber.setKeycloakSubject(keycloakSubject);
        subscriberRepository.save(subscriber);

        String sessionToken = "USER-TOKEN-" + UUID.randomUUID();
        activeUserSessions.put(sessionToken, new UserSessionInfo(subscriber.getId(), LocalDateTime.now().plusDays(7)));

        return new SessionToken(sessionToken);
    }

    @Override
    public void logoutUser(String userToken) {
        if (userToken != null && userToken.startsWith("Bearer ")) {
            userToken = userToken.substring(7);
        }
        if (userToken != null) {
            activeUserSessions.remove(userToken);
            log.info("User session token invalidated.");
        }
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
                .collect(Collectors.toSet())
                : Set.of();

        return SubscriberResponseDto.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .fullName(entity.getFullName())
                .active(entity.isActive())
                .subscribedSites(entity.getSubscribedSites())
                .departments(deptSummaries)
                .generalEmployee(entity.isGeneralEmployee())
                .departmentSites(deptSites)
                .effectiveSites(entity.getEffectiveSites(allSites))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
