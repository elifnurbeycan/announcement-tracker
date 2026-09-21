package com.yasarbilgi.announcementtracker.controller;

import com.yasarbilgi.announcementtracker.dto.request.DepartmentRequestDto;
import com.yasarbilgi.announcementtracker.dto.response.ApiResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.DepartmentResponseDto;
import com.yasarbilgi.announcementtracker.dto.response.SubscriberResponseDto;
import com.yasarbilgi.announcementtracker.enums.SiteType;
import com.yasarbilgi.announcementtracker.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(value = "/api/v1/departments", produces = "application/json;charset=UTF-8")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<DepartmentResponseDto>>> getAllDepartments() {
        List<DepartmentResponseDto> list = departmentService.getAllDepartments();
        return ResponseEntity.ok(ApiResponseDto.ok("Departmanlar başarıyla getirildi.", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<DepartmentResponseDto>> getDepartmentById(@PathVariable Long id) {
        DepartmentResponseDto response = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(ApiResponseDto.ok("Departman detayı getirildi.", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDto<DepartmentResponseDto>> createDepartment(@Valid @RequestBody DepartmentRequestDto dto) {
        DepartmentResponseDto response = departmentService.createDepartment(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok("Departman başarıyla oluşturuldu.", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<DepartmentResponseDto>> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequestDto dto) {
        DepartmentResponseDto response = departmentService.updateDepartment(id, dto);
        return ResponseEntity.ok(ApiResponseDto.ok("Departman güncellendi.", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(ApiResponseDto.ok("Departman silindi."));
    }

    @PostMapping("/batch-delete")
    public ResponseEntity<ApiResponseDto<Void>> deleteDepartmentsBatch(@RequestBody List<Long> ids) {
        departmentService.deleteDepartmentsBatch(ids);
        return ResponseEntity.ok(ApiResponseDto.ok("Seçilen departmanlar başarıyla silindi."));
    }

    @PatchMapping("/{id}/sites")
    public ResponseEntity<ApiResponseDto<DepartmentResponseDto>> updateDepartmentSites(
            @PathVariable Long id,
            @RequestBody Set<SiteType> sites) {
        DepartmentResponseDto response = departmentService.updateDepartmentSites(id, sites);
        return ResponseEntity.ok(ApiResponseDto.ok("Departman siteleri güncellendi.", response));
    }

    @GetMapping("/{id}/subscribers")
    public ResponseEntity<ApiResponseDto<List<SubscriberResponseDto>>> getDepartmentSubscribers(@PathVariable Long id) {
        List<SubscriberResponseDto> list = departmentService.getDepartmentSubscribers(id);
        return ResponseEntity.ok(ApiResponseDto.ok("Departmana bağlı aboneler getirildi.", list));
    }
}
