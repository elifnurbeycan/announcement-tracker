# e-Belge & KOSGEB Duyuru Takip Servisi 🔔

Bu proje, Gelir İdaresi Başkanlığı (GİB) e-Belge portalı ve KOSGEB resmi duyurularını otomatik olarak izleyen, veritabanına kaydeden, performans odaklı **Spring Boot (Java 21)**, **Spring Security**, **PostgreSQL** ve **Modern Single-Page Dashboard** tabanlı kurumsal bir izleme ve e-posta bildirim sistemidir.

---

## Frontend build

Tarayıcıda Babel çalıştırılmaz. JSX kaynaklarında değişiklik yaptıktan sonra derlenmiş statik dosyaları güncelleyin:

```powershell
node scripts/build-frontend.js
```

Derlenmiş çıktılar `src/main/resources/static/js/dist` altında tutulur ve uygulama bu dosyaları sunar.

---

## 🚀 Temel Özellikler

- **🌐 Çoklu Kaynak Web Kazıma (Multi-Source Scraping)**: GİB e-Belge ve KOSGEB portalı duyuruları, ek dosyaları (.pdf, .zip vb.) ve görselleri otomatik taranır.
- **⚡ Akıllı Erken Çıkış (3 Üst Üste Var Olan Duyuru Kuralı)**: Taramalar esnasında veritabanında daha önce kaydedilmiş 3 üst üste duyuruya rastlandığında tarama anında sonlandırılır.
- **🔀 Paralel Eş Zamanlı Kazıma**: `CompletableFuture` mimarisi ile tüm resmi siteler eş zamanlı (paralel) olarak taranır.
- **🔐 Güvenli Oturum Altyapısı**: Oturum kimlikleri JavaScript'e açılmadan `HttpOnly` cookie'de tutulur; değiştirici istekler CSRF token'ıyla doğrulanır.
- **🏢 Departman ve Abone Portalı**:
  - **Super Admin Paneli (`dashboard.html`)**: Duyuruları, aboneleri, departmanları, tarama sıklığını ve kaynakları yönetme.
  - **Abone Portalı (`user-dashboard.html`)**: Çalışanların kendi takip tercihlerini ve ilgili duyuruları görüntüleyebildiği özel portal.
- **🗑️ Toplu Seçim ve Silme (Bulk Delete)**: "Toplu Seç" düğmeli modüler arayüz ve koyu temalı `ConfirmModal` onay pop-up'ı ile toplu abone ve departman silme.
- **✉️ Kalıcı E-Posta Outbox'ı**: Her duyuru–alıcı teslimatı PostgreSQL'de `PENDING/SENDING/SENT/FAILED` durumlarıyla izlenir; SMTP hataları artan gecikmeyle yeniden denenir ve uygulama yeniden başlasa bile teslimatlar kaybolmaz.
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
- **Kimlik Doğrulama**: Keycloak Authorization Code akışı ve `SessionAuthenticationFilter` ile rol tabanlı sunucu tarafı oturum doğrulaması; `HttpOnly`, `Secure`, `SameSite` cookie ve CSRF koruması. Kullanıcı parolaları uygulamaya gönderilmez.

---

## 💻 Kullanılan Teknolojiler

- **Backend**: Java 21, Spring Boot 4, Spring Security, Spring Data JPA, Spring Async
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

### 2. Konfigürasyon

Parola ve erişim anahtarlarını YAML dosyalarına yazmayın. `.env.example` içeriğini kendi ortamınızın secret yönetim sistemine aktarın. Canlı ortamda en az şu ayarlar zorunludur:

```bash
SPRING_PROFILES_ACTIVE=prod
SESSION_COOKIE_SECURE=true
DB_PASSWORD=change-me
SPRING_MAIL_PASSWORD=change-me
APP_BASE_URL=https://announcements.example.com
KEYCLOAK_CLIENT_SECRET=change-me
LOCAL_LOGIN_ENABLED=false
```

`prod` profili HTTPS, `HttpOnly`, `Secure` ve `SameSite=Lax` oturum cookie'lerini kullanır. TLS sonlandırması reverse proxy'de yapılıyorsa proxy'nin `Forwarded` veya `X-Forwarded-*` başlıklarını doğru iletmesi gerekir.

Keycloak istemcisi confidential client olarak tanımlanmalı, Standard Flow etkinleştirilmeli ve geçerli yönlendirme adresine `https://announcements.example.com/login/oauth2/code/keycloak` eklenmelidir. Yönetici hesaplarında `ADMIN`, `SUPER_ADMIN`, `ROLE_ADMIN` veya `ROLE_SUPER_ADMIN` realm rollerinden biri bulunmalıdır. Canlı profilde yerel parola girişi kapalıdır; tarayıcı Authorization Code akışıyla Keycloak'a yönlendirilir.

#### Docker ile Yerel Keycloak

Depodaki Compose tanımı Keycloak'ı `http://localhost:8180` adresinde, ayrı bir PostgreSQL veritabanıyla çalıştırır ve `announcement-tracker-realm` yapılandırmasını ilk açılışta içe aktarır. `keycloak-config` servisi tema, SMTP, güvenli yönlendirme adresleri ve servis hesabının en düşük kullanıcı-yönetim rollerini her açılışta idempotent olarak uygular:

```powershell
docker compose up -d
docker compose logs keycloak-config
```

Yönetim konsolu `http://localhost:8180/admin` adresindedir. İlk girişte kullanıcı adı `admin`, parola ise yukarıda verdiğiniz değerdir. `announcement-tracker-realm` içinde uygulamanın yerel yöneticisiyle aynı kullanıcı adına sahip bir kullanıcı oluşturup `ROLE_SUPER_ADMIN` rolünü atayın. Çalışan girişi için Keycloak kullanıcısının e-posta adresi uygulamadaki aktif abonenin e-posta adresiyle aynı olmalıdır.

`.env` dosyasında `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`,
`SPRING_MAIL_PASSWORD` ve `MAIL_FROM` doldurulmalıdır. Keycloak, imzalı ve süreli
`UPDATE_PASSWORD` bağlantısını bu SMTP hesabıyla gönderir. Uygulamayı normal IDE Run
butonuyla veya aşağıdaki komutla başlatabilirsiniz:

```powershell
.\mvnw.cmd spring-boot:run
```

Kalıcı yönetim konsolu hesabı `.env` içindeki `KEYCLOAK_CONSOLE_ADMIN_USERNAME` ve
`KEYCLOAK_CONSOLE_ADMIN_PASSWORD` değerleriyle oluşturulur. Bootstrap hesabı yalnızca ilk
kurulum içindir; canlı ortamda bu değerler `.env` yerine kurumun secret manager'ından verilmelidir.

Yerel istemci Authorization Code + PKCE kullanır; Direct Access Grant, joker yönlendirme adresleri ve herkese açık kullanıcı kaydı kapalıdır. `start-dev` yalnızca geliştirme içindir. Canlı Keycloak kurulumu HTTPS, sabit hostname, secret yönetimi ve production `start` modu ile ayrıca yapılandırılmalıdır.

Super Admin panelinden abone oluşturulduğunda backend aynı e-posta için Keycloak hesabını ve `ROLE_SUBSCRIBER` rolünü otomatik oluşturur ve tek kullanımlık şifre belirleme e-postasını gönderir. Mevcut aboneler için Aboneler tablosundaki **Şifre Bağlantısı** işlemi aynı güvenli e-postayı yeniden yollar. Ad-soyad/e-posta güncelleme, aktif-pasif yapma, Excel içe aktarma ve silme işlemleri de Keycloak'a yansıtılır. Uygulama bunun için kullanıcı parolası yerine yalnızca `manage-users`, `query-users`, `view-users` ve realm bilgisini okumak için `view-realm` rollerine sahip `announcement-tracker-admin` servis hesabını kullanır.

### 3. Uygulamayı Başlatma
```bash
./mvnw spring-boot:run
```
Uygulama başlatıldıktan sonra panellere erişebilirsiniz:
- **Yönetim Paneli (Super Admin)**: `http://localhost:8080/admin-login.html`
- **Abone Portalı**: `http://localhost:8080/user-login.html`

### 4. Testleri Çalıştırma (Unit + PostgreSQL + Playwright E2E UI)

Test profili H2 kullanmaz. Entegrasyon testleri Testcontainers aracılığıyla geçici bir
PostgreSQL 17 konteyneri oluşturduğu için testlerden önce Docker Desktop çalışır durumda olmalıdır.

```bash
./mvnw test
```
