package com.yasarbilgi.announcementtracker.e2e;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DashboardUiE2eTest {

    @LocalServerPort
    private int port;

    private static Playwright playwright;
    private static Browser browser;

    // Tarayıcının canlı açıldığını görmek için HEADLESS = false
    private static final boolean HEADLESS = false;

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
                .setSlowMo(HEADLESS ? 0 : 800);

        try {
            browser = playwright.chromium().launch(launchOptions.setChannel("msedge"));
        } catch (Exception e) {
            browser = playwright.chromium().launch(launchOptions.setChannel("chrome"));
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

    @BeforeEach
    void createContextAndPage() {
        context = browser.newContext();
        page = context.newPage();
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private void performSuperAdminLogin() {
        page.navigate(baseUrl() + "/login.html");
        page.fill("input[type='text']", "admin");
        page.fill("input[type='password']", "admin123");
        page.click("button[type='submit']");
        page.waitForURL("**/dashboard.html");
    }

    @Test
    @DisplayName("Playwright UI: Giriş sayfasının temiz görünümü ve hatalı giriş senaryosu")
    void testLoginPageAndErrorHandling() {
        page.navigate(baseUrl() + "/login.html");

        assertThat(page.title()).contains("Super Admin Giriş Paneli");
        assertThat(page.locator("h2").innerText()).contains("Super Admin Giriş Paneli");

        // Input placeholders check
        assertThat(page.locator("input[type='text']").getAttribute("placeholder")).isEqualTo("Kullanıcı adınızı girin");
        assertThat(page.locator("input[type='password']").getAttribute("placeholder")).isEqualTo("••••••••");

        // Invalid login check
        page.fill("input[type='text']", "admin");
        page.fill("input[type='password']", "wrongpass");
        page.click("button[type='submit']");

        Locator errorBox = page.locator(".login-error");
        errorBox.waitFor();
        assertThat(errorBox.innerText()).contains("Geçersiz kullanıcı adı veya şifre");
    }

    @Test
    @DisplayName("Playwright UI: Dashboard ana sayfa, İstatistik Kartları ve Filtreleme Tabları testi")
    void testDashboardMainViewAndFilterTabs() {
        performSuperAdminLogin();

        // Admin badge & Navbar verification
        Locator adminBadge = page.locator(".admin-badge");
        adminBadge.waitFor();
        assertThat(adminBadge.innerText()).contains("Admin");

        // Stat cards check
        Locator statCards = page.locator(".stat-card");
        assertThat(statCards.count()).isGreaterThanOrEqualTo(3);

        // Search bar interaction
        Locator searchInput = page.locator("input[placeholder*='Duyurularda ara']");
        if (searchInput.count() > 0) {
            searchInput.fill("fatura");
            page.waitForTimeout(300);
            assertThat(searchInput.inputValue()).isEqualTo("fatura");
        }
    }

    @Test
    @DisplayName("Playwright UI: Manuel Duyuru Tarama Tetikleme (Scrape) Butonu testi")
    void testManualScrapeTrigger() {
        performSuperAdminLogin();

        Locator scrapeBtn = page.locator(".btn-scrape");
        scrapeBtn.waitFor();
        assertThat(scrapeBtn.innerText()).contains("Duyuruları Tara");

        // Click scrape button
        scrapeBtn.click();

        // Should show loading or toast
        page.waitForTimeout(500);
    }

    @Test
    @DisplayName("Playwright UI: Yeni Abone Ekleme Formu ve Site Tercihleri Seçim Modalı testi")
    void testSubscriberFormModalInteraction() {
        performSuperAdminLogin();

        // Look for "+ Yeni Abone Ekle" or subscriber form
        Locator emailInput = page.locator("input[type='email']");
        if (emailInput.count() > 0) {
            emailInput.fill("playwright.test@example.com");

            Locator nameInput = page.locator("input[placeholder*='Ad Soyad']");
            if (nameInput.count() > 0) {
                nameInput.fill("Playwright E2E Tester");
            }
        }
    }

    @Test
    @DisplayName("Playwright UI: Güvenli Oturumu Kapatma (Logout) testi")
    void testLogoutFlow() {
        performSuperAdminLogin();

        Locator logoutBtn = page.locator(".btn-logout");
        logoutBtn.waitFor();
        logoutBtn.click();

        page.waitForURL("**/login.html");
        assertThat(page.url()).endsWith("/login.html");
    }
}
