package com.example.demo.respository;

import com.example.demo.entity.IssueSprintHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IssueSprintHistoryRepository extends JpaRepository<IssueSprintHistory, Long> {

    @Query("SELECT h FROM IssueSprintHistory h WHERE h.issue.id = :issueId AND h.leftAt IS NULL")
    Optional<IssueSprintHistory> findOpenHistoryByIssueId(@Param("issueId") Long issueId);

    @Query("SELECT h FROM IssueSprintHistory h WHERE h.issue.id = :issueId ORDER BY h.enteredAt DESC, h.id DESC")
    Page<IssueSprintHistory> findHistoriesByIssueId(@Param("issueId") Long issueId, Pageable pageable);
}
