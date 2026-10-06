package com.jobconnect.dto.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ApplicationResponseDto {

    private Long id;
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private String jobLocation;
    private Long candidateId;
    private Long candidateUserId;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String candidateHeadline;
    private String candidateLocation;
    private Long resumeId;
    private String resumeFilename;
    private String coverLetter;
    private String status;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;

    private List<StatusHistoryDto> statusHistory = new ArrayList<>();

    public static class StatusHistoryDto {
        private String previousStatus;
        private String newStatus;
        private String changedByName;
        private String notes;
        private LocalDateTime changedAt;

        public StatusHistoryDto() {}

        public StatusHistoryDto(String previousStatus, String newStatus, String changedByName, String notes, LocalDateTime changedAt) {
            this.previousStatus = previousStatus;
            this.newStatus = newStatus;
            this.changedByName = changedByName;
            this.notes = notes;
            this.changedAt = changedAt;
        }

        public String getPreviousStatus() { return previousStatus; }
        public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }

        public String getNewStatus() { return newStatus; }
        public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

        public String getChangedByName() { return changedByName; }
        public void setChangedByName(String changedByName) { this.changedByName = changedByName; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public LocalDateTime getChangedAt() { return changedAt; }
        public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
    }

    public ApplicationResponseDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getJobLocation() { return jobLocation; }
    public void setJobLocation(String jobLocation) { this.jobLocation = jobLocation; }

    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }

    public Long getCandidateUserId() { return candidateUserId; }
    public void setCandidateUserId(Long candidateUserId) { this.candidateUserId = candidateUserId; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }

    public String getCandidatePhone() { return candidatePhone; }
    public void setCandidatePhone(String candidatePhone) { this.candidatePhone = candidatePhone; }

    public String getCandidateHeadline() { return candidateHeadline; }
    public void setCandidateHeadline(String candidateHeadline) { this.candidateHeadline = candidateHeadline; }

    public String getCandidateLocation() { return candidateLocation; }
    public void setCandidateLocation(String candidateLocation) { this.candidateLocation = candidateLocation; }

    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }

    public String getResumeFilename() { return resumeFilename; }
    public void setResumeFilename(String resumeFilename) { this.resumeFilename = resumeFilename; }

    public String getCoverLetter() { return coverLetter; }
    public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<StatusHistoryDto> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<StatusHistoryDto> statusHistory) { this.statusHistory = statusHistory; }
}
