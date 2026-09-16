package com.yasarbilgi.announcementtracker.dto.request;

import com.yasarbilgi.announcementtracker.enums.SiteType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentRequestDto {

    @NotBlank(message = "Departman adı boş olamaz.")
    private String name;

    private String description;

    private Set<SiteType> siteTypes;
}
