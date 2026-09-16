package com.yasarbilgi.announcementtracker.service;

import com.yasarbilgi.announcementtracker.dto.request.DepartmentRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.DepartmentResponseDto;
import com.yasarbilgi.announcementtracker.entity.Department;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.exception.ResourceNotFoundException;
import com.yasarbilgi.announcementtracker.exception.ScrapingException;
import com.yasarbilgi.announcementtracker.repository.DepartmentRepository;
import com.yasarbilgi.announcementtracker.repository.SubscriberRepository;
import com.yasarbilgi.announcementtracker.service.impl.DepartmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private SubscriberRepository subscriberRepository;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private Department sampleDepartment;

    @BeforeEach
    void setUp() {
        sampleDepartment = Department.builder()
                .id(1L)
                .name("Java")
                .description("Java Yazılım Ekibi")
                .sites(new HashSet<>(Arrays.asList(SiteType.EBELGE_GIB, SiteType.KOSGEB)))
                .subscribers(new HashSet<>())
                .build();
    }

    @Test
    @DisplayName("Departman Başarıyla Oluşturulmalı")
    void createDepartment_Success() {
        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name("Java")
                .description("Java Ekibi")
                .siteTypes(Set.of(SiteType.EBELGE_GIB))
                .build();

        when(departmentRepository.existsByName("Java")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(sampleDepartment);

        DepartmentResponseDto response = departmentService.createDepartment(requestDto);

        assertNotNull(response);
        assertEquals("Java", response.getName());
        verify(departmentRepository, times(1)).save(any(Department.class));
    }

    @Test
    @DisplayName("Aynı İsimde Departman Eklenmek İstendiğinde Hata Fırlatılmalı (Unique Enforce)")
    void createDepartment_DuplicateName_ThrowsException() {
        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name("Java")
                .description("Java Ekibi")
                .build();

        when(departmentRepository.existsByName("Java")).thenReturn(true);

        ScrapingException exception = assertThrows(ScrapingException.class, () ->
                departmentService.createDepartment(requestDto)
        );

        assertTrue(exception.getMessage().contains("departman zaten mevcut"));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Departman Güncelleme Başarılı Olmalı")
    void updateDepartment_Success() {
        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name("Java & Spring")
                .description("Güncellendi")
                .siteTypes(Set.of(SiteType.EBELGE_GIB, SiteType.KOSGEB))
                .build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(sampleDepartment));
        when(departmentRepository.existsByNameAndIdNot("Java & Spring", 1L)).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(sampleDepartment);

        DepartmentResponseDto response = departmentService.updateDepartment(1L, requestDto);

        assertNotNull(response);
        verify(departmentRepository, times(1)).save(sampleDepartment);
    }

    @Test
    @DisplayName("Departman Bulunamadığında Exception Fırlatılmalı")
    void getDepartmentById_NotFound_ThrowsException() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> departmentService.getDepartmentById(99L));
    }

    @Test
    @DisplayName("Departman Silinirken İlişkiler Kaldırılmalı ve Silinmeli")
    void deleteDepartment_Success() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(sampleDepartment));

        departmentService.deleteDepartment(1L);

        verify(departmentRepository, times(1)).delete(sampleDepartment);
    }
}
