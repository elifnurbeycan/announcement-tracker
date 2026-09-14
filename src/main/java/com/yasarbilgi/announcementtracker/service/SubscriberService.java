package com.yasarbilgi.announcementtracker.service;

import com.yasarbilgi.announcementtracker.dto.request.SubscriberRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.SubscriberResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * E-posta abonesi ekleme, silme, abonelik iptali ve Excel ile toplu yükleme işlemlerini yöneten servis arayüzü.
 */
public interface SubscriberService {

    SubscriberResponseDto addSubscriber(SubscriberRequestDto dto);

    List<SubscriberResponseDto> getAllSubscribers();

    void deleteSubscriber(Long id);

    void toggleSubscriberStatus(Long id, boolean active);

    boolean unsubscribeByEmail(String email);

    int importSubscribersFromExcel(MultipartFile file);
}

