package com.yasarbilgi.announcementtracker.controller;

import com.yasarbilgi.announcementtracker.dto.request.SubscriberRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.ApiResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.SubscriberResponseDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.service.SubscriberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(value = "/api/v1/subscribers", produces = "application/json;charset=UTF-8")
@RequiredArgsConstructor
public class SubscriberController {

    private final SubscriberService subscriberService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponseDto<List<SubscriberResponseDto>>> getAllSubscribers() {
        List<SubscriberResponseDto> list = subscriberService.getAllSubscribers();
        return ResponseEntity.ok(ApiResponseDto.ok("Subscribers retrieved", list));
    }

    @PostMapping({"", "/register"})
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

    @PostMapping("/upload-excel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponseDto<com.yasarbilgi.announcementtracker.dto.response.ExcelImportResultDto>> uploadExcelSubscribers(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "sites", required = false) List<SiteType> sites) {
        Set<SiteType> targetSites = (sites != null && !sites.isEmpty()) ? new java.util.HashSet<>(sites) : null;
        com.yasarbilgi.announcementtracker.dto.response.ExcelImportResultDto result = subscriberService.importSubscribersFromExcelDetailed(file, targetSites);
        
        String message;
        if (result.getErrorCount() == 0) {
            message = result.getSuccessCount() + " adet e-posta abonesi başarıyla içe aktarıldı.";
        } else {
            message = result.getSuccessCount() + " abone içe aktarıldı. " + result.getErrorCount() + " satırda doğrulama hatası oluştu.";
        }
        
        return ResponseEntity.ok(ApiResponseDto.ok(message, result));
    }

    @GetMapping("/template-excel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<byte[]> downloadExcelTemplate() {
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Aboneler");

            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("E-Posta");
            header.createCell(1).setCellValue("Ad Soyad");
            header.createCell(2).setCellValue("Departmanlar (Opsiyonel, Örn: Java, Backend)");

            org.apache.poi.ss.usermodel.Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("ornek.abone1@kurum.com");
            row1.createCell(1).setCellValue("Abone 1");
            row1.createCell(2).setCellValue("Java");

            org.apache.poi.ss.usermodel.Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("ornek.abone2@kurum.com");
            row2.createCell(1).setCellValue("Abone 2");
            row2.createCell(2).setCellValue("Java, Backend");

            org.apache.poi.ss.usermodel.Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("ornek.abone3@kurum.com");
            row3.createCell(1).setCellValue("Abone 3");
            row3.createCell(2).setCellValue(""); // Empty for Genel Çalışan

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                    .filename("Abone_Yukleme_Taslak_Sablonu.xlsx").build());
            headers.setContentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

            return new ResponseEntity<>(out.toByteArray(), headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            throw new RuntimeException("Taslak Excel dosyası oluşturulurken hata: " + e.getMessage(), e);
        }
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<ApiResponseDto<Boolean>> unsubscribeApi(@RequestParam String email) {
        boolean success = subscriberService.unsubscribeByEmail(email);
        if (success) {
            return ResponseEntity.ok(ApiResponseDto.ok("Aboneliğiniz başarıyla iptal edilmiştir.", true));
        } else {
            return ResponseEntity.badRequest().body(ApiResponseDto.error("Belirtilen e-posta adresi sistemde bulunamadı."));
        }
    }

    @GetMapping(value = "/unsubscribe", produces = org.springframework.http.MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> unsubscribeHtml(@RequestParam String email, CsrfToken csrfToken) {
        String safeEmail = HtmlUtils.htmlEscape(email == null ? "" : email);
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Abonelik İptali</title>" +
                unsubscribeStyles() + "</head><body><div class='card'>" +
                "<h2>Abonelikten Çık</h2>" +
                "<p><b>" + safeEmail + "</b> adresinin bildirim aboneliğini iptal etmek istediğinizi onaylayın.</p>" +
                "<form method='post' action='/api/v1/subscribers/unsubscribe/confirm'>" +
                "<input type='hidden' name='email' value='" + safeEmail + "'>" +
                "<input type='hidden' name='" + HtmlUtils.htmlEscape(csrfToken.getParameterName()) + "' value='" +
                HtmlUtils.htmlEscape(csrfToken.getToken()) + "'>" +
                "<button type='submit'>Aboneliği İptal Et</button></form>" +
                "<a href='/'>Vazgeç ve Ana Sayfaya Dön</a>" +
                "</div></body></html>";
        return ResponseEntity.ok(html);
    }

    @PostMapping(value = "/unsubscribe/confirm", produces = org.springframework.http.MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> unsubscribeConfirm(@RequestParam String email) {
        boolean success = subscriberService.unsubscribeByEmail(email);
        String safeEmail = HtmlUtils.htmlEscape(email == null ? "" : email);
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Abonelik İptali</title>" +
                unsubscribeStyles() + "</head><body>" +
                "<div class='card'>" +
                "<h2 class='" + (success ? "success" : "error") + "'>" + (success ? "Abonelik İptal Edildi" : "İşlem Başarısız") + "</h2>" +
                "<p>" + (success ? "<b>" + safeEmail + "</b> adresi e-Belge Duyuru Bildirim listesinden çıkarılmıştır. Artık e-posta bildirimi almayacaksınız." : "<b>" + safeEmail + "</b> adresi sistemde bulunamadı veya zaten abonelikten çıkarılmış.") + "</p>" +
                "<a href='/'>Ana Sayfaya Dön</a>" +
                "</div></body></html>";
        return ResponseEntity.ok(html);
    }

    private String unsubscribeStyles() {
        return "<style>body { font-family: sans-serif; background: #0f172a; color: white; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }" +
                ".card { background: #1e293b; padding: 40px; border-radius: 16px; text-align: center; max-width: 450px; border: 1px solid #334155; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }" +
                "h2 { color: #60a5fa; margin-bottom: 12px; }.success { color: #10b981; }.error { color: #ef4444; }" +
                "p { color: #94a3b8; font-size: 15px; line-height: 1.6; }" +
                "a, button { display: inline-block; margin: 20px 6px 0; padding: 10px 20px; border: 0; background: #2563eb; color: white; text-decoration: none; border-radius: 8px; font-weight: 600; cursor: pointer; }</style>";
    }
}
