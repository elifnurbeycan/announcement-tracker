package com.yasarbilgi.announcementtracker.service;

import com.yasarbilgi.announcementtracker.dto.response.ExcelImportResultDto;
import com.yasarbilgi.announcementtracker.dto.response.SubscriberResponseDto;
import com.yasarbilgi.announcementtracker.entity.Department;
import com.yasarbilgi.announcementtracker.entity.Subscriber;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.exception.ScrapingException;
import com.yasarbilgi.announcementtracker.repository.DepartmentRepository;
import com.yasarbilgi.announcementtracker.repository.SubscriberRepository;
import com.yasarbilgi.announcementtracker.service.impl.SubscriberServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExcelImportAndProfileTest {

    @Mock
    private SubscriberRepository subscriberRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private KeycloakAdminService keycloakAdminService;

    @InjectMocks
    private SubscriberServiceImpl subscriberService;

    private Department javaDept;
    private Department backendDept;

    @BeforeEach
    void setUp() {
        javaDept = Department.builder()
                .id(1L)
                .name("Java")
                .sites(new HashSet<>(Set.of(SiteType.EBELGE_GIB)))
                .subscribers(new HashSet<>())
                .build();

        backendDept = Department.builder()
                .id(2L)
                .name("Backend")
                .sites(new HashSet<>(Set.of(SiteType.KOSGEB)))
                .subscribers(new HashSet<>())
                .build();
    }

    @Test
    @DisplayName("Excel: Tek departmanlı kullanıcı ekleme")
    void excelImport_SingleDepartment() {
        String csvContent = "E-Posta,Ad Soyad,Departman\nabone1@kurum.com,Test Abone 1,Java\n";
        MockMultipartFile file = new MockMultipartFile("file", "subscribers.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(departmentRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(javaDept));
        when(subscriberRepository.findByEmail("abone1@kurum.com")).thenReturn(Optional.empty());

        ExcelImportResultDto result = subscriberService.importSubscribersFromExcelDetailed(file, null);

        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getErrorCount());
        verify(subscriberRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Excel: Birden fazla departmanlı kullanıcı ekleme")
    void excelImport_MultipleDepartments() {
        String csvContent = "E-Posta,Ad Soyad,Departman\nabone2@kurum.com,Test Abone 2,Java, Backend\n";
        MockMultipartFile file = new MockMultipartFile("file", "subscribers.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(departmentRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(javaDept));
        when(departmentRepository.findByNameIgnoreCase("Backend")).thenReturn(Optional.of(backendDept));
        when(subscriberRepository.findByEmail("abone2@kurum.com")).thenReturn(Optional.empty());

        ExcelImportResultDto result = subscriberService.importSubscribersFromExcelDetailed(file, null);

        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    @DisplayName("Excel: Departman alanı boş olan kullanıcı -> Genel Çalışan")
    void excelImport_EmptyDepartment_GeneralEmployee() {
        String csvContent = "E-Posta,Ad Soyad,Departman\nabone3@kurum.com,Test Abone 3,\n";
        MockMultipartFile file = new MockMultipartFile("file", "subscribers.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(subscriberRepository.findByEmail("abone3@kurum.com")).thenReturn(Optional.empty());

        ExcelImportResultDto result = subscriberService.importSubscribersFromExcelDetailed(file, null);

        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    @DisplayName("Excel: Var olmayan departman adı -> Validation Hatası Raporlanır")
    void excelImport_NonExistentDepartment_ValidationError() {
        String csvContent = "E-Posta,Ad Soyad,Departman\nabone4@kurum.com,Test Abone 4,NonExistentDept\n";
        MockMultipartFile file = new MockMultipartFile("file", "subscribers.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(departmentRepository.findByNameIgnoreCase("NonExistentDept")).thenReturn(Optional.empty());

        ExcelImportResultDto result = subscriberService.importSubscribersFromExcelDetailed(file, null);

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getErrorCount());
        assertTrue(result.getErrors().get(0).contains("NonExistentDept"));
    }

    @Test
    @DisplayName("Excel: Aynı departmanın tekrar yazılması -> Duplicate ilişki oluşmaz")
    void excelImport_DuplicateDepartmentInRow() {
        String csvContent = "E-Posta,Ad Soyad,Departman\nabone5@kurum.com,Test Abone 5,Java, Java\n";
        MockMultipartFile file = new MockMultipartFile("file", "subscribers.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(departmentRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(javaDept));
        when(subscriberRepository.findByEmail("abone5@kurum.com")).thenReturn(Optional.empty());

        ExcelImportResultDto result = subscriberService.importSubscribersFromExcelDetailed(file, null);

        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    @DisplayName("Profil: Kullanıcının kendi ad, e-posta, departman ve efektif duyuru kapsamını görmesi")
    void userProfile_ValidatesOwnInformation() {
        Subscriber subscriber = Subscriber.builder()
                .id(50L)
                .email("user@kurum.com")
                .fullName("Test Kullanıcısı")
                .departments(new HashSet<>(Arrays.asList(javaDept, backendDept)))
                .subscribedSites(new HashSet<>(Set.of(SiteType.EBELGE_GIB)))
                .build();

        when(subscriberRepository.findById(50L)).thenReturn(Optional.of(subscriber));
        when(departmentRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(javaDept, backendDept));
        when(subscriberRepository.save(any(Subscriber.class))).thenAnswer(inv -> inv.getArgument(0));

        // Simulate session token validation
        SubscriberResponseDto response = subscriberService.updateSubscriberDepartments(50L, Set.of(1L, 2L));

        assertNotNull(response);
        assertEquals("Test Kullanıcısı", response.getFullName());
        assertEquals("user@kurum.com", response.getEmail());
        assertEquals(2, response.getDepartments().size());
        assertFalse(response.isGeneralEmployee());
        assertTrue(response.getEffectiveSites().contains(SiteType.EBELGE_GIB));
        assertTrue(response.getEffectiveSites().contains(SiteType.KOSGEB));
    }

    @Test
    @DisplayName("Güvenlik: Geçersiz token ile profile erişim engellenir")
    void security_InvalidToken_ThrowsScrapingException() {
        assertThrows(ScrapingException.class, () -> subscriberService.validateUserToken("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("Güvenlik: İmzası doğrulanmamış JWT benzeri token kullanıcı oturumu sayılmaz")
    void security_ForgedJwtLikeToken_ThrowsScrapingException() {
        assertThrows(ScrapingException.class,
                () -> subscriberService.validateUserToken("Bearer forged.header.payload"));
    }
}
