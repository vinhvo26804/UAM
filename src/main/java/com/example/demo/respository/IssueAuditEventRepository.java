package com.example.demo.respository;

import com.example.demo.entity.IssueAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueAuditEventRepository extends JpaRepository<IssueAuditEvent, Long> {

    @Query("SELECT a FROM IssueAuditEvent a WHERE a.issue.id = :issueId ORDER BY a.createdAt DESC, a.id DESC")
    Page<IssueAuditEvent> findAuditEventsByIssueId(@Param("issueId") Long issueId, Pageable pageable);
}
