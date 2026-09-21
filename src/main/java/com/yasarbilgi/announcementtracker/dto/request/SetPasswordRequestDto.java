package com.yasarbilgi.announcementtracker.dto.request;

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
public class SetPasswordRequestDto {

    private String token;

    private String email;

    @NotBlank(message = "Şifre zorunludur")
    @Size(min = 6, max = 100, message = "Şifre en az 6, en fazla 100 karakter olmalıdır")
    private String password;
}
