package com.yasarbilgi.announcementtracker.config;

import tools.jackson.databind.ObjectMapper;
import com.yasarbilgi.announcementtracker.dto.response.LoginResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.UserLoginResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginResponseSecurityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Oturum token'ları JSON yanıtında tarayıcıya sızdırılmamalı")
    void sessionTokens_AreNotSerialized() throws Exception {
        String adminJson = objectMapper.writeValueAsString(LoginResponseDto.builder()
                .token("admin-secret")
                .username("admin")
                .build());
        String userJson = objectMapper.writeValueAsString(UserLoginResponseDto.builder()
                .token("user-secret")
                .email("user@example.com")
                .build());

        assertThat(adminJson).doesNotContain("admin-secret", "token");
        assertThat(userJson).doesNotContain("user-secret", "token");
    }
}
