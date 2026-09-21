package com.yasarbilgi.announcementtracker.service;

import com.yasarbilgi.announcementtracker.dto.request.LoginRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.AdminUserDto;
import com.yasarbilgi.announcementtracker.dto.response.LoginResponseDto;

public interface AuthService {

    LoginResponseDto login(LoginRequestDto request);

    LoginResponseDto createOidcSession(String username);

    void logout(String token);

    AdminUserDto validateToken(String token);
}
