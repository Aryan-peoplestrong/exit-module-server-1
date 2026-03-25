package com.PeopleStrong.ExitModule.repository;

import com.PeopleStrong.ExitModule.model.ItChecklist;
import com.PeopleStrong.ExitModule.model.enums.ChecklistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItChecklistRepository extends JpaRepository<ItChecklist, Long> {
    List<ItChecklist> findByStatus(ChecklistStatus status);
    List<ItChecklist> findByExitRequest_RequestId(Long requestId);
}
