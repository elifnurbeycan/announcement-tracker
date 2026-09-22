package com.yasarbilgi.announcementtracker.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "keycloak.admin.enabled=false",
                "app.security.sso-enabled=true",
                "app.security.local-login-enabled=true",
                "announcement.tracker.enabled=false"
        })
@ActiveProfiles("test")
class DashboardUiE2eTest {

    @LocalServerPort
    private int port;

    private static Playwright playwright;
    private static Browser browser;

    // Headless mode for automated CI/CD & local test suite execution
    private static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("playwright.headless", "false"));

    private BrowserContext context;
    private Page page;

    @BeforeAll
    static void launchBrowser() {
        System.setProperty("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");
        Playwright.CreateOptions options = new Playwright.CreateOptions();
        options.setEnv(java.util.Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1"));
        playwright = Playwright.create(options);

        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(HEADLESS)
                .setSlowMo(HEADLESS ? 0 : 300);

        try {
            browser = playwright.chromium().launch(launchOptions.setChannel("chrome"));
        } catch (Exception chromeFailure) {
            try {
                browser = playwright.chromium().launch(launchOptions.setChannel("msedge"));
            } catch (Exception edgeFailure) {
                browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                        .setHeadless(HEADLESS)
                        .setSlowMo(HEADLESS ? 0 : 300));
            }
        }
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.yasarbilgi.announcementtracker.repository.AdminUserRepository adminUserRepository;

    private static final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder TEST_PASSWORD_ENCODER = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

    @BeforeEach
    void createContextAndPage() {
        com.yasarbilgi.announcementtracker.entity.AdminUser admin = adminUserRepository.findByUsername("admin")
                .orElseGet(() -> com.yasarbilgi.announcementtracker.entity.AdminUser.builder().username("admin").build());
        if (admin.getPasswordHash() == null || !TEST_PASSWORD_ENCODER.matches("admin123", admin.getPasswordHash())) {
            admin.setPasswordHash(TEST_PASSWORD_ENCODER.encode("admin123"));
            admin.setFullName("System Admin");
            adminUserRepository.save(admin);
        }

        context = browser.newContext();
        page = context.newPage();
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private void performSuperAdminLogin() {
        page.navigate(baseUrl() + "/admin-login.html");
        // Production UI is SSO-only. Tests use the explicitly enabled local test
        // endpoint programmatically, without exposing a password form to users.
        page.evaluate("""
                async () => {
                    await window.AuthService.loginAdmin('admin', 'admin123');
                    window.location.href = '/dashboard.html';
                }
                """);
        page.waitForURL("**/dashboard.html");
    }

    @Test
    @DisplayName("E2E: Admin giriş sayfası yalnızca kurumsal SSO gösterir")
    void testAdminLoginAndErrorHandling() {
        page.navigate(baseUrl() + "/admin-login.html");

        assertThat(page.title()).contains("Super Admin Giriş Paneli");
        assertThat(page.locator("h2").innerText()).contains("Yönetici Girişi");

        assertThat(page.getByText("Kurumsal SSO (Keycloak Admin) ile Giriş Yap").isVisible()).isTrue();
        assertThat(page.locator("input[type='text']").count()).isZero();
        assertThat(page.locator("input[type='password']").count()).isZero();
    }

    @Test
    @DisplayName("E2E: Sidebar Navigasyonu ve Sayfa Geçişleri (Layout & Route State)")
    void testSidebarNavigationAndRouteState() {
        performSuperAdminLogin();

        // 1. Overview Page default
        assertThat(page.locator(".page-title-badge").innerText()).contains("Genel Bakış");

        // 2. Click Announcements tab
        page.click("a:has-text('Duyurular')");
        page.waitForSelector(".page-title-badge:has-text('Duyurular')");
        assertThat(page.locator(".page-title-badge").innerText()).contains("Duyurular");

        // 3. Click Subscribers tab
        page.click("a:has-text('Aboneler')");
        page.waitForSelector(".page-title-badge:has-text('Abone Yönetimi')");
        assertThat(page.locator(".page-title-badge").innerText()).contains("Abone Yönetimi");

        // 4. Click Departments tab
        page.click("a:has-text('Departmanlar')");
        page.waitForSelector(".page-title-badge:has-text('Departman Yönetimi')");
        assertThat(page.locator(".page-title-badge").innerText()).contains("Departman Yönetimi");

        // 5. Click Sources tab
        page.click("a:has-text('Kaynaklar')");
        page.waitForSelector(".page-title-badge:has-text('Kaynak Takibi')");
        assertThat(page.locator(".page-title-badge").innerText()).contains("Kaynak Takibi");

        // 6. Click Settings tab
        page.click("a:has-text('Ayarlar')");
        page.waitForSelector(".page-title-badge:has-text('Sistem Ayarları')");
        assertThat(page.locator(".page-title-badge").innerText()).contains("Sistem Ayarları");

        // Toggle sidebar collapse
        page.click(".toggle-btn");
        assertThat(page.locator(".sidebar").getAttribute("class")).contains("collapsed");
    }

    @Test
    @DisplayName("E2E: Kullanıcı girişi yalnızca kurumsal SSO sunar")
    void testRoleBasedNavigation() {
        // Super Admin access
        performSuperAdminLogin();
        assertThat(page.locator(".sidebar").innerText()).contains("Genel Bakış");
        assertThat(page.locator(".sidebar").innerText()).contains("Aboneler");
        assertThat(page.locator(".sidebar").innerText()).doesNotContain("Profilim");
        assertThat(page.locator(".sidebar").innerText()).contains("Departmanlar");

        // Kullanıcı parolası uygulamaya verilmez; giriş Keycloak Authorization Code akışına yönlenir.
        Page userPage = context.newPage();
        userPage.navigate(baseUrl() + "/user-login.html");
        Locator ssoLink = userPage.locator("a:has-text('Kurumsal SSO ile Giriş Yap')");
        ssoLink.waitFor();
        assertThat(ssoLink.getAttribute("href")).isEqualTo("/oauth2/authorization/keycloak");
        assertThat(userPage.locator("input[type='email']").count()).isZero();
        assertThat(userPage.locator("input[type='password']").count()).isZero();
    }

    @Test
    @DisplayName("E2E: Duyurular Sayfası, Filtreleme ve Tarama Tetikleme (Scrape)")
    void testAnnouncementsViewAndScrapeTrigger() {
        performSuperAdminLogin();

        page.click("a:has-text('Duyurular')");
        page.waitForSelector(".page-title-badge:has-text('Duyurular')");

        // Search interaction
        Locator searchInput = page.locator("input[placeholder*='Duyuru ara']");
        searchInput.waitFor();
        searchInput.fill("e-Belge");
        assertThat(searchInput.inputValue()).isEqualTo("e-Belge");

        // Filter pills
        page.click("button:has-text('KOSGEB')");
        page.click("button:has-text('Tüm Kaynaklar')");

        // Scrape button click and toast verification
        Locator scrapeBtn = page.locator(".btn-scrape").first();
        scrapeBtn.click();

        Locator toast = page.locator(".toast");
        toast.waitFor();
        assertThat(toast.innerText()).contains("Tarama tamamlandı");
    }

    @Test
    @DisplayName("E2E: Abone Yönetimi (Ekleme Modalı, Listeleme, Durum Değiştirme)")
    void testSubscriberCrudFlowAndModals() {
        performSuperAdminLogin();

        page.click("a:has-text('Aboneler')");
        page.waitForSelector(".page-title-badge:has-text('Abone Yönetimi')");

        // Open Add Subscriber Modal
        page.click("button:has-text('+ Abone Ekle')");
        page.waitForSelector(".modal-card");

        String uniqueEmail = "e2e-sub-" + UUID.randomUUID().toString().substring(0, 8) + "@kurum.com";
        page.fill(".modal-card input[type='text']", "Test Abonesi");
        page.fill(".modal-card input[type='email']", uniqueEmail);

        page.click(".modal-card button[type='submit']");

        // Toast verification
        Locator toast = page.locator(".toast");
        toast.waitFor();
        assertThat(toast.innerText()).contains("Abone ve kurumsal giriş hesabı oluşturuldu");

        // Verify subscriber appears in table
        page.fill("input[placeholder*='filtrele']", uniqueEmail);
        page.waitForSelector("td:has-text('" + uniqueEmail + "')");
        assertThat(page.locator("td:has-text('" + uniqueEmail + "')").isVisible()).isTrue();
    }

    @Test
    @DisplayName("E2E: Departman Yönetimi (Ekleme Modalı, Listeleme ve Düzenleme)")
    void testDepartmentCrudFlowAndModals() {
        performSuperAdminLogin();

        page.click("a:has-text('Departmanlar')");
        page.waitForSelector(".page-title-badge:has-text('Departman Yönetimi')");

        // Open Add Department Modal
        page.click("button:has-text('+ Departman Ekle')");
        page.waitForSelector(".modal-card");

        String deptName = "E2E Test Dept " + UUID.randomUUID().toString().substring(0, 4);
        page.fill(".modal-card input[type='text']", deptName);

        page.click(".modal-card button[type='submit']");

        // Toast verification
        Locator toast = page.locator(".toast");
        toast.waitFor();
        assertThat(toast.innerText()).contains("Departman oluşturuldu");

        // Verify department appears in list
        Locator departmentCell = page.locator("td:has-text('" + deptName + "')");
        departmentCell.waitFor();
        assertThat(departmentCell.isVisible()).isTrue();
    }

    @Test
    @DisplayName("E2E: Excel İçe Aktar Modalı ve Şablon İndirme Etkileşimi")
    void testExcelImportModalAndTemplateDownload() {
        performSuperAdminLogin();

        page.click("a:has-text('Aboneler')");
        page.click("button:has-text('Excel/CSV')");
        page.waitForSelector(".modal-card");

        assertThat(page.locator(".modal-card h3").innerText()).contains("Toplu İçe Aktar");

        // Verify template download button exists
        Locator downloadBtn = page.locator("button:has-text('Excel Şablonunu İndir')");
        assertThat(downloadBtn.isVisible()).isTrue();

        // Close modal
        page.click(".modal-card button:has-text('Kapat')");
    }

    @Test
    @DisplayName("E2E: Sistem Ayarları (Tarama Periyodu Güncelleme)")
    void testSettingsScrapePeriod() {
        performSuperAdminLogin();

        page.click("a:has-text('Ayarlar')");
        page.waitForSelector(".page-title-badge:has-text('Sistem Ayarları')");

        page.selectOption("select", "30");

        Locator toast = page.locator(".toast");
        toast.waitFor();
        assertThat(toast.innerText()).contains("Tarama periyodu güncellendi");
    }

    @Test
    @DisplayName("E2E: Oturumu Güvenli Çıkış Yapma (Logout)")
    void testLogoutFlow() {
        performSuperAdminLogin();

        page.click(".user-trigger");
        page.click(".dropdown-item.danger");

        page.waitForURL("**/admin-login.html");
        assertThat(page.url()).contains("/admin-login.html");
    }
}
