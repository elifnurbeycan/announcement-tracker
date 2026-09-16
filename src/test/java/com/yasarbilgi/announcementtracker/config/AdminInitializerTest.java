package com.yasarbilgi.announcementtracker.config;

import com.yasarbilgi.announcementtracker.repository.AdminUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @InjectMocks
    private AdminInitializer adminInitializer;

    @Test
    @DisplayName("Veritabanında admin kullanıcısı yoksa uyarı logu basılmalı ve yazma yapılmamalı")
    void run_NoAdminUser_ShouldLogWarningWithoutWriting() {
        when(adminUserRepository.count()).thenReturn(0L);

        adminInitializer.run();

        verify(adminUserRepository, times(1)).count();
        verify(adminUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("Veritabanında admin kullanıcısı varsa kontrol başarıyla tamamlanmalı")
    void run_AdminExists_ShouldPassCheck() {
        when(adminUserRepository.count()).thenReturn(1L);

        adminInitializer.run();

        verify(adminUserRepository, times(1)).count();
        verify(adminUserRepository, never()).save(any());
    }
}
