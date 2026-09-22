package com.yasarbilgi.announcementtracker.service;

import com.yasarbilgi.announcementtracker.dto.response.AdminUserDto;
import com.yasarbilgi.announcementtracker.dto.session.SessionToken;

public interface AuthService {

    SessionToken createOidcSession(String keycloakSubject, String username);

    void logout(String token);

    AdminUserDto validateToken(String token);
}
