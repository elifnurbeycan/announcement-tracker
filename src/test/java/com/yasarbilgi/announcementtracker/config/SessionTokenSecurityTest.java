package com.yasarbilgi.announcementtracker.config;

import com.yasarbilgi.announcementtracker.dto.session.SessionToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTokenSecurityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Oturum token'ları JSON yanıtında tarayıcıya sızdırılmamalı")
    void sessionTokens_AreNotSerialized() throws Exception {
        String adminJson = objectMapper.writeValueAsString(new SessionToken("admin-secret"));
        String userJson = objectMapper.writeValueAsString(new SessionToken("user-secret"));

        assertThat(adminJson).doesNotContain("admin-secret", "token");
        assertThat(userJson).doesNotContain("user-secret", "token");
    }
}
