package com.yasarbilgi.announcementtracker.controller;

import com.yasarbilgi.announcementtracker.dto.request.SubscriberRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.ApiResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.SubscriberResponseDto;
import com.yasarbilgi.announcementtracker.service.SubscriberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscribers")
@RequiredArgsConstructor
public class    SubscriberController {

    private final SubscriberService subscriberService;

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<SubscriberResponseDto>>> getAllSubscribers() {
        List<SubscriberResponseDto> list = subscriberService.getAllSubscribers();
        return ResponseEntity.ok(ApiResponseDto.ok("Subscribers retrieved", list));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDto<SubscriberResponseDto>> addSubscriber(
            @Valid @RequestBody SubscriberRequestDto dto) {
        SubscriberResponseDto response = subscriberService.addSubscriber(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok("Subscriber registered successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteSubscriber(@PathVariable Long id) {
        subscriberService.deleteSubscriber(id);
        return ResponseEntity.ok(ApiResponseDto.ok("Subscriber removed successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponseDto<Void>> toggleStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        subscriberService.toggleSubscriberStatus(id, active);
        return ResponseEntity.ok(ApiResponseDto.ok("Abone aktiflik durumu güncellendi."));
    }

    @PostMapping("/upload-excel")
    public ResponseEntity<ApiResponseDto<Integer>> uploadExcelSubscribers(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        int count = subscriberService.importSubscribersFromExcel(file);
        return ResponseEntity.ok(ApiResponseDto.ok(count + " adet e-posta abonesi başarıyla içe aktarıldı.", count));
    }

    @GetMapping("/template-excel")
    public ResponseEntity<byte[]> downloadExcelTemplate() {
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Aboneler");

            // Header Row
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("E-Posta");
            header.createCell(1).setCellValue("Ad Soyad");

            // Example Data Rows
            org.apache.poi.ss.usermodel.Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("ornek.abone1@firma.com");
            row1.createCell(1).setCellValue("Ahmet Yılmaz");

            org.apache.poi.ss.usermodel.Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("ornek.abone2@firma.com");
            row2.createCell(1).setCellValue("Ayşe Kaya");

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

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
    public ResponseEntity<String> unsubscribeHtml(@RequestParam String email) {
        boolean success = subscriberService.unsubscribeByEmail(email);
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Abonelik İptali</title>" +
                "<style>body { font-family: sans-serif; background: #0f172a; color: white; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }" +
                ".card { background: #1e293b; padding: 40px; border-radius: 16px; text-align: center; max-width: 450px; border: 1px solid #334155; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }" +
                "h2 { color: " + (success ? "#10b981" : "#ef4444") + "; margin-bottom: 12px; }" +
                "p { color: #94a3b8; font-size: 15px; line-height: 1.6; }" +
                "a { display: inline-block; margin-top: 20px; padding: 10px 20px; background: #2563eb; color: white; text-decoration: none; border-radius: 8px; font-weight: 600; }</style></head><body>" +
                "<div class='card'>" +
                "<h2>" + (success ? "Abonelik İptal Edildi" : "İşlem Başarısız") + "</h2>" +
                "<p>" + (success ? "<b>" + email + "</b> adresi e-Belge Duyuru Bildirim listesinden çıkarılmıştır. Artık e-posta bildirimi almayacaksınız." : "<b>" + email + "</b> adresi sistemde bulunamadı veya zaten abonelikten çıkarılmış.") + "</p>" +
                "<a href='/'>Ana Sayfaya Dön</a>" +
                "</div></body></html>";
        return ResponseEntity.ok(html);
    }
}

