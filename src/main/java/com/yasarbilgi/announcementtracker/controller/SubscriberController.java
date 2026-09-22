package com.yasarbilgi.announcementtracker.controller;

import com.yasarbilgi.announcementtracker.dto.request.SubscriberRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.ApiResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.SubscriberResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.ExcelImportResultDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.service.SubscriberExcelTemplateService;
import com.yasarbilgi.announcementtracker.service.SubscriberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping(value = "/api/v1/subscribers", produces = "application/json;charset=UTF-8")
@RequiredArgsConstructor
public class SubscriberController {

    private final SubscriberService subscriberService;
    private final SubscriberExcelTemplateService excelTemplateService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponseDto<List<SubscriberResponseDto>>> getAllSubscribers() {
        List<SubscriberResponseDto> list = subscriberService.getAllSubscribers();
        return ResponseEntity.ok(ApiResponseDto.ok("Subscribers retrieved", list));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponseDto<SubscriberResponseDto>> addSubscriber(
            @Valid @RequestBody SubscriberRequestDto dto) {
        SubscriberResponseDto response = subscriberService.addSubscriber(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok("Subscriber registered successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponseDto<SubscriberResponseDto>> updateSubscriber(
            @PathVariable Long id,
            @Valid @RequestBody SubscriberRequestDto dto) {
        SubscriberResponseDto response = subscriberService.updateSubscriber(id, dto);
        return ResponseEntity.ok(ApiResponseDto.ok("Abone bilgileri başarıyla güncellendi.", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponseDto<Void>> deleteSubscriber(@PathVariable Long id) {
        subscriberService.deleteSubscriber(id);
        return ResponseEntity.ok(ApiResponseDto.ok("Subscriber removed successfully"));
    }

    @PostMapping("/batch-delete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponseDto<Void>> deleteSubscribersBatch(@RequestBody List<Long> ids) {
        subscriberService.deleteSubscribersBatch(ids);
        return ResponseEntity.ok(ApiResponseDto.ok("Seçilen aboneler başarıyla silindi."));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponseDto<Void>> toggleStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        subscriberService.toggleSubscriberStatus(id, active);
        return ResponseEntity.ok(ApiResponseDto.ok("Abone aktiflik durumu güncellendi."));
    }

    @PostMapping("/{id}/password-setup-email")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponseDto<Void>> sendPasswordSetupEmail(@PathVariable Long id) {
        subscriberService.sendPasswordSetupEmail(id);
        return ResponseEntity.ok(ApiResponseDto.ok(
                "Güvenli ve süreli şifre belirleme bağlantısı aboneye gönderildi."));
    }

    @PatchMapping("/{id}/sites")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponseDto<SubscriberResponseDto>> updateSitePreferences(
            @PathVariable Long id,
            @RequestBody Set<SiteType> sites) {
        SubscriberResponseDto response = subscriberService.updateSitePreferences(id, sites);
        return ResponseEntity.ok(ApiResponseDto.ok("Abone site tercihleri güncellendi.", response));
    }

    @PatchMapping("/{id}/departments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponseDto<SubscriberResponseDto>> updateSubscriberDepartments(
            @PathVariable Long id,
            @RequestBody Set<Long> departmentIds) {
        SubscriberResponseDto response = subscriberService.updateSubscriberDepartments(id, departmentIds);
        return ResponseEntity.ok(ApiResponseDto.ok("Abone departman tercihleri güncellendi.", response));
    }

    @PostMapping(value = "/upload-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponseDto<ExcelImportResultDto>> uploadExcelSubscribers(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sites", required = false) List<SiteType> sites) {
        Set<SiteType> targetSites = (sites != null && !sites.isEmpty()) ? new HashSet<>(sites) : null;
        ExcelImportResultDto result = subscriberService.importSubscribersFromExcelDetailed(file, targetSites);
        
        String message;
        if (result.getErrorCount() == 0) {
            message = result.getSuccessCount() + " adet e-posta abonesi başarıyla içe aktarıldı.";
        } else {
            message = result.getSuccessCount() + " abone içe aktarıldı. " + result.getErrorCount() + " satırda doğrulama hatası oluştu.";
        }
        
        return ResponseEntity.ok(ApiResponseDto.ok(message, result));
    }

    @GetMapping(
            value = "/template-excel",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<byte[]> downloadExcelTemplate() {
        byte[] template = excelTemplateService.createTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("Abone_Yukleme_Taslak_Sablonu.xlsx").build());
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return new ResponseEntity<>(template, headers, HttpStatus.OK);
    }
}
