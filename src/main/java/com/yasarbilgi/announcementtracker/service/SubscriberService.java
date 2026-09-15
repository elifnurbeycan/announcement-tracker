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

    List<SubscriberResponseDto> getAllSubscribers();

    void deleteSubscriber(Long id);

    void toggleSubscriberStatus(Long id, boolean active);

    SubscriberResponseDto updateSitePreferences(Long id, Set<SiteType> siteTypes);

    boolean unsubscribeByEmail(String email);

    int importSubscribersFromExcel(MultipartFile file);

    int importSubscribersFromExcel(MultipartFile file, Set<SiteType> targetSites);
}
