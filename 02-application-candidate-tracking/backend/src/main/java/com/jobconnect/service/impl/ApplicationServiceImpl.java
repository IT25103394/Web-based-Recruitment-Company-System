package com.jobconnect.service.impl;

import com.jobconnect.dto.application.ApplicationCreateDto;
import com.jobconnect.dto.application.ApplicationResponseDto;
import com.jobconnect.dto.application.ApplicationStatusUpdateDto;
import com.jobconnect.entity.*;
import com.jobconnect.exception.BadRequestException;
import com.jobconnect.exception.ResourceNotFoundException;
import com.jobconnect.exception.UnauthorizedException;
import com.jobconnect.repository.*;
import com.jobconnect.security.UserPrincipal;
import com.jobconnect.service.ApplicationService;
import com.jobconnect.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final JobPostingRepository jobPostingRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final ResumeRepository resumeRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ApplicationServiceImpl(
            ApplicationRepository applicationRepository,
            ApplicationStatusHistoryRepository historyRepository,
            JobPostingRepository jobPostingRepository,
            CandidateProfileRepository candidateProfileRepository,
            ResumeRepository resumeRepository,
            EmployerProfileRepository employerProfileRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.resumeRepository = resumeRepository;
        this.employerProfileRepository = employerProfileRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    private UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return (UserPrincipal) auth.getPrincipal();
    }

    @Override
    public ApplicationResponseDto applyToJob(ApplicationCreateDto dto) {
        UserPrincipal currentUser = getCurrentUser();
        CandidateProfile candidate = candidateProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new BadRequestException("Only registered candidates can apply for jobs"));

        JobPosting job = jobPostingRepository.findById(dto.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job Posting", "id", dto.getJobId()));

        // Enforce rule: Expired jobs or non-approved jobs reject applications
        if (job.getStatus() != JobPosting.JobStatus.APPROVED) {
            throw new BadRequestException("This job posting is not currently accepting applications (Status: " + job.getStatus() + ")");
        }
        if (job.getDeadline().isBefore(LocalDateTime.now())) {
            job.setStatus(JobPosting.JobStatus.EXPIRED);
            jobPostingRepository.save(job);
            throw new BadRequestException("The application deadline for this position has passed. Applications are closed.");
        }

        // Prevent duplicate application
        if (applicationRepository.existsByJobIdAndCandidateId(job.getId(), candidate.getId())) {
            throw new BadRequestException("You have already submitted an application for this vacancy.");
        }

        Resume resume = null;
        if (dto.getResumeId() != null) {
            resume = resumeRepository.findById(dto.getResumeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", dto.getResumeId()));
        } else if (!candidate.getResumes().isEmpty()) {
            resume = candidate.getResumes().get(0);
        }

        Application application = new Application();
        application.setJob(job);
        application.setCandidate(candidate);
        application.setResume(resume);
        application.setCoverLetter(dto.getCoverLetter());
        application.setStatus(Application.ApplicationStatus.APPLIED);

        Application saved = applicationRepository.save(application);

        // Record history
        User actor = userRepository.findById(currentUser.getId()).orElse(candidate.getUser());
        ApplicationStatusHistory history = new ApplicationStatusHistory(
                saved, null, Application.ApplicationStatus.APPLIED.name(), actor, "Application submitted by candidate."
        );
        historyRepository.save(history);
        saved.getStatusHistory().add(history);

        // Notify employer
        notificationService.createNotification(
                job.getEmployer().getUser().getId(),
                "NEW_APPLICATION",
                "New Application Received",
                candidate.getUser().getFullName() + " applied for '" + job.getTitle() + "'.",
                saved.getId()
        );

        return mapToDto(saved);
    }

    @Override
    public ApplicationResponseDto updateApplicationStatus(Long applicationId, ApplicationStatusUpdateDto dto) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        UserPrincipal user = getCurrentUser();
        boolean isAdmin = user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isEmployer = application.getJob().getEmployer().getUser().getId().equals(user.getId());

        if (!isAdmin && !isEmployer) {
            throw new UnauthorizedException("Unauthorized to update this application's status");
        }

        Application.ApplicationStatus newStatus;
        try {
            newStatus = Application.ApplicationStatus.valueOf(dto.getStatus().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid application status: " + dto.getStatus());
        }

        String prevStatus = application.getStatus().name();
        application.setStatus(newStatus);
        Application saved = applicationRepository.save(application);

        User actor = userRepository.findById(user.getId()).orElse(application.getJob().getEmployer().getUser());
        ApplicationStatusHistory history = new ApplicationStatusHistory(
                saved, prevStatus, newStatus.name(), actor,
                StringUtils.hasText(dto.getNotes()) ? dto.getNotes() : "Status updated to " + newStatus.name()
        );
        historyRepository.save(history);
        saved.getStatusHistory().add(history);

        // Notify candidate
        notificationService.createNotification(
                application.getCandidate().getUser().getId(),
                "APPLICATION_STATUS",
                "Application Status Update",
                "Your application for '" + application.getJob().getTitle() + "' at " +
                        application.getJob().getEmployer().getCompanyName() + " was updated to: " + newStatus.name(),
                saved.getId()
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponseDto getApplicationDetails(Long applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        UserPrincipal user = getCurrentUser();
        boolean isAdmin = user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isCandidate = app.getCandidate().getUser().getId().equals(user.getId());
        boolean isEmployer = app.getJob().getEmployer().getUser().getId().equals(user.getId());

        if (!isAdmin && !isCandidate && !isEmployer) {
            throw new UnauthorizedException("Unauthorized to view this application");
        }

        return mapToDto(app);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponseDto> getMyApplications(String status, Pageable pageable) {
        UserPrincipal user = getCurrentUser();
        CandidateProfile candidate = candidateProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Candidate profile not found"));

        if (StringUtils.hasText(status)) {
            try {
                Application.ApplicationStatus appStatus = Application.ApplicationStatus.valueOf(status.toUpperCase());
                return applicationRepository.findByCandidateIdAndStatus(candidate.getId(), appStatus, pageable).map(this::mapToDto);
            } catch (Exception ignored) {}
        }
        return applicationRepository.findByCandidateId(candidate.getId(), pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponseDto> getJobApplications(Long jobId, String status, Pageable pageable) {
        JobPosting job = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Posting", "id", jobId));

        UserPrincipal user = getCurrentUser();
        boolean isAdmin = user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !job.getEmployer().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Unauthorized to view applications for this job");
        }

        if (StringUtils.hasText(status)) {
            try {
                Application.ApplicationStatus appStatus = Application.ApplicationStatus.valueOf(status.toUpperCase());
                return applicationRepository.findByJobIdAndStatus(jobId, appStatus, pageable).map(this::mapToDto);
            } catch (Exception ignored) {}
        }
        return applicationRepository.findByJobId(jobId, pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponseDto> getEmployerApplications(String status, Pageable pageable) {
        UserPrincipal user = getCurrentUser();
        EmployerProfile employer = employerProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Employer profile not found"));

        if (StringUtils.hasText(status)) {
            try {
                Application.ApplicationStatus appStatus = Application.ApplicationStatus.valueOf(status.toUpperCase());
                return applicationRepository.findByEmployerIdAndStatus(employer.getId(), appStatus, pageable).map(this::mapToDto);
            } catch (Exception ignored) {}
        }
        return applicationRepository.findByEmployerId(employer.getId(), pageable).map(this::mapToDto);
    }

    private ApplicationResponseDto mapToDto(Application app) {
        ApplicationResponseDto dto = new ApplicationResponseDto();
        dto.setId(app.getId());
        dto.setJobId(app.getJob().getId());
        dto.setJobTitle(app.getJob().getTitle());
        dto.setCompanyName(app.getJob().getEmployer().getCompanyName());
        dto.setJobLocation(app.getJob().getLocation());

        dto.setCandidateId(app.getCandidate().getId());
        dto.setCandidateUserId(app.getCandidate().getUser().getId());
        dto.setCandidateName(app.getCandidate().getUser().getFullName());
        dto.setCandidateEmail(app.getCandidate().getUser().getEmail());
        dto.setCandidatePhone(app.getCandidate().getUser().getPhone());
        dto.setCandidateHeadline(app.getCandidate().getHeadline());
        dto.setCandidateLocation(app.getCandidate().getLocation());

        if (app.getResume() != null) {
            dto.setResumeId(app.getResume().getId());
            dto.setResumeFilename(app.getResume().getOriginalFilename());
        }

        dto.setCoverLetter(app.getCoverLetter());
        dto.setStatus(app.getStatus().name());
        dto.setAppliedAt(app.getAppliedAt());
        dto.setUpdatedAt(app.getUpdatedAt());

        dto.setStatusHistory(app.getStatusHistory().stream().map(h ->
                new ApplicationResponseDto.StatusHistoryDto(
                        h.getPreviousStatus(),
                        h.getNewStatus(),
                        h.getChangedBy() != null ? h.getChangedBy().getFullName() : "System",
                        h.getNotes(),
                        h.getChangedAt()
                )
        ).collect(Collectors.toList()));

        return dto;
    }

    @Override
    public void withdrawApplication(Long applicationId) {
        UserPrincipal currentUser = getCurrentUser();
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));
        if (!app.getCandidate().getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to withdraw this application");
        }
        applicationRepository.delete(app);
    }
}
