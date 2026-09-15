package com.yasarbilgi.announcementtracker.config;

import com.yasarbilgi.announcementtracker.entity.AdminUser;
import com.yasarbilgi.announcementtracker.repository.AdminUserRepository;
import com.yasarbilgi.announcementtracker.util.PasswordEncoderHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private PasswordEncoderHelper passwordEncoderHelper;

    @InjectMocks
    private AdminInitializer adminInitializer;

    @Test
    @DisplayName("Admin kullanıcısı veritabanında yoksa varsayılan admin kullanıcısını oluşturmalı")
    void run_AdminDoesNotExist_ShouldCreateAdminUser() {
        when(adminUserRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoderHelper.encode("admin123")).thenReturn("encodedPassword");

        adminInitializer.run();

        verify(adminUserRepository, times(1)).save(any(AdminUser.class));
    }

    @Test
    @DisplayName("Admin kullanıcısı veritabanında zaten varsa tekrar oluşturmamalı")
    void run_AdminExists_ShouldSkipCreation() {
        when(adminUserRepository.existsByUsername("admin")).thenReturn(true);

        adminInitializer.run();

        verify(adminUserRepository, never()).save(any(AdminUser.class));
    }
}
