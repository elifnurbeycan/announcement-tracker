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

    @NotBlank(message = "Aktivasyon jetonu zorunludur")
    @Size(min = 36, max = 100, message = "Aktivasyon jetonu geçersiz biçimde (en az 36, en fazla 100 karakter olmalıdır)")
    private String token;

    @NotBlank(message = "Şifre zorunludur")
    @Size(min = 6, max = 100, message = "Şifre en az 6, en fazla 100 karakter olmalıdır")
    private String password;
}
