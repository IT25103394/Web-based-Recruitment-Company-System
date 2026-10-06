package com.jobconnect.repository;

import com.jobconnect.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByJobIdAndCandidateId(Long jobId, Long candidateId);
    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);

    Page<Application> findByCandidateId(Long candidateId, Pageable pageable);
    Page<Application> findByCandidateIdAndStatus(Long candidateId, Application.ApplicationStatus status, Pageable pageable);

    Page<Application> findByJobId(Long jobId, Pageable pageable);
    Page<Application> findByJobIdAndStatus(Long jobId, Application.ApplicationStatus status, Pageable pageable);

    @Query("SELECT a FROM Application a WHERE a.job.employer.id = :employerId")
    Page<Application> findByEmployerId(@Param("employerId") Long employerId, Pageable pageable);

    @Query("SELECT a FROM Application a WHERE a.job.employer.id = :employerId AND a.status = :status")
    Page<Application> findByEmployerIdAndStatus(@Param("employerId") Long employerId, @Param("status") Application.ApplicationStatus status, Pageable pageable);

    long countByJobEmployerId(Long employerId);
    long countByJobEmployerIdAndStatus(Long employerId, Application.ApplicationStatus status);
    long countByCandidateId(Long candidateId);
    long countByCandidateIdAndStatus(Long candidateId, Application.ApplicationStatus status);
}
