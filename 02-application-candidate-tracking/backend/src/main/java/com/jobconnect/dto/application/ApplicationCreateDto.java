package com.jobconnect.dto.application;

import jakarta.validation.constraints.NotNull;

public class ApplicationCreateDto {

    @NotNull(message = "Job ID is required")
    private Long jobId;

    private Long resumeId;
    private String coverLetter;

    public ApplicationCreateDto() {}

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }

    public String getCoverLetter() { return coverLetter; }
    public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }
}
