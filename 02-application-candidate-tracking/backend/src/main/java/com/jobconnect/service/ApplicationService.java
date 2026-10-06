package com.jobconnect.service;

import com.jobconnect.dto.application.ApplicationCreateDto;
import com.jobconnect.dto.application.ApplicationResponseDto;
import com.jobconnect.dto.application.ApplicationStatusUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplicationService {

    ApplicationResponseDto applyToJob(ApplicationCreateDto dto);
    ApplicationResponseDto updateApplicationStatus(Long applicationId, ApplicationStatusUpdateDto dto);
    ApplicationResponseDto getApplicationDetails(Long applicationId);

    Page<ApplicationResponseDto> getMyApplications(String status, Pageable pageable);
    Page<ApplicationResponseDto> getJobApplications(Long jobId, String status, Pageable pageable);
    Page<ApplicationResponseDto> getEmployerApplications(String status, Pageable pageable);
    void withdrawApplication(Long applicationId);
}
