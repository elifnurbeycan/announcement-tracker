package com.yasarbilgi.announcementtracker.service.impl;

import com.yasarbilgi.announcementtracker.entity.Announcement;
import com.yasarbilgi.announcementtracker.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * E-posta bildirimlerinin oluşturulması ve gönderilmesini sağlayan servis uygulaması.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

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

        String subject = "Yeni Duyuru Bildirimi (" + announcements.size() + " Yeni Duyuru)";

        for (String recipient : recipientEmails) {
            String htmlBody = buildHtmlEmailBody(announcements, recipient);
            sendHtmlEmail(recipient, subject, htmlBody);
        }
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
            log.info("=== E-POSTA İÇERİĞİ (Geliştirici İnceleme) [{}] ===\nKonu: {}\nİçerik:\n{}", to, subject, htmlContent);
        } catch (Exception e) {
            log.error("E-posta iletim hatası: {}", e.getMessage());
            log.info("=== E-POSTA İÇERİĞİ (Geliştirici İnceleme) [{}] ===\nKonu: {}\nİçerik:\n{}", to, subject, htmlContent);
        }
    }

    /**
     * Kurumsal standartlara uygun HTML e-posta şablonunu oluşturur.
     */
    private String buildHtmlEmailBody(List<Announcement> announcements, String recipientEmail) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        sb.append("<style>")
          .append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f1f5f9; color: #1e293b; margin: 0; padding: 0; -webkit-font-smoothing: antialiased; }")
          .append(".wrapper { width: 100%; background-color: #f1f5f9; padding: 24px 0; }")
          .append(".container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.08); }")
          .append(".hero-header { background: linear-gradient(135deg, #0f172a, #1e3a8a); text-align: center; padding: 32px 20px; color: #ffffff; }")
          .append(".hero-title { font-size: 24px; font-weight: 800; color: #ffffff; margin: 0; letter-spacing: -0.5px; }")
          .append(".hero-subtitle { font-size: 14px; color: #93c5fd; font-weight: 500; margin-top: 6px; }")
          .append(".content-padding { padding: 28px 24px; }")
          .append(".card { border: 1px solid #e2e8f0; border-radius: 12px; padding: 24px; margin-bottom: 24px; background: #ffffff; box-shadow: 0 2px 8px rgba(0,0,0,0.03); }")
          .append(".badge { display: inline-block; background: #dbeafe; color: #1d4ed8; font-size: 13px; font-weight: 700; padding: 6px 14px; border-radius: 20px; text-transform: uppercase; letter-spacing: 0.5px; }")
          .append(".date { color: #64748b; font-size: 14px; font-weight: 600; float: right; }")
          .append(".title { font-size: 20px; font-weight: 800; color: #0f172a; margin: 16px 0 12px 0; line-height: 1.4; }")
          .append(".content { font-size: 16px; color: #334155; line-height: 1.7; margin-bottom: 20px; }")
          .append(".cta-btn-primary { display: block; width: 100%; text-align: center; background: linear-gradient(135deg, #2563eb, #1d4ed8); color: #ffffff !important; text-decoration: none; padding: 14px 20px; border-radius: 10px; font-size: 15px; font-weight: 700; box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3); box-sizing: border-box; margin-top: 14px; }")
          .append(".cta-btn-secondary { display: block; width: 100%; text-align: center; background: linear-gradient(135deg, #0f172a, #1e293b); color: #ffffff !important; text-decoration: none; padding: 12px 20px; border-radius: 10px; font-size: 14px; font-weight: 700; box-shadow: 0 4px 12px rgba(15, 23, 42, 0.25); box-sizing: border-box; margin-top: 10px; }")
          .append(".footer { background: #f8fafc; padding: 20px; font-size: 13px; color: #94a3b8; text-align: center; border-top: 1px solid #e2e8f0; }")
          .append("</style></head><body>");

        sb.append("<div class='wrapper'><div class='container'>");

        // Kurumsal Üst Başlık Banner
        sb.append("<div class='hero-header'>");
        sb.append("  <div class='hero-title'>e-Belge Duyuru & Bildirim Servisi</div>");
        sb.append("  <div class='hero-subtitle'>Gelir İdaresi Başkanlığı Resmi Güncellemeleri</div>");
        sb.append("</div>");

        sb.append("<div class='content-padding'>");

        for (Announcement a : announcements) {
            String dateFormatted = a.getAnnouncementDate() != null
                    ? a.getAnnouncementDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    : "";

            sb.append("<div class='card'>");
            sb.append("<span class='badge'>").append(a.getSourceSite().getDisplayName()).append("</span>");
            sb.append("<span class='date'>").append(dateFormatted).append("</span>");
            sb.append("<div style='clear: both;'></div>");

            sb.append("<div class='title'>").append(escapeHtml(a.getTitle())).append("</div>");
            sb.append("<div class='content'>").append(escapeHtml(a.getContent())).append("</div>");

            // Duyuru içerisinde gömülü görsel var ise gösterilir
            if (a.getImageUrl() != null && !a.getImageUrl().isBlank()) {
                sb.append("<div style='margin: 16px 0; text-align: center;'>");
                sb.append("  <img src='").append(a.getImageUrl()).append("' style='max-width: 100%; border-radius: 10px; border: 1px solid #e2e8f0;' alt='Duyuru Görseli' />");
                sb.append("</div>");
            }

            // Ek dosya / PDF bağlantısı (E-posta istemcisi uyumlu tablo yerleşimi)
            if (a.getAttachmentUrl() != null && !a.getAttachmentUrl().isBlank()) {
                String attachUrl = a.getAttachmentUrl();
                boolean isPdf = attachUrl.endsWith(".pdf");
                String badgeText = isPdf ? "PDF DOKÜMANI" : "DOSYA PAKETİ";
                String badgeBg = isPdf ? "#dc2626" : "#2563eb";
                String btnText = isPdf ? "Ek PDF Dokümanını İndir &darr;" : "Ek Dosya Paketini İndir &darr;";

                sb.append("<table width='100%' cellpadding='0' cellspacing='0' style='margin: 22px 0 16px 0; border: 1px solid #cbd5e1; border-radius: 12px; background: #f8fafc; border-collapse: separate;'>");
                sb.append("  <tr><td style='padding: 18px 20px;'>");
                
                sb.append("    <table width='100%' cellpadding='0' cellspacing='0'>");
                sb.append("      <tr>");
                sb.append("        <td style='font-size: 14px; font-weight: 700; color: #0f172a;'>📎 Duyuru Ek Dosyası</td>");
                sb.append("        <td align='right'><span style='background: ").append(badgeBg).append("; color: #ffffff; font-size: 11px; font-weight: 700; padding: 4px 10px; border-radius: 6px;'>").append(badgeText).append("</span></td>");
                sb.append("      </tr>");
                sb.append("    </table>");

                sb.append("    <div style='font-size: 13px; color: #1d4ed8; background: #ffffff; padding: 12px 14px; border-radius: 8px; border: 1px solid #cbd5e1; word-break: break-all; overflow-wrap: anywhere; margin: 14px 0 16px 0; font-family: -apple-system, BlinkMacSystemFont, Roboto, sans-serif;'>");
                sb.append("      <a href='").append(attachUrl).append("' style='color: #1d4ed8 !important; font-weight: 600; text-decoration: underline; word-break: break-all; overflow-wrap: anywhere;' target='_blank'>").append(escapeHtml(attachUrl)).append("</a>");
                sb.append("    </div>");

                sb.append("    <a href='").append(attachUrl).append("' class='cta-btn-secondary' target='_blank'>").append(btnText).append("</a>");

                sb.append("  </td></tr>");
                sb.append("</table>");
            }

            // Duyurunun asıl orijinal web sayfasına yönlendiren ana buton
            String sourceUrl = (a.getSourceUrl() != null && !a.getSourceUrl().isBlank()) 
                    ? a.getSourceUrl() 
                    : a.getSourceSite().getBaseUrl();

            sb.append("<div style='margin-top: 20px;'>");
            sb.append("  <a href='").append(sourceUrl).append("' class='cta-btn-primary' target='_blank'>Duyurunun Asıl Web Sayfasına Git &rarr;</a>");
            sb.append("</div>");

            sb.append("</div>");
        }


        sb.append("</div>"); // content-padding sonu


        String unsubUrl = appBaseUrl + "/api/v1/subscribers/unsubscribe?email=" + recipientEmail;

        sb.append("<div class='footer'>");
        sb.append("<p>Bu e-posta Java 21 & Spring Boot Duyuru Takip Servisi tarafından otomatik olarak oluşturulmuştur.</p>");
        sb.append("<p style='margin-top: 8px; font-size: 12px;'>E-posta listesinden ayrılmak için <a href='").append(unsubUrl).append("' style='color: #475569; text-decoration: underline;'>Abonelikten Çıkın</a>.</p>");
        sb.append("</div></div></div></body></html>");

        return sb.toString();
    }

    /**
     * Güvenlik amacıyla HTML özel karakterlerini kaçış karakterlerine çevirir.
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}


