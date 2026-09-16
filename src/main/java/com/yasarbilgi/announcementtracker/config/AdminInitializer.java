package com.yasarbilgi.announcementtracker.config;

import com.yasarbilgi.announcementtracker.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;

    @Override
    public void run(String... args) {
        long count = adminUserRepository.count();
        if (count == 0) {
            log.warn("GÜVENLİK UYARISI: Veritabanında hiçbir yönetici hesabı bulunamadı. Lütfen veritabanından elle bir admin kullanıcısı tanımlayın.");
        } else {
            log.info("Veritabanı güvenlik kontrolü tamamlandı: {} adet yönetici hesabı aktif.", count);
        }
    }
}
