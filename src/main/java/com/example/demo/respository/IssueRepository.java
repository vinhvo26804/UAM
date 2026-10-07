package com.example.demo.respository;

import com.example.demo.entity.Issue;
import com.example.demo.entity.IssueStatus;
import com.example.demo.entity.IssueType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    Optional<Issue> findByIdAndSpaceIdAndDeletedAtIsNull(Long id, Long spaceId);

    Optional<Issue> findByIssueKeyAndSpaceIdAndDeletedAtIsNull(String issueKey, Long spaceId);

    Optional<Issue> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT i FROM Issue i WHERE i.space.id = :spaceId AND i.deletedAt IS NULL " +
           "AND (CAST(:q AS string) IS NULL OR LOWER(i.summary) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR LOWER(i.issueKey) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))) " +
           "AND (:type IS NULL OR i.issueType = :type) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (:assigneeId IS NULL OR (i.assignee IS NOT NULL AND i.assignee.id = :assigneeId)) " +
           "AND (:epicId IS NULL OR (i.epic IS NOT NULL AND i.epic.id = :epicId)) " +
           "AND (:parentIssueId IS NULL OR (i.parentIssue IS NOT NULL AND i.parentIssue.id = :parentIssueId)) " +
           "AND (:sprintId IS NULL OR (i.sprint IS NOT NULL AND i.sprint.id = :sprintId)) " +
           "AND (:backlog IS NULL OR (:backlog = true AND i.sprint IS NULL AND i.issueType <> 'EPIC'))")
    Page<Issue> searchIssues(
            @Param("spaceId") Long spaceId,
            @Param("q") String q,
            @Param("type") IssueType type,
            @Param("status") IssueStatus status,
            @Param("assigneeId") Long assigneeId,
            @Param("epicId") Long epicId,
            @Param("parentIssueId") Long parentIssueId,
            @Param("sprintId") Long sprintId,
            @Param("backlog") Boolean backlog,
            Pageable pageable
    );

    @Query("SELECT i FROM Issue i WHERE i.parentIssue.id = :parentId AND i.deletedAt IS NULL")
    List<Issue> findSubtasksByParentId(@Param("parentId") Long parentId);

    @Query("SELECT i FROM Issue i WHERE i.parentIssue.id = :parentId AND i.deletedAt IS NULL")
    Page<Issue> findSubtasksByParentId(@Param("parentId") Long parentId, Pageable pageable);

    @Query("SELECT i FROM Issue i WHERE i.epic.id = :epicId AND i.deletedAt IS NULL")
    List<Issue> findIssuesByEpicId(@Param("epicId") Long epicId);

    @Query("SELECT i FROM Issue i WHERE i.epic.id = :epicId AND i.deletedAt IS NULL")
    Page<Issue> findIssuesByEpicId(@Param("epicId") Long epicId, Pageable pageable);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.parentIssue.id = :parentId AND i.deletedAt IS NULL")
    long countSubtasksByParentId(@Param("parentId") Long parentId);

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.epic.id = :epicId AND i.deletedAt IS NULL")
    long countIssuesByEpicId(@Param("epicId") Long epicId);

    @Query("SELECT i FROM Issue i WHERE i.sprint.id = :sprintId AND i.deletedAt IS NULL")
    List<Issue> findIssuesBySprintId(@Param("sprintId") Long sprintId);

    @Query("SELECT i FROM Issue i WHERE i.sprint.id = :sprintId AND i.parentIssue IS NULL AND i.deletedAt IS NULL")
    List<Issue> findRootIssuesBySprintId(@Param("sprintId") Long sprintId);
}
