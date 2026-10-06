package com.jobconnect.controller;

import com.jobconnect.dto.application.ApplicationCreateDto;
import com.jobconnect.dto.application.ApplicationResponseDto;
import com.jobconnect.dto.application.ApplicationStatusUpdateDto;
import com.jobconnect.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponseDto> applyToJob(@Valid @RequestBody ApplicationCreateDto dto) {
        return ResponseEntity.ok(applicationService.applyToJob(dto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApplicationResponseDto> updateStatus(
            @PathVariable Long id, @Valid @RequestBody ApplicationStatusUpdateDto dto) {
        return ResponseEntity.ok(applicationService.updateApplicationStatus(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponseDto> getApplicationDetails(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getApplicationDetails(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> withdrawApplication(@PathVariable Long id) {
        applicationService.withdrawApplication(id);
        return ResponseEntity.ok(Map.of("message", "Application successfully withdrawn"));
    }

    @GetMapping({"/my", "/my-applications"})
    public ResponseEntity<?> getMyApplications(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));
        Page<ApplicationResponseDto> pageResult = applicationService.getMyApplications(status, pageable);
        if (request.getRequestURI().endsWith("/my-applications")) {
            return ResponseEntity.ok(pageResult.getContent());
        }
        return ResponseEntity.ok(pageResult);
    }

    @GetMapping("/employer")
    public ResponseEntity<Page<ApplicationResponseDto>> getEmployerApplications(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));
        return ResponseEntity.ok(applicationService.getEmployerApplications(status, pageable));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<Page<ApplicationResponseDto>> getJobApplications(
            @PathVariable Long jobId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));
        return ResponseEntity.ok(applicationService.getJobApplications(jobId, status, pageable));
    }
}
