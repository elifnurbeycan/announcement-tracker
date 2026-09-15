package com.yasarbilgi.announcementtracker.config;

import com.yasarbilgi.announcementtracker.entity.AdminUser;
import com.yasarbilgi.announcementtracker.repository.AdminUserRepository;
import com.yasarbilgi.announcementtracker.util.PasswordEncoderHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoderHelper passwordEncoderHelper;

    @Override
    public void run(String... args) {
        if (!adminUserRepository.existsByUsername("admin")) {
            log.info("No superadmin user found in database. Initializing default 'admin' account...");

            AdminUser superAdmin = AdminUser.builder()
                    .username("admin")
                    .passwordHash(passwordEncoderHelper.encode("admin123"))
                    .fullName("Süper Admin")
                    .build();

            adminUserRepository.save(superAdmin);
            log.info("Default SuperAdmin user ('admin') created successfully.");
        } else {
            log.info("SuperAdmin user 'admin' already exists in database.");
        }
    }
}
