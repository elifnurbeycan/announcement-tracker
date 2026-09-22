package com.yasarbilgi.announcementtracker.service;

import com.yasarbilgi.announcementtracker.dto.request.SubscriberRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.SubscriberResponseDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/**
 * E-posta abonesi ekleme, silme, abonelik iptali ve Excel ile toplu yükleme işlemlerini yöneten servis arayüzü.
 */
public interface SubscriberService {

    SubscriberResponseDto addSubscriber(SubscriberRequestDto dto);

    SubscriberResponseDto updateSubscriber(Long id, SubscriberRequestDto dto);

    List<SubscriberResponseDto> getAllSubscribers();

    void deleteSubscriber(Long id);

    void deleteSubscribersBatch(List<Long> ids);

    void toggleSubscriberStatus(Long id, boolean active);

    void sendPasswordSetupEmail(Long id);

    SubscriberResponseDto updateSitePreferences(Long id, Set<SiteType> siteTypes);

    boolean unsubscribeByEmail(String email);

    String generateUnsubscribeToken(String email);

    boolean unsubscribeByToken(String token);

    int importSubscribersFromExcel(MultipartFile file);

    int importSubscribersFromExcel(MultipartFile file, Set<SiteType> targetSites);

    com.yasarbilgi.announcementtracker.dto.response.ExcelImportResultDto importSubscribersFromExcelDetailed(MultipartFile file, Set<SiteType> targetSites);

    com.yasarbilgi.announcementtracker.dto.response.UserLoginResponseDto createOidcSession(String email);

    void logoutUser(String token);

    SubscriberResponseDto validateUserToken(String userToken);

    SubscriberResponseDto updateSubscriberDepartments(Long id, Set<Long> departmentIds);
}
