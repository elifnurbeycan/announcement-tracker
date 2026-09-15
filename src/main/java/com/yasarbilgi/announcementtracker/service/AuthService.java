package com.yasarbilgi.announcementtracker.service;

import com.yasarbilgi.announcementtracker.dto.request.LoginRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.AdminUserDto;
import com.yasarbilgi.announcementtracker.dto.response.LoginResponseDto;

public interface AuthService {

    LoginResponseDto login(LoginRequestDto request);

    void logout(String token);

    AdminUserDto validateToken(String token);
}
