package com.jobconnect.service;

import com.jobconnect.dto.interview.InterviewCreateDto;
import com.jobconnect.dto.interview.InterviewResponseDto;
import com.jobconnect.dto.interview.InterviewUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InterviewService {

    InterviewResponseDto scheduleInterview(InterviewCreateDto dto);
    Page<InterviewResponseDto> getCandidateInterviews(Pageable pageable);
    Page<InterviewResponseDto> getEmployerInterviews(Pageable pageable);
    List<InterviewResponseDto> getUpcomingInterviews();
    void updateInterviewStatus(Long interviewId, String status);
    InterviewResponseDto getInterviewById(Long interviewId);
    InterviewResponseDto updateInterview(Long interviewId, InterviewUpdateDto dto);
    void deleteInterview(Long interviewId);
}
