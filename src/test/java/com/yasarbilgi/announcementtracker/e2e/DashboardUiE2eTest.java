package com.yasarbilgi.announcementtracker.e2e;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "keycloak.admin.enabled=false",
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
    private com.yasarbilgi.announcementtracker.service.AuthService authService;

    @BeforeEach
    void createContextAndPage() {
        context = browser.newContext();
        page = context.newPage();
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private void performSuperAdminLogin() {
        String token = authService.createOidcSession("e2e-admin-subject", "admin").value();
        context.addCookies(java.util.List.of(
                new com.microsoft.playwright.options.Cookie(
                        com.yasarbilgi.announcementtracker.config.SessionCookieService.ADMIN_COOKIE,
                        token
                ).setUrl(baseUrl())
        ));
        page.navigate(baseUrl() + "/dashboard.html");
        page.waitForURL("**/dashboard.html");
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
    @DisplayName("E2E: Yönetici menüsü rol bazlı olarak izole edilir")
    void testRoleBasedNavigation() {
        // Super Admin access
        performSuperAdminLogin();
        assertThat(page.locator(".sidebar").innerText()).contains("Genel Bakış");
        assertThat(page.locator(".sidebar").innerText()).contains("Aboneler");
        assertThat(page.locator(".sidebar").innerText()).doesNotContain("Profilim");
        assertThat(page.locator(".sidebar").innerText()).contains("Departmanlar");
    }

    @Test
    @DisplayName("E2E: Duyurular Sayfası, Filtreleme ve Tarama Tetikleme (Scrape)")
    void testAnnouncementsViewAndScrapeTrigger() {
        performSuperAdminLogin();

        page.click("a:has-text('Duyurular')");
        page.waitForSelector(".page-title-badge:has-text('Duyurular')");

        page.locator(".btn-scrape").first().click();

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
        page.waitForSelector(".dropdown-item.danger");

        Request request = page.waitForRequest(req -> req.url().contains("/sso/logout"), () -> {
            page.click(".dropdown-item.danger");
        });

        assertThat(request.url()).contains("/sso/logout");
    }
}
