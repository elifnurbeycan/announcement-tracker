package com.yasarbilgi.announcementtracker.service.impl;

import com.yasarbilgi.announcementtracker.entity.Announcement;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.service.EmailService;
import com.yasarbilgi.announcementtracker.service.UnsubscribeTokenService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * E-posta bildirimlerinin oluşturulması ve gönderilmesini sağlayan servis uygulaması.
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final UnsubscribeTokenService unsubscribeTokenService;

    public EmailServiceImpl(JavaMailSender mailSender, UnsubscribeTokenService unsubscribeTokenService) {
        this.mailSender = mailSender;
        this.unsubscribeTokenService = unsubscribeTokenService;
    }

    @Value("${announcement.tracker.email.from:noreply@announcementtracker.com}")
    private String mailFrom;

    @Value("${announcement.tracker.app-base-url:http://localhost:8080}")
    private String appBaseUrl;

    /**
     * Yeni duyuruları toplu e-posta olarak kayıtlı abonelere asenkron gönderir.
     */
    @Override
    @Async("emailExecutor")
    public void sendAnnouncementNotification(List<Announcement> announcements, List<String> recipientEmails) {
        if (announcements == null || announcements.isEmpty()) {
            log.info("Gönderilecek yeni duyuru bulunamadı.");
            return;
        }

        if (recipientEmails == null || recipientEmails.isEmpty()) {
            log.warn("Bildirim gönderilecek aktif e-posta abonesi bulunamadı.");
            return;
        }

        for (String recipient : recipientEmails) {
            try {
                sendAnnouncementNotificationNow(announcements, recipient);
            } catch (Exception exception) {
                log.error("Asenkron e-posta bildirimi başarısız oldu ({}): {}", recipient, exception.getMessage());
            }
        }
    }

    @Override
    public void sendAnnouncementNotificationNow(List<Announcement> announcements, String recipientEmail) {
        if (announcements == null || announcements.isEmpty()) {
            throw new IllegalArgumentException("Gönderilecek duyuru bulunamadı.");
        }
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("Bildirim alıcısı boş olamaz.");
        }
        String subject = "Yeni Duyuru Bildirimi (" + announcements.size() + " Yeni Duyuru)";
        String htmlBody = buildHtmlEmailBody(announcements, recipientEmail);
        sendHtmlEmail(recipientEmail, subject, htmlBody);
    }

    /**
     * Tek bir duyuruyu hedef abonelere asenkron olarak e-posta ile iletir.
     */
    @Override
    @Async("emailExecutor")
    public void sendSingleAnnouncementNotification(Announcement announcement, List<String> recipientEmails) {
        sendAnnouncementNotification(List.of(announcement), recipientEmails);
    }

    /**
     * HTML e-posta mesajını JavaMailSender kullanarak alıcıya gönderir.
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (isTestOrDummyEmail(to)) {
            log.info("Test/Dummy e-posta alıcısı tespit edildi ({}). Gerçek SMTP iletimi atlandı.", to);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("E-posta bildirimi başarıyla gönderildi: {}", to);
        } catch (MessagingException e) {
            log.error("E-posta gönderimi başarısız oldu ({}): {}", to, e.getMessage());
            throw new MailSendException("E-posta mesajı oluşturulamadı: " + to, e);
        } catch (Exception e) {
            log.error("E-posta iletim hatası: {}", e.getMessage());
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new MailSendException("E-posta iletimi başarısız oldu: " + to, e);
        }
    }

    /**
     * Kurumsal standartlara uygun HTML e-posta şablonunu oluşturur.
     */
    private String buildHtmlEmailBody(List<Announcement> announcements, String recipientEmail) {
        boolean onlyTaklitTagsis = announcements.stream()
                .allMatch(announcement -> announcement.getSourceSite() == SiteType.TAKLIT_TAGSIS);
        boolean onlyEbelge = announcements.stream()
                .allMatch(announcement -> announcement.getSourceSite() == SiteType.EBELGE_GIB);

        String headerSubtitle = onlyTaklitTagsis
                ? "Tarım ve Orman Bakanlığı kamuoyu duyuruları"
                : onlyEbelge
                    ? "Gelir İdaresi Başkanlığı e-Belge duyuruları"
                    : "Resmî kaynaklardan güncel duyurular";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        sb.append("<style>")
          .append("body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;background:#eef2f7;color:#172033;margin:0;padding:0;-webkit-font-smoothing:antialiased}")
          .append(".wrapper{width:100%;background:#eef2f7;padding:28px 12px;box-sizing:border-box}")
          .append(".container{max-width:640px;margin:0 auto;background:#fff;border-radius:18px;overflow:hidden;box-shadow:0 12px 32px rgba(15,23,42,.10)}")
          .append(".hero-header{background:linear-gradient(135deg,#101b3f,#2448a3);padding:34px 34px 30px;color:#fff}")
          .append(".eyebrow{font-size:12px;font-weight:700;letter-spacing:1.2px;text-transform:uppercase;color:#bfdbfe;margin-bottom:8px}")
          .append(".hero-title{font-size:25px;font-weight:800;color:#fff;margin:0;letter-spacing:-.4px}")
          .append(".hero-subtitle{font-size:14px;color:#dbeafe;line-height:1.5;margin-top:7px}")
          .append(".intro{padding:22px 28px 0;color:#475569;font-size:14px;line-height:1.6}")
          .append(".content-padding{padding:20px 28px 30px}")
          .append(".card{border:1px solid #dbe3ee;border-radius:14px;padding:22px;margin-bottom:18px;background:#fff;box-shadow:0 3px 10px rgba(15,23,42,.035)}")
          .append(".card:last-child{margin-bottom:0}")
          .append(".meta-table{width:100%;border-collapse:collapse;margin-bottom:14px}")
          .append(".badge{display:inline-block;background:#e8f0ff;color:#2156c9;font-size:11px;font-weight:800;padding:6px 10px;border-radius:999px;letter-spacing:.35px}")
          .append(".date{color:#64748b;font-size:13px;font-weight:600;text-align:right;white-space:nowrap}")
          .append(".title{font-size:19px;font-weight:750;color:#101828;margin:0 0 14px;line-height:1.4}")
          .append(".content{font-size:15px;color:#475569;line-height:1.7;margin-bottom:18px}")
          .append(".detail-table{width:100%;border-collapse:separate;border-spacing:0;border:1px solid #e2e8f0;border-radius:11px;overflow:hidden;margin:6px 0 18px}")
          .append(".detail-table td{padding:10px 12px;border-bottom:1px solid #e8edf4;font-size:14px;line-height:1.45;vertical-align:top}")
          .append(".detail-table tr:last-child td{border-bottom:0}")
          .append(".detail-label{width:34%;background:#f8fafc;color:#64748b;font-weight:650}")
          .append(".detail-value{color:#172033;font-weight:600;word-break:break-word}")
          .append(".alert-value{color:#b42318;background:#fff8f6}")
          .append(".cta-btn-primary{display:block;text-align:center;background:#2864dc;color:#fff!important;text-decoration:none;padding:13px 18px;border-radius:9px;font-size:14px;font-weight:750;box-sizing:border-box;margin-top:16px}")
          .append(".cta-btn-secondary{display:block;text-align:center;background:#172033;color:#fff!important;text-decoration:none;padding:12px 18px;border-radius:9px;font-size:13px;font-weight:700;box-sizing:border-box;margin-top:10px}")
          .append(".footer{background:#f8fafc;padding:21px 24px;font-size:12px;line-height:1.55;color:#8290a5;text-align:center;border-top:1px solid #e2e8f0}")
          .append("@media(max-width:620px){.wrapper{padding:0}.container{border-radius:0}.hero-header{padding:28px 22px}.intro{padding:20px 18px 0}.content-padding{padding:16px 18px 24px}.card{padding:18px}.hero-title{font-size:22px}.title{font-size:17px}.detail-table td{display:block;width:auto!important;border-bottom:0}.detail-table .detail-label{padding-bottom:3px}.detail-table .detail-value{padding-top:2px;border-bottom:1px solid #e8edf4}.detail-table tr:last-child .detail-value{border-bottom:0}}")
          .append("</style></head><body>");

        sb.append("<div class='wrapper'><div class='container'>");

        sb.append("<div class='hero-header'>");
        sb.append("<div class='eyebrow'>Duyuru Takip Sistemi</div>");
        sb.append("<div class='hero-title'>Yeni duyuru bildirimi</div>");
        sb.append("<div class='hero-subtitle'>").append(headerSubtitle).append("</div>");
        sb.append("</div>");

        sb.append("<div class='intro'>Takip ettiğiniz kaynaklarda ")
                .append(announcements.size())
                .append(" yeni kayıt yayımlandı. Ayrıntıları aşağıda inceleyebilirsiniz.</div>");
        sb.append("<div class='content-padding'>");

        for (Announcement a : announcements) {
            appendAnnouncementCard(sb, a);
        }

        sb.append("</div>"); // content-padding sonu

        String unsubToken = unsubscribeTokenService.generate(recipientEmail);
        String unsubUrl = escapeHtml(appBaseUrl + "/api/v1/subscribers/unsubscribe?token=" + unsubToken);

        sb.append("<div class='footer'>");
        sb.append("<div>Bu ileti Duyuru Takip Sistemi tarafından otomatik olarak oluşturulmuştur.</div>");
        sb.append("<div style='margin-top:7px'>Bildirimleri almak istemiyorsanız <a href='").append(unsubUrl).append("' style='color:#475569;text-decoration:underline'>abonelikten çıkabilirsiniz</a>.</div>");
        sb.append("</div></div></div></body></html>");

        return sb.toString();
    }

    private void appendAnnouncementCard(StringBuilder sb, Announcement announcement) {
        String dateFormatted = announcement.getAnnouncementDate() != null
                ? announcement.getAnnouncementDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                : "Tarih belirtilmedi";
        String sourceName = announcement.getSourceSite() != null
                ? announcement.getSourceSite().getDisplayName()
                : "Resmî duyuru";

        sb.append("<div class='card'>");
        sb.append("<table class='meta-table' cellpadding='0' cellspacing='0'><tr>");
        sb.append("<td><span class='badge'>").append(escapeHtml(sourceName)).append("</span></td>");
        if (announcement.getSourceSite() != SiteType.TAKLIT_TAGSIS) {
            sb.append("<td class='date'>").append(dateFormatted).append("</td>");
        }
        sb.append("</tr></table>");

        if (announcement.getSourceSite() == SiteType.TAKLIT_TAGSIS) {
            appendTaklitTagsisContent(sb, announcement, dateFormatted);
        } else {
            sb.append("<div class='title'>").append(escapeHtml(announcement.getTitle())).append("</div>");
            sb.append("<div class='content'>").append(escapeHtml(announcement.getContent())).append("</div>");
        }

        appendImageAndAttachment(sb, announcement);
        appendSourceButton(sb, announcement);
        sb.append("</div>");
    }

    private void appendTaklitTagsisContent(
            StringBuilder sb,
            Announcement announcement,
            String announcementDate) {
        Map<String, String> details = parseTaklitTagsisContent(announcement.getContent());
        String company = details.getOrDefault("Firma", "");
        String product = details.getOrDefault("Ürün", "");
        String heading = !product.isBlank() ? product : (!company.isBlank() ? company : announcement.getTitle());

        sb.append("<div class='title'>").append(escapeHtml(heading)).append("</div>");
        sb.append("<table class='detail-table' cellpadding='0' cellspacing='0'>");
        if (announcement.getAnnouncementDate() != null) {
            appendDetailRow(sb, "Kamuoyu Duyuru Tarihi", announcementDate, false);
        }
        appendDetailRow(sb, "Firma Adı", company, false);
        appendDetailRow(sb, "Marka", details.get("Marka"), false);
        appendDetailRow(sb, "Ürün Adı", product, false);
        appendDetailRow(sb, "Uygunsuzluk", details.get("Uygunsuzluk"), true);
        appendDetailRow(sb, "Parti / Seri No", details.get("Parti/Seri No"), false);
        appendDetailRow(sb, "İl / İlçe", details.get("İl/İlçe"), false);
        appendDetailRow(sb, "Ürün Grubu", details.get("Ürün Grubu"), false);
        appendDetailRow(sb, "Liste", details.get("Liste"), false);
        sb.append("</table>");
    }

    private Map<String, String> parseTaklitTagsisContent(String content) {
        Map<String, String> details = new LinkedHashMap<>();
        if (content == null || content.isBlank()) {
            return details;
        }

        for (String segment : content.split("\\s*\\|\\s*")) {
            int separator = segment.indexOf(':');
            if (separator <= 0) {
                continue;
            }
            String key = segment.substring(0, separator).trim();
            String value = segment.substring(separator + 1).trim();
            if (!key.isBlank() && !value.isBlank()) {
                details.put(key, value);
            }
        }
        return details;
    }

    private void appendDetailRow(StringBuilder sb, String label, String value, boolean alert) {
        if (value == null || value.isBlank()) {
            return;
        }
        sb.append("<tr><td class='detail-label'>").append(escapeHtml(label)).append("</td>");
        sb.append("<td class='detail-value")
                .append(alert ? " alert-value" : "")
                .append("'>")
                .append(escapeHtml(value))
                .append("</td></tr>");
    }

    private void appendImageAndAttachment(StringBuilder sb, Announcement announcement) {
        if (announcement.getImageUrl() != null && !announcement.getImageUrl().isBlank()) {
            sb.append("<div style='margin:16px 0;text-align:center'>")
                    .append("<img src='").append(sanitizeUrl(announcement.getImageUrl()))
                    .append("' style='max-width:100%;border-radius:10px;border:1px solid #e2e8f0' alt='Duyuru görseli'></div>");
        }

        if (announcement.getAttachmentUrl() == null || announcement.getAttachmentUrl().isBlank()) {
            return;
        }

        String attachmentUrl = sanitizeUrl(announcement.getAttachmentUrl());
        boolean pdf = announcement.getAttachmentUrl().toLowerCase().endsWith(".pdf");
        sb.append("<div style='margin:18px 0;padding:16px;border:1px solid #dbe3ee;border-radius:10px;background:#f8fafc'>")
                .append("<div style='font-size:13px;font-weight:700;color:#172033;margin-bottom:8px'>Duyuru eki</div>")
                .append("<div style='font-size:12px;color:#64748b;word-break:break-all'>")
                .append(attachmentUrl).append("</div>")
                .append("<a href='").append(attachmentUrl).append("' class='cta-btn-secondary' target='_blank'>")
                .append(pdf ? "PDF belgesini aç" : "Ek dosyayı aç")
                .append("</a></div>");
    }

    private void appendSourceButton(StringBuilder sb, Announcement announcement) {
        String rawSourceUrl = announcement.getSourceUrl() != null && !announcement.getSourceUrl().isBlank()
                ? announcement.getSourceUrl()
                : announcement.getSourceSite() != null ? announcement.getSourceSite().getBaseUrl() : "#";
        sb.append("<a href='").append(sanitizeUrl(rawSourceUrl))
                .append("' class='cta-btn-primary' target='_blank'>Resmî kaydı görüntüle</a>");
    }

    private String sanitizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "#";
        }
        String trimmed = url.trim();
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return escapeHtml(trimmed);
        }
        return "#";
    }

    /**
     * Güvenlik amacıyla HTML özel karakterlerini kaçış karakterlerine çevirir.
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    @Override
    @Async("emailExecutor")
    public void sendWelcomeAndActivationEmail(String email, String fullName, String passwordResetUrlOrToken) {
        if (email == null || email.isBlank()) return;

        if (isTestOrDummyEmail(email)) {
            log.info("Test/Dummy hoş geldin e-posta alıcısı tespit edildi ({}). Gerçek SMTP iletimi atlandı.", email);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("Duyuru Takip Sistemi Hesabınız Oluşturuldu");

            String setPasswordUrl = (passwordResetUrlOrToken != null && passwordResetUrlOrToken.startsWith("http"))
                    ? passwordResetUrlOrToken
                    : appBaseUrl + "/login";

            String name = (fullName != null && !fullName.isBlank()) ? fullName : email.split("@")[0];

            String content = """
                <!DOCTYPE html>
                <html>
                <head><meta charset='UTF-8'></head>
                <body style='font-family: Arial, sans-serif; background-color: #f8fafc; padding: 20px;'>
                  <div style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);'>
                    <div style='background: linear-gradient(135deg, #1e293b, #0f172a); color: white; padding: 24px; text-align: center;'>
                      <h2 style='margin: 0; font-size: 20px;'>Duyuru Takip Sistemine Hoş Geldiniz!</h2>
                    </div>
                    <div style='padding: 24px; color: #334155; line-height: 1.6;'>
                      <p>Merhaba <strong>%s</strong>,</p>
                      <p>Duyuru takip sistemimize aboneliğiniz yönetici tarafından başarıyla oluşturulmuştur. Artık seçtiğiniz resmi kaynaklardan yayımlanan güncel duyuruları e-posta olarak alacaksınız.</p>
                      <p>Parolanızı belirlemeniz için Keycloak tarafından ayrıca tek kullanımlık ve süreli bir güvenlik e-postası gönderilecektir.</p>
                      <div style='text-align: center; margin: 30px 0;'>
                        <a href='%s' style='background-color: #2563eb; color: white; padding: 12px 24px; border-radius: 6px; text-decoration: none; font-weight: bold; display: inline-block;'>Kullanıcı Giriş Ekranına Git &rarr;</a>
                      </div>
                      <p style='font-size: 13px; color: #64748b;'>Parolanız yalnızca güvenli Keycloak sunucusunda tutulur; duyuru takip uygulaması parolanızı görmez.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(escapeHtml(name), setPasswordUrl);

            helper.setText(content, true);
            log.info("==========================================================");
            log.info("WELCOME & PASSWORD SETUP LINK FOR {}: {}", email, setPasswordUrl);
            log.info("==========================================================");
            mailSender.send(mimeMessage);
            log.info("Welcome & password setup email sent to: {}", email);
        } catch (Exception e) {
            log.warn("SMTP email send failed (local SMTP settings may be unconfigured). Direct login: {}/login", appBaseUrl);
            log.error("Failed to send welcome email to {}: {}", email, e.getMessage());
        }
    }

    private boolean isTestOrDummyEmail(String email) {
        if (email == null || email.isBlank()) {
            return true;
        }
        String lower = email.trim().toLowerCase();

        // Test veya Sahte Alan Adları (Domains)
        if (lower.endsWith("@kurum.com") ||
            lower.endsWith("@example.com") ||
            lower.endsWith("@example.org") ||
            lower.endsWith("@example.net") ||
            lower.endsWith("@test.com") ||
            lower.endsWith("@localhost") ||
            lower.endsWith("@invalid") ||
            lower.endsWith("@domain.com") ||
            lower.endsWith("@sample.com")) {
            return true;
        }

        // Test / E2E Alıcı Ön Ekleri ve İsim Kalıpları
        if (lower.startsWith("e2e-") ||
            lower.startsWith("test-") ||
            lower.startsWith("dummy-") ||
            lower.startsWith("fake-") ||
            lower.contains("e2e-sub") ||
            lower.contains("dummy") ||
            lower.contains("fake")) {
            return true;
        }

        return false;
    }
}
