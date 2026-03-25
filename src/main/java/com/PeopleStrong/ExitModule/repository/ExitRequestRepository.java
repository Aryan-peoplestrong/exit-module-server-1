package com.PeopleStrong.ExitModule.repository;

import com.PeopleStrong.ExitModule.model.ExitRequest;
import com.PeopleStrong.ExitModule.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExitRequestRepository extends JpaRepository<ExitRequest, Long> {
    List<ExitRequest> findByEmployee_EmpId(Long empId);
    
    List<ExitRequest> findByEmployee_L1Manager_EmpId(Long l1ManagerId);

    List<ExitRequest> findByEmployee_HrManager_EmpId(Long hrManagerId);

    @Query("SELECT e FROM ExitRequest e WHERE e.status IN :statuses AND e.lastUpdated < :cutoffDate")
    List<ExitRequest> findStaleRequests(@Param("statuses") List<RequestStatus> statuses, @Param("cutoffDate") LocalDateTime cutoffDate);
}
