package com.jobconnect.controller;

import com.jobconnect.dto.interview.InterviewCreateDto;
import com.jobconnect.dto.interview.InterviewResponseDto;
import com.jobconnect.dto.interview.InterviewUpdateDto;
import com.jobconnect.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping
    public ResponseEntity<InterviewResponseDto> scheduleInterview(@Valid @RequestBody InterviewCreateDto dto) {
        return ResponseEntity.ok(interviewService.scheduleInterview(dto));
    }

    @GetMapping({"/candidate", "/my-interviews"})
    public ResponseEntity<?> getCandidateInterviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            jakarta.servlet.http.HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InterviewResponseDto> pageResult = interviewService.getCandidateInterviews(pageable);
        if (request.getRequestURI().endsWith("/my-interviews")) {
            return ResponseEntity.ok(pageResult.getContent());
        }
        return ResponseEntity.ok(pageResult);
    }

    @GetMapping("/employer")
    public ResponseEntity<Page<InterviewResponseDto>> getEmployerInterviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(interviewService.getEmployerInterviews(pageable));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<InterviewResponseDto>> getUpcomingInterviews() {
        return ResponseEntity.ok(interviewService.getUpcomingInterviews());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateInterviewStatus(
            @PathVariable Long id, @RequestParam String status) {
        interviewService.updateInterviewStatus(id, status);
        return ResponseEntity.ok(Map.of("message", "Interview status updated successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterviewResponseDto> getInterviewById(@PathVariable Long id) {
        return ResponseEntity.ok(interviewService.getInterviewById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InterviewResponseDto> updateInterview(
            @PathVariable Long id, @Valid @RequestBody InterviewUpdateDto dto) {
        return ResponseEntity.ok(interviewService.updateInterview(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteInterview(@PathVariable Long id) {
        interviewService.deleteInterview(id);
        return ResponseEntity.ok(Map.of("message", "Interview deleted successfully"));
    }
}
