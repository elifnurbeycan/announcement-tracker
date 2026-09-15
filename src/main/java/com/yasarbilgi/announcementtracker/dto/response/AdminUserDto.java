package com.yasarbilgi.announcementtracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {

    private Long id;
    private String username;
    private String fullName;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
