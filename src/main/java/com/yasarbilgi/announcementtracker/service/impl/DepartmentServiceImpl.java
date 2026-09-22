package com.yasarbilgi.announcementtracker.service.impl;

import com.yasarbilgi.announcementtracker.dto.request.DepartmentRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.DepartmentResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.DepartmentSummaryDto;
import com.yasarbilgi.announcementtracker.dto.response.SubscriberResponseDto;
import com.yasarbilgi.announcementtracker.entity.Department;
import com.yasarbilgi.announcementtracker.entity.Subscriber;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.exception.ResourceNotFoundException;
import com.yasarbilgi.announcementtracker.exception.ScrapingException;
import com.yasarbilgi.announcementtracker.repository.DepartmentRepository;
import com.yasarbilgi.announcementtracker.repository.SubscriberRepository;
import com.yasarbilgi.announcementtracker.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final SubscriberRepository subscriberRepository;

    @Override
    public List<DepartmentResponseDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public DepartmentResponseDto getDepartmentById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departman bulunamadı, ID: " + id));
        return mapToResponseDto(dept);
    }

    @Override
    @Transactional
    public DepartmentResponseDto createDepartment(DepartmentRequestDto dto) {
        String cleanName = dto.getName() != null ? dto.getName().trim() : "";
        if (departmentRepository.existsByName(cleanName)) {
            throw new ScrapingException("'" + cleanName + "' adında bir departman zaten mevcut. Lütfen farklı bir isim giriniz.");
        }

        Set<SiteType> sites = dto.getSiteTypes() != null ? new HashSet<>(dto.getSiteTypes()) : new HashSet<>();

        Department department = Department.builder()
                .name(cleanName)
                .description(dto.getDescription() != null ? dto.getDescription().trim() : null)
                .sites(sites)
                .build();

        Department saved = departmentRepository.save(department);
        log.info("Yeni departman oluşturuldu: {} (ID: {})", saved.getName(), saved.getId());
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional
    public DepartmentResponseDto updateDepartment(Long id, DepartmentRequestDto dto) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departman bulunamadı, ID: " + id));

        String cleanName = dto.getName() != null ? dto.getName().trim() : "";
        if (departmentRepository.existsByNameAndIdNot(cleanName, id)) {
            throw new ScrapingException("'" + cleanName + "' adında başka bir departman zaten mevcut.");
        }

        dept.setName(cleanName);
        dept.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        if (dto.getSiteTypes() != null) {
            dept.setSites(new HashSet<>(dto.getSiteTypes()));
        }

        Department updated = departmentRepository.save(dept);
        log.info("Departman güncellendi: {} (ID: {})", updated.getName(), updated.getId());
        return mapToResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departman bulunamadı, ID: " + id));

        // Remove department from subscriber associations before deletion
        for (Subscriber sub : dept.getSubscribers()) {
            sub.getDepartments().remove(dept);
            subscriberRepository.save(sub);
        }

        departmentRepository.delete(dept);
        log.info("Departman silindi: {} (ID: {})", dept.getName(), id);
    }

    @Override
    @Transactional
    public void deleteDepartmentsBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            try {
                deleteDepartment(id);
            } catch (Exception e) {
                log.warn("Toplu silme sırasında departman ID: {} silinemedi: {}", id, e.getMessage());
            }
        }
        log.info("Toplu departman silme tamamlandı. Toplam talep edilen: {}", ids.size());
    }

    @Override
    @Transactional
    public DepartmentResponseDto updateDepartmentSites(Long id, Set<SiteType> siteTypes) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departman bulunamadı, ID: " + id));

        dept.setSites(siteTypes != null ? new HashSet<>(siteTypes) : new HashSet<>());
        Department updated = departmentRepository.save(dept);
        log.info("Departman site tercihleri güncellendi: {} -> {}", dept.getName(), dept.getSites());
        return mapToResponseDto(updated);
    }

    @Override
    public List<SubscriberResponseDto> getDepartmentSubscribers(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departman bulunamadı, ID: " + id));

        return dept.getSubscribers().stream()
                .map(this::mapSubscriberToDto)
                .toList();
    }

    private DepartmentResponseDto mapToResponseDto(Department dept) {
        int subCount = dept.getSubscribers() != null ? dept.getSubscribers().size() : 0;
        return DepartmentResponseDto.builder()
                .id(dept.getId())
                .name(dept.getName())
                .description(dept.getDescription())
                .sites(dept.getSites() != null ? new HashSet<>(dept.getSites()) : new HashSet<>())
                .subscriberCount(subCount)
                .createdAt(dept.getCreatedAt())
                .build();
    }

    private SubscriberResponseDto mapSubscriberToDto(Subscriber entity) {
        Set<SiteType> allSites = new HashSet<>(Arrays.asList(SiteType.values()));
        List<DepartmentSummaryDto> deptSummaries = entity.getDepartments() != null
                ? entity.getDepartments().stream()
                .map(d -> DepartmentSummaryDto.builder()
                        .id(d.getId())
                        .name(d.getName())
                        .sites(d.getSites() != null ? new HashSet<>(d.getSites()) : new HashSet<>())
                        .build())
                .toList()
                : List.of();

        return SubscriberResponseDto.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .fullName(entity.getFullName())
                .active(entity.isActive())
                .hasPasswordSet(entity.isActive())
                .subscribedSites(entity.getSubscribedSites())
                .departments(deptSummaries)
                .isGeneralEmployee(entity.isGeneralEmployee())
                .effectiveSites(entity.getEffectiveSites(allSites))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
