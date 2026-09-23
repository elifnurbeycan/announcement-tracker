package com.yasarbilgi.announcementtracker.controller;

import com.yasarbilgi.announcementtracker.service.SubscriberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/api/v1/subscribers/unsubscribe")
@RequiredArgsConstructor
public class UnsubscribeController {

    private final SubscriberService subscriberService;

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirmationPage(@RequestParam String token, CsrfToken csrfToken) {
        String safeToken = HtmlUtils.htmlEscape(token == null ? "" : token);
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Abonelik İptali</title>" +
                styles() + "</head><body><div class='card'>" +
                "<h2>Abonelikten Çık</h2>" +
                "<p>Bildirim aboneliğinizi iptal etmek istediğinizi onaylıyor musunuz?</p>" +
                "<form method='post' action='/api/v1/subscribers/unsubscribe/confirm'>" +
                "<input type='hidden' name='token' value='" + safeToken + "'>" +
                "<input type='hidden' name='" + HtmlUtils.htmlEscape(csrfToken.getParameterName()) + "' value='" +
                HtmlUtils.htmlEscape(csrfToken.getToken()) + "'>" +
                "<button type='submit'>Aboneliği İptal Et</button></form>" +
                "<a href='/'>Vazgeç ve Giriş Sayfasına Dön</a>" +
                "</div></body></html>";
        return ResponseEntity.ok(html);
    }

    @PostMapping(value = "/confirm", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirm(@RequestParam String token) {
        subscriberService.unsubscribeByToken(token);
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Abonelik İptali</title>" +
                styles() + "</head><body><div class='card'>" +
                "<h2 class='success'>Abonelik İptal Edildi</h2>" +
                "<p>Aboneliğiniz Duyuru Takip Sistemi bildirim listesinden çıkarılmıştır. Artık e-posta bildirimi almayacaksınız.</p>" +
                "<a href='/'>Giriş Sayfasına Dön</a>" +
                "</div></body></html>";
        return ResponseEntity.ok(html);
    }

    private String styles() {
        return "<style>body { font-family: sans-serif; background: #0f172a; color: white; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }" +
                ".card { background: #1e293b; padding: 40px; border-radius: 16px; text-align: center; max-width: 450px; border: 1px solid #334155; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }" +
                "h2 { color: #60a5fa; margin-bottom: 12px; }.success { color: #10b981; }.error { color: #ef4444; }" +
                "p { color: #94a3b8; font-size: 15px; line-height: 1.6; }" +
                "a, button { display: inline-block; margin: 20px 6px 0; padding: 10px 20px; border: 0; background: #2563eb; color: white; text-decoration: none; border-radius: 8px; font-weight: 600; cursor: pointer; }</style>";
    }
}
