package com.yasarbilgi.announcementtracker.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriberRequestDto {

    @NotBlank(message = "Email address is required")
    @Email(message = "Invalid email format")
    private String email;

    private String fullName;
}
