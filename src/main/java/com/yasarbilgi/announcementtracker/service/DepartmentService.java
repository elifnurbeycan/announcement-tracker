package com.yasarbilgi.announcementtracker.service;

import com.yasarbilgi.announcementtracker.dto.request.DepartmentRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.DepartmentResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.SubscriberResponseDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;

import java.util.List;
import java.util.Set;

public interface DepartmentService {

    List<DepartmentResponseDto> getAllDepartments();

    DepartmentResponseDto getDepartmentById(Long id);

    DepartmentResponseDto createDepartment(DepartmentRequestDto dto);

    DepartmentResponseDto updateDepartment(Long id, DepartmentRequestDto dto);

    void deleteDepartment(Long id);

    DepartmentResponseDto updateDepartmentSites(Long id, Set<SiteType> siteTypes);

    List<SubscriberResponseDto> getDepartmentSubscribers(Long id);
}
