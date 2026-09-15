package com.yasarbilgi.announcementtracker.controller;

import com.yasarbilgi.announcementtracker.dto.response.ApiResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.SiteDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sites")
public class SiteController {

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<SiteDto>>> getAllSites() {
        List<SiteDto> sites = Arrays.stream(SiteType.values())
                .map(site -> SiteDto.builder()
                        .name(site.name())
                        .displayName(site.getDisplayName())
                        .baseUrl(site.getBaseUrl())
                        .build())
                .toList();

        return ResponseEntity.ok(ApiResponseDto.ok("Duyuru kaynakları listelendi", sites));
    }
}
