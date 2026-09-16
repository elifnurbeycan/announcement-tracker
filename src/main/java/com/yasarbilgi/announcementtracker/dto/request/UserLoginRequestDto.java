package com.yasarbilgi.announcementtracker.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginRequestDto {

    @NotBlank(message = "E-posta adresi zorunludur")
    @Email(message = "Geçersiz e-posta biçimi")
    @Size(max = 255, message = "E-posta en fazla 255 karakter olabilir")
    private String email;

    @NotBlank(message = "Şifre zorunludur")
    @Size(min = 6, max = 100, message = "Şifre en az 6, en fazla 100 karakter olabilir")
    private String password;
}
