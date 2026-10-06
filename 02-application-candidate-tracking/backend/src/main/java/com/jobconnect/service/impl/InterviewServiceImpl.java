package com.jobconnect.service.impl;

import com.jobconnect.dto.interview.InterviewCreateDto;
import com.jobconnect.dto.interview.InterviewResponseDto;
import com.jobconnect.dto.interview.InterviewUpdateDto;
import com.jobconnect.entity.*;
import com.jobconnect.exception.BadRequestException;
import com.jobconnect.exception.ResourceNotFoundException;
import com.jobconnect.exception.UnauthorizedException;
import com.jobconnect.repository.*;
import com.jobconnect.security.UserPrincipal;
import com.jobconnect.service.InterviewService;
import com.jobconnect.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final NotificationService notificationService;

    public InterviewServiceImpl(
            InterviewRepository interviewRepository,
            ApplicationRepository applicationRepository,
            CandidateProfileRepository candidateProfileRepository,
            EmployerProfileRepository employerProfileRepository,
            NotificationService notificationService) {
        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.employerProfileRepository = employerProfileRepository;
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
    public InterviewResponseDto scheduleInterview(InterviewCreateDto dto) {
        UserPrincipal user = getCurrentUser();
        EmployerProfile employer = employerProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Only employers can schedule interviews"));

        Application application = applicationRepository.findById(dto.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", dto.getApplicationId()));

        if (!application.getJob().getEmployer().getId().equals(employer.getId())) {
            throw new UnauthorizedException("This application is not for your company's job");
        }

        Interview.LocationType locationType = Interview.LocationType.ONLINE;
        if (dto.getLocationType() != null) {
            try {
                locationType = Interview.LocationType.valueOf(dto.getLocationType().toUpperCase());
            } catch (Exception ignored) {}
        }

        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setCandidate(application.getCandidate());
        interview.setEmployer(employer);
        interview.setScheduledAt(dto.getScheduledAt());
        interview.setLocationType(locationType);
        interview.setLocationOrLink(dto.getLocationOrLink());
        interview.setInstructions(dto.getInstructions());
        interview.setStatus(Interview.InterviewStatus.SCHEDULED);

        Interview saved = interviewRepository.save(interview);

        // Update application status to INTERVIEW_SCHEDULED
        application.setStatus(Application.ApplicationStatus.INTERVIEW_SCHEDULED);
        applicationRepository.save(application);

        // Notify candidate
        String dateFormatted = dto.getScheduledAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"));
        notificationService.createNotification(
                application.getCandidate().getUser().getId(),
                "INTERVIEW_SCHEDULED",
                "Interview Scheduled",
                employer.getCompanyName() + " scheduled an interview for '" + application.getJob().getTitle() + "' on " + dateFormatted,
                saved.getId()
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InterviewResponseDto> getCandidateInterviews(Pageable pageable) {
        UserPrincipal user = getCurrentUser();
        CandidateProfile candidate = candidateProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Candidate profile not found"));

        return interviewRepository.findByCandidateIdOrderByScheduledAtAsc(candidate.getId(), pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InterviewResponseDto> getEmployerInterviews(Pageable pageable) {
        UserPrincipal user = getCurrentUser();
        EmployerProfile employer = employerProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Employer profile not found"));

        return interviewRepository.findByEmployerIdOrderByScheduledAtAsc(employer.getId(), pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponseDto> getUpcomingInterviews() {
        UserPrincipal user = getCurrentUser();
        boolean isCandidate = user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_JOB_SEEKER"));

        if (isCandidate) {
            return candidateProfileRepository.findByUserId(user.getId())
                    .map(c -> interviewRepository.findByCandidateIdAndScheduledAtAfterOrderByScheduledAtAsc(c.getId(), LocalDateTime.now()).stream()
                            .map(this::mapToDto).collect(Collectors.toList()))
                    .orElse(List.of());
        } else {
            return employerProfileRepository.findByUserId(user.getId())
                    .map(e -> interviewRepository.findByEmployerIdAndScheduledAtAfterOrderByScheduledAtAsc(e.getId(), LocalDateTime.now()).stream()
                            .map(this::mapToDto).collect(Collectors.toList()))
                    .orElse(List.of());
        }
    }

    @Override
    public void updateInterviewStatus(Long interviewId, String status) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        try {
            interview.setStatus(Interview.InterviewStatus.valueOf(status.toUpperCase()));
            interviewRepository.save(interview);
        } catch (Exception ex) {
            throw new BadRequestException("Invalid interview status: " + status);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewResponseDto getInterviewById(Long interviewId) {
        UserPrincipal user = getCurrentUser();
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        boolean isCandidate = user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_JOB_SEEKER"));
        boolean isEmployer = user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYER"));

        if (isCandidate) {
            CandidateProfile candidate = candidateProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new BadRequestException("Candidate profile not found"));
            if (!interview.getCandidate().getId().equals(candidate.getId())) {
                throw new UnauthorizedException("You can only view your own interviews");
            }
        } else if (isEmployer) {
            EmployerProfile employer = employerProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new BadRequestException("Employer profile not found"));
            if (!interview.getEmployer().getId().equals(employer.getId())) {
                throw new UnauthorizedException("You can only view interviews for your company");
            }
        }

        return mapToDto(interview);
    }

    @Override
    public InterviewResponseDto updateInterview(Long interviewId, InterviewUpdateDto dto) {
        UserPrincipal user = getCurrentUser();
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        EmployerProfile employer = employerProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Only employers can update interviews"));

        if (!interview.getEmployer().getId().equals(employer.getId())) {
            throw new UnauthorizedException("You can only update interviews for your company");
        }

        if (interview.getStatus() != Interview.InterviewStatus.SCHEDULED) {
            throw new BadRequestException("Only scheduled interviews can be updated");
        }

        if (dto.getScheduledAt() != null) {
            interview.setScheduledAt(dto.getScheduledAt());
        }

        if (dto.getLocationType() != null) {
            try {
                interview.setLocationType(Interview.LocationType.valueOf(dto.getLocationType().toUpperCase()));
            } catch (Exception ex) {
                throw new BadRequestException("Invalid location type: " + dto.getLocationType());
            }
        }

        if (dto.getLocationOrLink() != null) {
            interview.setLocationOrLink(dto.getLocationOrLink());
        }

        if (dto.getInstructions() != null) {
            interview.setInstructions(dto.getInstructions());
        }

        Interview saved = interviewRepository.save(interview);

        return mapToDto(saved);
    }

    @Override
    public void deleteInterview(Long interviewId) {
        UserPrincipal user = getCurrentUser();
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        EmployerProfile employer = employerProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Only employers can delete interviews"));

        if (!interview.getEmployer().getId().equals(employer.getId())) {
            throw new UnauthorizedException("You can only delete interviews for your company");
        }

        if (interview.getStatus() != Interview.InterviewStatus.SCHEDULED) {
            throw new BadRequestException("Only scheduled interviews can be deleted");
        }

        interviewRepository.delete(interview);
    }

    private InterviewResponseDto mapToDto(Interview i) {
        InterviewResponseDto dto = new InterviewResponseDto();
        dto.setId(i.getId());
        dto.setApplicationId(i.getApplication().getId());
        dto.setJobId(i.getApplication().getJob().getId());
        dto.setJobTitle(i.getApplication().getJob().getTitle());
        dto.setCompanyName(i.getEmployer().getCompanyName());
        dto.setCandidateId(i.getCandidate().getId());
        dto.setCandidateName(i.getCandidate().getUser().getFullName());
        dto.setCandidateEmail(i.getCandidate().getUser().getEmail());
        dto.setScheduledAt(i.getScheduledAt());
        dto.setLocationType(i.getLocationType().name());
        dto.setLocationOrLink(i.getLocationOrLink());
        dto.setInstructions(i.getInstructions());
        dto.setStatus(i.getStatus().name());
        dto.setCreatedAt(i.getCreatedAt());
        return dto;
    }
}
