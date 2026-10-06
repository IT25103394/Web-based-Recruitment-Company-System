package com.jobconnect.dto.interview;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class InterviewUpdateDto {

    @Future(message = "Interview date must be in the future")
    private LocalDateTime scheduledAt;

    private String locationType;

    @NotBlank(message = "Meeting link or location address is required")
    private String locationOrLink;

    private String instructions;

    public InterviewUpdateDto() {}

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public String getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType; }

    public String getLocationOrLink() { return locationOrLink; }
    public void setLocationOrLink(String locationOrLink) { this.locationOrLink = locationOrLink; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
}
