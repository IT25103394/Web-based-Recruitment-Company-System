package com.jobconnect.dto.application;

import jakarta.validation.constraints.NotBlank;

public class ApplicationStatusUpdateDto {

    @NotBlank(message = "Status is required")
    private String status;

    private String notes;

    public ApplicationStatusUpdateDto() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
