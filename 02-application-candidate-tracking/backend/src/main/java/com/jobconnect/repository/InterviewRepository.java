package com.jobconnect.repository;

import com.jobconnect.entity.Interview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    Page<Interview> findByCandidateIdOrderByScheduledAtAsc(Long candidateId, Pageable pageable);
    Page<Interview> findByEmployerIdOrderByScheduledAtAsc(Long employerId, Pageable pageable);

    List<Interview> findByCandidateIdAndScheduledAtAfterOrderByScheduledAtAsc(Long candidateId, LocalDateTime now);
    List<Interview> findByEmployerIdAndScheduledAtAfterOrderByScheduledAtAsc(Long employerId, LocalDateTime now);

    long countByCandidateIdAndStatus(Long candidateId, Interview.InterviewStatus status);
    long countByEmployerIdAndStatus(Long employerId, Interview.InterviewStatus status);
}
