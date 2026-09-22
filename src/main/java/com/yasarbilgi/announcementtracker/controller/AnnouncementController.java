package com.yasarbilgi.announcementtracker.controller;

import com.yasarbilgi.announcementtracker.dto.response.ApiResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.AnnouncementResponseDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<AnnouncementResponseDto>>> getAnnouncements(
            @RequestParam(required = false) SiteType siteType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean hasAttachment,
            @PageableDefault(size = 20, sort = "announcementDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AnnouncementResponseDto> page = announcementService
                .getAllAnnouncements(siteType, search, hasAttachment, pageable);
        return ResponseEntity.ok(ApiResponseDto.ok("Announcements fetched successfully", page));
    }

    @GetMapping("/counts")
    public ResponseEntity<ApiResponseDto<Map<SiteType, Long>>> getAnnouncementCounts() {
        return ResponseEntity.ok(ApiResponseDto.ok(
                "Announcement counts fetched successfully",
                announcementService.getAnnouncementCounts()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<AnnouncementResponseDto>> getAnnouncementById(@PathVariable Long id) {
        AnnouncementResponseDto dto = announcementService.getAnnouncementById(id);
        return ResponseEntity.ok(ApiResponseDto.ok("Announcement found", dto));
    }

    @PostMapping({"/trigger", "/trigger-scrape"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponseDto<List<AnnouncementResponseDto>>> triggerScrape(
            @RequestParam(required = false) SiteType siteType) {

        List<AnnouncementResponseDto> newItems;
        if (siteType != null) {
            newItems = announcementService.triggerScrapeSite(siteType);
        } else {
            newItems = announcementService.triggerScrapeAll();
        }

        return ResponseEntity.ok(ApiResponseDto.ok(
                "Scraping executed successfully. Found " + newItems.size() + " new announcements.",
                newItems
        ));
    }

    @PostMapping("/notify-pending")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponseDto<Integer>> notifyPending() {
        int count = announcementService.notifyPendingAnnouncements();
        return ResponseEntity.ok(ApiResponseDto.ok(
                count + " kalıcı e-posta teslimatı kuyruğa alındı.", count));
    }

    @PostMapping("/send-test-email")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponseDto<Void>> sendTestEmail() {
        announcementService.sendTestEmail();
        return ResponseEntity.ok(ApiResponseDto.ok("Test email notification dispatched to subscribers"));
    }
}
