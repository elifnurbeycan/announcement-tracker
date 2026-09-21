# e-Belge & KOSGEB Duyuru Takip Servisi 🔔

Bu proje, Gelir İdaresi Başkanlığı (GİB) e-Belge portalı ve KOSGEB resmi duyurularını otomatik olarak izleyen, veritabanına kaydeden, performans odaklı **Spring Boot (Java 21)**, **Spring Security**, **PostgreSQL** ve **Modern Single-Page Dashboard** tabanlı kurumsal bir izleme ve e-posta bildirim sistemidir.

---

## 🚀 Temel Özellikler

- **🌐 Çoklu Kaynak Web Kazıma (Multi-Source Scraping)**: GİB e-Belge ve KOSGEB portalı duyuruları, ek dosyaları (.pdf, .zip vb.) ve görselleri otomatik taranır.
- **⚡ Akıllı Erken Çıkış (3 Üst Üste Var Olan Duyuru Kuralı)**: Taramalar esnasında veritabanında daha önce kaydedilmiş 3 üst üste duyuruya rastlandığında tarama anında sonlandırılır.
- **🔀 Paralel Eş Zamanlı Kazıma**: `CompletableFuture` mimarisi ile tüm resmi siteler eş zamanlı (paralel) olarak taranır.
- **🔐 Hibrit Güvenlik Altyapısı (`CustomTokenAuthenticationFilter`)**: Hem Keycloak OAuth2 hem de yerel token (`SA-TOKEN-...`, `USER-TOKEN-...`) ve Cookie tabanlı kimlik doğrulama.
- **🏢 Departman ve Abone Portalı**:
  - **Super Admin Paneli (`dashboard.html`)**: Duyuruları, aboneleri, departmanları, tarama sıklığını ve kaynakları yönetme.
  - **Abone Portalı (`user-dashboard.html`)**: Çalışanların kendi takip tercihlerini ve ilgili duyuruları görüntüleyebildiği özel portal.
- **🗑️ Toplu Seçim ve Silme (Bulk Delete)**: "Toplu Seç" düğmeli modüler arayüz ve koyu temalı `ConfirmModal` onay pop-up'ı ile toplu abone ve departman silme.
- **✉️ Asenkron E-Posta Bildirimleri (`@Async`)**: Yeni duyurular oluştuğunda e-postalar arka planda ayrı thread pool (`emailExecutor`) üzerinde abonelerin tercih edilen site ve departmanlarına özel olarak asenkron iletilir.
- **📦 Hibernate JDBC Batching**: Toplu duyuru eklemeleri `batch_size: 50` ile tek bir SQL paketinde iletilerek veritabanı yükü %90 azaltılır.
- **📊 Dinamik Tarama Periyodu**: Tarama sıklığı (ör. 15 dk, 30 dk, 1 saat) yönetim panelinden anlık değiştirilebilir.
- **📁 Toplu Abone Aktarımı**: Excel (.xlsx, .xls) ve CSV dosyalarından toplu abone içe aktarma ve şablon indirme desteği.
- **🧪 Otomatik E2E UI ve Unit Test Kapsamı**: Playwright Java SDK ile uçtan uca UI testleri ve 98 adet unit/entegrasyon testi.

---

## 🛠️ Mimari ve Yeni Kaynak (Site) Ekleme

Proje **Strategy Pattern** ve **Spring Bean Auto-Registration** mimarisinde kurgulanmıştır. Yeni bir duyuru kaynağı eklemek için:

1. **`SiteType` Enum'ına Ekleme** (`src/main/java/com/yasarbilgi/announcementtracker/enums/SiteType.java`):
   ```java
   EBELGE_GIB("e-Belge GİB", "https://ebelge.gib.gov.tr/duyurular.html"),
   KOSGEB("KOSGEB Duyuruları", "https://www.kosgeb.gov.tr/site/tr/genel/duyurular"),
   YENI_KAYNAK("Yeni Kaynak", "https://ornek-site.gov.tr/duyurular");
   ```

2. **Scraper Sınıfı Oluşturma** (`src/main/java/com/yasarbilgi/announcementtracker/service/scraper/impl/`):
   `AbstractAnnouncementScraper` sınıfından türeterek `@Component` eklemeniz yeterlidir:
   ```java
   @Component
   public class YeniKaynakScraper extends AbstractAnnouncementScraper {

       @Override
       public SiteType getSiteType() {
           return SiteType.YENI_KAYNAK;
       }

       @Override
       public List<ScrapedAnnouncementDto> scrape(Predicate<String> hashExistsPredicate) {
           Document doc = fetchDocument(getSiteType().getBaseUrl());
           List<ScrapedAnnouncementDto> results = new ArrayList<>();
           // Ayrıştırma ve hashExistsPredicate.test(contentHash) ile 3 üst üste Erken Çıkış kontrolü
           return results;
       }
   }
   ```

---

## 🗄️ Veritabanı ve Güvenlik Altyapısı

- **PostgreSQL 17**: 3NF ve BCNF standartlarında ilişkisel veri modeli.
- **`ON DELETE CASCADE`**: İlişkili tablolarda (`subscriber_site_preferences`, `subscriber_departments`) güvenli silme kısıtlaması.
- **Performans İndeksleri**: `idx_announcement_hash`, `idx_announcement_site`, `idx_announcement_notified` ve `idx_announcement_date` indeksleri ile yüksek hızlı sorgulama.
- **Kimlik Doğrulama**: `CustomTokenAuthenticationFilter` ile Admin ve Abone rolleri için güvenli JWT/Token doğrulaması.

---

## 💻 Kullanılan Teknolojiler

- **Backend**: Java 21, Spring Boot 3, Spring Security, Spring Data JPA, Spring Async
- **Database**: PostgreSQL 17
- **Web Scraping**: Jsoup
- **Testing**: Playwright Java SDK (E2E UI), JUnit 5, Mockito, AssertJ
- **Frontend**: Modular Single-Page Application (Vanilla JS / React Standalone, Custom Glassmorphism Dark Theme)
- **Document Processing**: Apache POI (Excel / CSV parsing)

---

## ⚙️ Kurulum ve Çalıştırma

### 1. Veritabanı Hazırlığı
PostgreSQL üzerinde `announcement_tracker_db` veritabanını oluşturun:
```sql
CREATE DATABASE announcement_tracker_db;
```

### 2. Konfigürasyon (`src/main/resources/application.yaml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/announcement_tracker_db
    username: postgres
    password: YOUR_PASSWORD
  mail:
    host: smtp.gmail.com
    port: 587
    username: YOUR_EMAIL@gmail.com
    password: YOUR_APP_PASSWORD
```

### 3. Uygulamayı Başlatma
```bash
./mvnw spring-boot:run
```
Uygulama başlatıldıktan sonra panellere erişebilirsiniz:
- **Yönetim Paneli (Super Admin)**: `http://localhost:8080/admin-login.html` (Varsayılan Kullanıcı: `admin` / Şifre: `admin123`)
- **Abone Portalı**: `http://localhost:8080/user-login.html`

### 4. Testleri Çalıştırma (Unit + Playwright E2E UI)
```bash
./mvnw test
```
