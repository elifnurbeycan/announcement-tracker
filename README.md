# e-Belge & KOSGEB Duyuru Takip Servisi 🔔

Bu proje, Gelir İdaresi Başkanlığı (GİB) e-Belge portalı ve KOSGEB resmi duyurularını otomatik olarak izleyen, veritabanına kaydeden, performans odaklı **Spring Boot (Java 21)** ve **Modern Web Paneli** tabanlı kurumsal bir izleme ve e-posta bildirim sistemidir.

---

## 📸 Ekran Görüntüleri

### Kurumsal Yönetim Paneli
![Yönetim Paneli](docs/images/dashboard-ui.png)

---

## 🚀 Temel Özellikler

- **🌐 Çoklu Kaynak Web Kazıma (Multi-Source Scraping)**: GİB e-Belge ve KOSGEB portalı duyuruları, ek dosyaları (.pdf, .zip vb.) ve görselleri otomatik taranır.
- **⚡ Akıllı Erken Çıkış (Early Exit)**: Taramalar esnasında veritabanında daha önce taranmış olan ilk duyuruya ulaşıldığı an **1 milisaniye** içinde tarama sonlandırılır.
- **🔀 Paralel Eş Zamanlı Kazıma**: `CompletableFuture` mimarisi ile tüm resmi siteler eş zamanlı (paralel) olarak taranır.
- **✉️ Asenkron E-Posta Bildirimleri (`@Async`)**: Yeni duyurular oluştuğunda e-postalar arka planda ayrı thread pool (`emailExecutor`) üzerinde asenkron olarak gönderilir, kullanıcıyı bekletmez.
- **📦 Hibernate JDBC Batching**: Toplu duyuru eklemeleri `batch_size: 50` ile tek bir SQL paketinde iletilerek veritabanı yükü %90 azaltılır.
- **📊 Dinamik Tarama Periyodu**: Tarama sıklığı (ör. 15 dk, 20 dk, 1 saat) yönetim panelinden anlık değiştirilebilir ve ayarlar saklanır.
- **📁 Toplu Abone Aktarımı**: Excel (.xlsx, .xls) ve CSV dosyalarından toplu abone içe aktarma ve site bazlı tercih atama desteklenir.
- **🧪 Otomatik E2E UI ve Unit Test Kapsamı**: Playwright Java SDK ile uçtan uca UI testleri ve 98 adet unit/entegrasyon testi ile %100 yeşil test altyapısı.

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
           // Ayrıştırma ve hashExistsPredicate.test(contentHash) ile Erken Çıkış kontrolü
           return results;
       }
   }
   ```

---

## 🗄️ Veritabanı ve Normalizasyon (PostgreSQL)

- **1NF / 2NF / 3NF / BCNF Uyumlu**: İlişkisel veri yapısı 3NF ve BCNF standartlarına tam uygundur.
- **`ON DELETE CASCADE`**: `subscriber_site_preferences` tablosunda veritabanı ve Hibernate seviyesinde güvenli silme kısıtlaması tanımlanmıştır.
- **Performans İndeksleri**: `idx_announcement_hash`, `idx_announcement_site`, `idx_announcement_notified` ve `idx_announcement_date` indeksleri ile yüksek hızlı sorgulama.

---

## 💻 Kullanılan Teknolojiler

- **Backend**: Java 21, Spring Boot 3, Spring Data JPA, Spring Async
- **Database**: PostgreSQL 17
- **Web Scraping**: Jsoup
- **Testing**: Playwright Java SDK (E2E UI), JUnit 5, Mockito, AssertJ
- **Frontend**: Single-Page Responsive UI (Vanilla CSS, Modern Glassmorphism Theme)
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
Uygulama başlatıldıktan sonra `http://localhost:8080` adresinden yönetim paneline erişebilirsiniz.

### 4. Testleri Çalıştırma (Unit + Playwright E2E UI)
```bash
# E2E UI testleri varsayılan olarak yerel tarayıcı (Edge/Chrome) kanalını kullanır
./mvnw test
```
