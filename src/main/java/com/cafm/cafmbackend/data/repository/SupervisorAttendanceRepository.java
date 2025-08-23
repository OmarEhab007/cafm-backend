package com.cafm.cafmbackend.data.repository;

import com.cafm.cafmbackend.data.entity.SupervisorAttendance;
import com.cafm.cafmbackend.data.entity.SupervisorAttendance.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SupervisorAttendance entities.
 */
@Repository
public interface SupervisorAttendanceRepository extends JpaRepository<SupervisorAttendance, UUID>, 
                                                       JpaSpecificationExecutor<SupervisorAttendance> {

    /**
     * Find attendance for a specific supervisor and date.
     */
    Optional<SupervisorAttendance> findBySupervisorIdAndAttendanceDateAndDeletedAtIsNull(
            UUID supervisorId, LocalDate attendanceDate);

    /**
     * Find attendance for a specific supervisor, school and date.
     */
    Optional<SupervisorAttendance> findBySupervisorIdAndSchoolIdAndAttendanceDateAndDeletedAtIsNull(
            UUID supervisorId, UUID schoolId, LocalDate attendanceDate);

    /**
     * Find all attendance records for a supervisor.
     */
    Page<SupervisorAttendance> findBySupervisorIdAndDeletedAtIsNull(UUID supervisorId, Pageable pageable);

    /**
     * Find all attendance records for a school.
     */
    Page<SupervisorAttendance> findBySchoolIdAndDeletedAtIsNull(UUID schoolId, Pageable pageable);

    /**
     * Find attendance records for a date range.
     */
    @Query("SELECT sa FROM SupervisorAttendance sa WHERE sa.supervisor.id = :supervisorId " +
           "AND sa.attendanceDate BETWEEN :startDate AND :endDate " +
           "AND sa.deletedAt IS NULL " +
           "ORDER BY sa.attendanceDate DESC")
    List<SupervisorAttendance> findBySupervisorAndDateRange(@Param("supervisorId") UUID supervisorId,
                                                            @Param("startDate") LocalDate startDate,
                                                            @Param("endDate") LocalDate endDate);

    /**
     * Find attendance by status.
     */
    Page<SupervisorAttendance> findByStatusAndDeletedAtIsNull(AttendanceStatus status, Pageable pageable);

    /**
     * Find incomplete attendance (checked in but not checked out).
     */
    @Query("SELECT sa FROM SupervisorAttendance sa WHERE sa.checkInTime IS NOT NULL " +
           "AND sa.checkOutTime IS NULL " +
           "AND sa.attendanceDate = :date " +
           "AND sa.deletedAt IS NULL")
    List<SupervisorAttendance> findIncompleteAttendance(@Param("date") LocalDate date);

    /**
     * Find attendance for a company.
     */
    @Query("SELECT sa FROM SupervisorAttendance sa WHERE sa.company.id = :companyId " +
           "AND sa.attendanceDate BETWEEN :startDate AND :endDate " +
           "AND sa.deletedAt IS NULL")
    List<SupervisorAttendance> findByCompanyAndDateRange(@Param("companyId") UUID companyId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    /**
     * Count attendance by status for a company.
     */
    @Query("SELECT sa.status, COUNT(sa) FROM SupervisorAttendance sa " +
           "WHERE sa.company.id = :companyId " +
           "AND sa.attendanceDate = :date " +
           "AND sa.deletedAt IS NULL " +
           "GROUP BY sa.status")
    List<Object[]> countByStatusForCompanyAndDate(@Param("companyId") UUID companyId,
                                                  @Param("date") LocalDate date);

    /**
     * Find supervisors who visited multiple schools on a date.
     */
    @Query(value = "SELECT * FROM supervisor_attendance " +
                   "WHERE attendance_date = :date " +
                   "AND jsonb_array_length(schools_visited) > 1 " +
                   "AND deleted_at IS NULL", 
           nativeQuery = true)
    List<SupervisorAttendance> findMultiSchoolVisits(@Param("date") LocalDate date);

    /**
     * Find unverified attendance records.
     */
    @Query("SELECT sa FROM SupervisorAttendance sa WHERE sa.verified = false " +
           "AND sa.attendanceDate < :beforeDate " +
           "AND sa.deletedAt IS NULL")
    List<SupervisorAttendance> findUnverifiedBefore(@Param("beforeDate") LocalDate beforeDate);

    /**
     * Calculate attendance rate for a supervisor.
     */
    @Query("SELECT COUNT(sa) FROM SupervisorAttendance sa " +
           "WHERE sa.supervisor.id = :supervisorId " +
           "AND sa.attendanceDate BETWEEN :startDate AND :endDate " +
           "AND sa.status IN ('CHECKED_OUT', 'VERIFIED') " +
           "AND sa.deletedAt IS NULL")
    Long countCompleteAttendance(@Param("supervisorId") UUID supervisorId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);

    /**
     * Find late check-ins.
     */
    @Query("SELECT sa FROM SupervisorAttendance sa WHERE sa.company.id = :companyId " +
           "AND sa.attendanceDate = :date " +
           "AND sa.status = 'LATE' " +
           "AND sa.deletedAt IS NULL")
    List<SupervisorAttendance> findLateAttendance(@Param("companyId") UUID companyId,
                                                  @Param("date") LocalDate date);

    /**
     * Check if supervisor has attendance for date.
     */
    @Query("SELECT COUNT(sa) > 0 FROM SupervisorAttendance sa " +
           "WHERE sa.supervisor.id = :supervisorId " +
           "AND sa.attendanceDate = :date " +
           "AND sa.deletedAt IS NULL")
    boolean existsBySupervisorAndDate(@Param("supervisorId") UUID supervisorId,
                                      @Param("date") LocalDate date);
}