package com.yasarbilgi.announcementtracker.dto.request;

import com.yasarbilgi.announcementtracker.enums.SiteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentRequestDto {

    @NotBlank(message = "Departman adı boş olamaz.")
    @Size(min = 2, max = 100, message = "Departman adı 2 ile 100 karakter arasında olmalıdır.")
    private String name;

    @Size(max = 500, message = "Departman açıklaması en fazla 500 karakter olabilir.")
    private String description;

    private Set<SiteType> siteTypes;
}
