package com.PassBanned.API.repository;


import com.PassBanned.API.Model.BannedPassenger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BannedPassengerRepository extends JpaRepository<BannedPassenger, Long> {
    Optional<BannedPassenger> findByPassportNumber(String passportNumber);
    
    boolean existsByPassportNumber(String passportNumber);
    
    List<BannedPassenger> findByStatus(BannedPassenger.Status status);
    
    List<BannedPassenger> findBySeverity(BannedPassenger.Severity severity);
    
    @Query("SELECT bp FROM BannedPassenger bp WHERE bp.banDate >= :startDate")
    List<BannedPassenger> findRecentBans(@Param("startDate") LocalDate startDate);
    
    @Query("SELECT COUNT(bp) FROM BannedPassenger bp WHERE bp.status = 'ACTIVE'")
    long countActiveBans();
    
    @Query("SELECT COUNT(bp) FROM BannedPassenger bp WHERE bp.severity = 'CRITICAL'")
    long countCriticalCases();
    
    @Query("SELECT bp FROM BannedPassenger bp WHERE " +
           "LOWER(bp.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(bp.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(bp.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(bp.passportNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(bp.nationality) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<BannedPassenger> searchPassengers(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT bp FROM BannedPassenger bp WHERE " +
           "(:severity IS NULL OR bp.severity = :severity) AND " +
           "(:status IS NULL OR bp.status = :status) AND " +
           "(:banDuration IS NULL OR bp.banDuration = :banDuration) AND " +
           "(:nationality IS NULL OR LOWER(bp.nationality) LIKE LOWER(CONCAT('%', :nationality, '%')))")
    Page<BannedPassenger> findWithFilters(
        @Param("severity") BannedPassenger.Severity severity,
        @Param("status") BannedPassenger.Status status,
        @Param("banDuration") BannedPassenger.BanDuration banDuration,
        @Param("nationality") String nationality,
        Pageable pageable
    );
}