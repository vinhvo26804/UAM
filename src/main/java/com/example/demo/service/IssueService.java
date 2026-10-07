package com.example.demo.service;

import com.example.demo.dto.request.issue.ChangeIssueStatusRequest;
import com.example.demo.dto.request.issue.CreateIssueRequest;
import com.example.demo.dto.request.issue.UpdateIssueRequest;
import com.example.demo.dto.response.issue.IssueHistoryResponse;
import com.example.demo.dto.response.issue.IssueResponse;
import com.example.demo.dto.response.issue.IssueSprintHistoryResponse;
import com.example.demo.dto.response.issue.IssueSummaryResponse;
import com.example.demo.entity.*;
import com.example.demo.respository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final SpaceRepository spaceRepository;
    private final SpaceMemberRepository spaceMemberRepository;
    private final SprintRepository sprintRepository;
    private final IssueSprintHistoryRepository issueSprintHistoryRepository;
    private final IssueAuditEventRepository issueAuditEventRepository;
    private final UserRepository userRepository;
    private final SpaceAccessService spaceAccessService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Transactional
    public IssueResponse createIssue(CreateIssueRequest request, UUID idempotencyKey, String username) {
        User actor = spaceAccessService.getActiveUser(username);
        SpaceMember member = spaceAccessService.getMembershipOrThrow(request.getSpaceId(), actor.getId());
        spaceAccessService.requireRole(member, SpaceRole.ADMIN, SpaceRole.MANAGER, SpaceRole.MEMBER);

        Space space = spaceAccessService.lockSpaceForWrite(request.getSpaceId());

        // Validate allowed issue types in space
        if (space.getAllowedIssueTypes() != null && !space.getAllowedIssueTypes().isEmpty()) {
            if (!space.getAllowedIssueTypes().contains(request.getType())) {
                throw new IssueOperationException(HttpStatus.BAD_REQUEST, "INVALID_FIELD", "Loại công việc không được hỗ trợ trong Không gian làm việc này");
            }
        }

        // Idempotency check
        String canonicalPayload = String.format("CREATE_ISSUE:%d:%s:%s", request.getSpaceId(), request.getType(), request.getSummary().trim());
        String payloadHash = idempotencyService.computeHash(canonicalPayload);

        if (idempotencyKey != null) {
            Optional<IdempotencyRecord> existing = idempotencyService.findRecord(actor, "ISSUE_CREATE", idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyRecord rec = existing.get();
                idempotencyService.validateNotReusedWithDifferentPayload(rec, payloadHash);
                try {
                    return objectMapper.readValue(rec.getResponseBody(), IssueResponse.class);
                } catch (Exception e) {
                    // Fallback to reload if deserialization fails
                }
            }
        }

        // Hierarchy validations
        Issue epic = null;
        Issue parentIssue = null;
        Sprint sprint = null;

        if (request.getType() == IssueType.EPIC) {
            if (request.getParentIssueId() != null || request.getEpicId() != null || request.getSprintId() != null || request.getEstimateHours() != null) {
                throw new IssueOperationException(HttpStatus.BAD_REQUEST, "INVALID_FIELD", "Epic không thể có công việc cha, Epic khác, Sprint hoặc ước lượng giờ");
            }
        } else if (request.getType() == IssueType.SUBTASK) {
            if (request.getParentIssueId() == null) {
                throw new IssueOperationException(HttpStatus.BAD_REQUEST, "INVALID_FIELD", "Subtask bắt buộc phải có công việc cha");
            }
            if (request.getEpicId() != null) {
                throw new IssueOperationException(HttpStatus.BAD_REQUEST, "INVALID_FIELD", "Subtask không thể liên kết trực tiếp với Epic");
            }
            parentIssue = issueRepository.findById(request.getParentIssueId())
                    .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy công việc cha"));
            if (!parentIssue.getSpace().getId().equals(space.getId())) {
                throw new IssueOperationException(HttpStatus.BAD_REQUEST, "INVALID_RELATION", "Công việc cha phải thuộc cùng Không gian làm việc");
            }
            if (parentIssue.getIssueType() == IssueType.EPIC || parentIssue.getIssueType() == IssueType.SUBTASK) {
                throw new IssueOperationException(HttpStatus.BAD_REQUEST, "INVALID_RELATION", "Công việc cha của Subtask phải là Story, Task hoặc Bug");
            }
            if (parentIssue.getStatus().isDoneCategory()) {
                throw new IssueOperationException(HttpStatus.CONFLICT, "PARENT_DONE", "Công việc cha đã kết thúc (DONE/CANCELLED), không thể thêm Subtask mới");
            }
            // Subtask automatically follows parent's sprint
            sprint = parentIssue.getSprint();
        } else {
            // STORY, TASK, BUG
            if (request.getParentIssueId() != null) {
                throw new IssueOperationException(HttpStatus.BAD_REQUEST, "INVALID_FIELD", "Chỉ Subtask mới có công việc cha");
            }
            if (request.getEpicId() != null) {
                epic = issueRepository.findById(request.getEpicId())
                        .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy Epic"));
                if (!epic.getSpace().getId().equals(space.getId())) {
                    throw new IssueOperationException(HttpStatus.BAD_REQUEST, "INVALID_RELATION", "Epic phải thuộc cùng Không gian làm việc");
                }
                if (epic.getIssueType() != IssueType.EPIC) {
                    throw new IssueOperationException(HttpStatus.BAD_REQUEST, "INVALID_RELATION", "Công việc được chọn làm Epic không phải là loại EPIC");
                }
            }
            if (request.getSprintId() != null) {
                spaceAccessService.requireRole(member, SpaceRole.ADMIN, SpaceRole.MANAGER);
                sprint = sprintRepository.findById(request.getSprintId())
                        .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy Sprint"));
                if (!sprint.getSpace().getId().equals(space.getId())) {
                    throw new IssueOperationException(HttpStatus.BAD_REQUEST, "INVALID_RELATION", "Sprint phải thuộc cùng Không gian làm việc");
                }
                if (sprint.getStatus() == SprintStatus.CLOSED) {
                    throw new IssueOperationException(HttpStatus.CONFLICT, "SPRINT_STATE_CONFLICT", "Không thể gán công việc vào Sprint đã đóng");
                }
            }
        }

        // Reporter
        User reporter = actor;
        if (request.getReporterId() != null && !request.getReporterId().equals(actor.getId())) {
            spaceAccessService.requireRole(member, SpaceRole.ADMIN);
            reporter = userRepository.findById(request.getReporterId())
                    .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy người báo cáo"));
            validateUserEligibleMember(reporter, space.getId());
        }

        // Assignee
        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy người nhận việc"));
            validateUserEligibleAssignee(assignee, space.getId());
        }

        // Labels
        Set<String> cleanLabels = sanitizeLabels(request.getLabels());

        // Allocate issue number & key under space lock
        long issueNumber = space.getNextIssueNumber();
        space.setNextIssueNumber(issueNumber + 1);
        space.setWorkRevision(space.getWorkRevision() + 1);
        spaceRepository.save(space);

        String issueKey = space.getKey() + "-" + issueNumber;

        Issue issue = Issue.builder()
                .space(space)
                .issueNumber(issueNumber)
                .issueKey(issueKey)
                .issueType(request.getType())
                .summary(request.getSummary().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .status(IssueStatus.TODO)
                .priority(request.getPriority() != null ? request.getPriority() : IssuePriority.MEDIUM)
                .reporter(reporter)
                .assignee(assignee)
                .epic(epic)
                .parentIssue(parentIssue)
                .sprint(sprint)
                .estimateHours(request.getEstimateHours())
                .dueDate(request.getDueDate())
                .labels(cleanLabels)
                .createdBy(actor)
                .updatedBy(actor)
                .build();
        issue = issueRepository.save(issue);

        // Sprint history if assigned to sprint initially
        if (sprint != null) {
            IssueSprintHistory hist = IssueSprintHistory.builder()
                    .space(space)
                    .issue(issue)
                    .sprint(sprint)
                    .enteredAt(LocalDateTime.now())
                    .enteredBy(actor)
                    .enterReason("CREATE")
                    .build();
            issueSprintHistoryRepository.save(hist);
        }

        // Audit log
        IssueAuditEvent audit = IssueAuditEvent.builder()
                .issue(issue)
                .actor(actor)
                .action("CREATE")
                .changes("Tạo công việc " + issueKey)
                .build();
        issueAuditEventRepository.save(audit);

        IssueResponse response = toIssueResponse(issue, member.getRole());

        if (idempotencyKey != null) {
            idempotencyService.saveRecord(actor, space, "ISSUE_CREATE", idempotencyKey, payloadHash, 201, response, issue.getId());
        }

        return response;
    }

    @Transactional(readOnly = true)
    public IssueResponse getIssueDetail(Long id, String username) {
        User user = spaceAccessService.getActiveUser(username);
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy công việc"));
        SpaceMember member = spaceAccessService.getMembershipOrThrow(issue.getSpace().getId(), user.getId());
        return toIssueResponse(issue, member.getRole());
    }

    @Transactional(readOnly = true)
    public Page<IssueSummaryResponse> searchIssues(
            Long spaceId,
            String q,
            IssueType type,
            IssueStatus status,
            Long assigneeId,
            Long epicId,
            Long parentIssueId,
            Long sprintId,
            Boolean backlog,
            String username,
            Pageable pageable
    ) {
        User user = spaceAccessService.getActiveUser(username);
        SpaceMember member = spaceAccessService.getMembershipOrThrow(spaceId, user.getId());

        String trimmedQuery = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
        return issueRepository.searchIssues(spaceId, trimmedQuery, type, status, assigneeId, epicId, parentIssueId, sprintId, backlog, pageable)
                .map(i -> toIssueSummaryResponse(i, member.getRole()));
    }

    @Transactional
    public IssueResponse updateIssue(Long id, UpdateIssueRequest request, String username) {
        User actor = spaceAccessService.getActiveUser(username);
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy công việc"));

        Long spaceId = issue.getSpace().getId();
        SpaceMember member = spaceAccessService.getMembershipOrThrow(spaceId, actor.getId());
        spaceAccessService.requireRole(member, SpaceRole.ADMIN, SpaceRole.MANAGER, SpaceRole.MEMBER);

        Space space = spaceAccessService.lockSpaceForWrite(spaceId);

        if (!Objects.equals(issue.getVersion(), request.getVersion())) {
            throw new IssueOperationException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "Công việc đã bị thay đổi bởi người khác. Vui lòng tải lại trang");
        }

        List<String> auditChanges = new ArrayList<>();

        if (request.isSummaryPresent()) {
            if (request.getSummary() == null || request.getSummary().trim().isEmpty()) {
                throw new IssueOperationException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Tiêu đề không được để trống");
            }
            String newSummary = request.getSummary().trim();
            if (!newSummary.equals(issue.getSummary())) {
                auditChanges.add(String.format("Tiêu đề: '%s' -> '%s'", issue.getSummary(), newSummary));
                issue.setSummary(newSummary);
            }
        }

        if (request.isDescriptionPresent()) {
            String newDesc = request.getDescription() != null ? request.getDescription().trim() : null;
            if (!Objects.equals(newDesc, issue.getDescription())) {
                auditChanges.add("Cập nhật mô tả");
                issue.setDescription(newDesc);
            }
        }

        if (request.isPriorityPresent() && request.getPriority() != null) {
            if (issue.getPriority() != request.getPriority()) {
                auditChanges.add(String.format("Độ ưu tiên: %s -> %s", issue.getPriority(), request.getPriority()));
                issue.setPriority(request.getPriority());
            }
        }

        if (request.isReporterIdPresent()) {
            if (request.getReporterId() == null) {
                throw new IssueOperationException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Người báo cáo không được để trống");
            }
            if (!request.getReporterId().equals(issue.getReporter().getId())) {
                spaceAccessService.requireRole(member, SpaceRole.ADMIN);
                User newReporter = userRepository.findById(request.getReporterId())
                        .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy người báo cáo"));
                validateUserEligibleMember(newReporter, spaceId);
                auditChanges.add(String.format("Người báo cáo: %s -> %s", issue.getReporter().getUsername(), newReporter.getUsername()));
                issue.setReporter(newReporter);
            }
        }

        if (request.isAssigneeIdPresent()) {
            Long newAssigneeId = request.getAssigneeId();
            Long currentAssigneeId = issue.getAssignee() != null ? issue.getAssignee().getId() : null;
            if (!Objects.equals(newAssigneeId, currentAssigneeId)) {
                if (newAssigneeId == null) {
                    auditChanges.add("Gỡ người nhận việc");
                    issue.setAssignee(null);
                } else {
                    User newAssignee = userRepository.findById(newAssigneeId)
                            .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy người nhận việc"));
                    validateUserEligibleAssignee(newAssignee, spaceId);
                    auditChanges.add(String.format("Người nhận việc: %s", newAssignee.getUsername()));
                    issue.setAssignee(newAssignee);
                }
            }
        }

        if (request.isEpicIdPresent() && issue.getIssueType() != IssueType.EPIC && issue.getIssueType() != IssueType.SUBTASK) {
            Long newEpicId = request.getEpicId();
            Long currentEpicId = issue.getEpic() != null ? issue.getEpic().getId() : null;
            if (!Objects.equals(newEpicId, currentEpicId)) {
                spaceAccessService.requireRole(member, SpaceRole.ADMIN, SpaceRole.MANAGER);
                if (newEpicId == null) {
                    auditChanges.add("Tháo liên kết Epic");
                    issue.setEpic(null);
                } else {
                    Issue newEpic = issueRepository.findById(newEpicId)
                            .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy Epic"));
                    if (!newEpic.getSpace().getId().equals(spaceId) || newEpic.getIssueType() != IssueType.EPIC) {
                        throw new IssueOperationException(HttpStatus.BAD_REQUEST, "INVALID_RELATION", "Epic không hợp lệ");
                    }
                    auditChanges.add(String.format("Liên kết Epic: %s", newEpic.getIssueKey()));
                    issue.setEpic(newEpic);
                }
            }
        }

        if (request.isSprintIdPresent() && issue.getIssueType() != IssueType.EPIC && issue.getIssueType() != IssueType.SUBTASK) {
            Long newSprintId = request.getSprintId();
            Long currentSprintId = issue.getSprint() != null ? issue.getSprint().getId() : null;
            if (!Objects.equals(newSprintId, currentSprintId)) {
                spaceAccessService.requireRole(member, SpaceRole.ADMIN, SpaceRole.MANAGER);
                Sprint targetSprint = null;
                if (newSprintId != null) {
                    targetSprint = sprintRepository.findById(newSprintId)
                            .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy Sprint"));
                    if (!targetSprint.getSpace().getId().equals(spaceId)) {
                        throw new IssueOperationException(HttpStatus.BAD_REQUEST, "INVALID_RELATION", "Sprint không cùng Không gian");
                    }
                    if (targetSprint.getStatus() == SprintStatus.CLOSED) {
                        throw new IssueOperationException(HttpStatus.CONFLICT, "SPRINT_STATE_CONFLICT", "Không thể gán vào Sprint đã đóng");
                    }
                }

                // Move issue and all its subtasks
                moveIssueAndSubtasksToSprint(issue, targetSprint, actor, space, auditChanges);
            }
        }

        if (request.isEstimateHoursPresent() && issue.getIssueType() != IssueType.EPIC) {
            if (!Objects.equals(request.getEstimateHours(), issue.getEstimateHours())) {
                auditChanges.add("Cập nhật ước lượng giờ: " + request.getEstimateHours());
                issue.setEstimateHours(request.getEstimateHours());
            }
        }

        if (request.isDueDatePresent()) {
            if (!Objects.equals(request.getDueDate(), issue.getDueDate())) {
                auditChanges.add("Cập nhật hạn chót: " + request.getDueDate());
                issue.setDueDate(request.getDueDate());
            }
        }

        if (request.isLabelsPresent()) {
            Set<String> newLabels = sanitizeLabels(request.getLabels());
            if (!newLabels.equals(issue.getLabels())) {
                auditChanges.add("Cập nhật nhãn: " + String.join(", ", newLabels));
                issue.setLabels(newLabels);
            }
        }

        if (!auditChanges.isEmpty()) {
            issue.setUpdatedBy(actor);
            issue.setVersion(issue.getVersion() + 1);
            issue = issueRepository.save(issue);

            space.setWorkRevision(space.getWorkRevision() + 1);
            spaceRepository.save(space);

            IssueAuditEvent audit = IssueAuditEvent.builder()
                    .issue(issue)
                    .actor(actor)
                    .action("UPDATE")
                    .changes(String.join("; ", auditChanges))
                    .build();
            issueAuditEventRepository.save(audit);
        }

        return toIssueResponse(issue, member.getRole());
    }

    @Transactional
    public IssueResponse transitionStatus(Long id, ChangeIssueStatusRequest request, String username) {
        User actor = spaceAccessService.getActiveUser(username);
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy công việc"));

        Long spaceId = issue.getSpace().getId();
        SpaceMember member = spaceAccessService.getMembershipOrThrow(spaceId, actor.getId());
        spaceAccessService.requireRole(member, SpaceRole.ADMIN, SpaceRole.MANAGER, SpaceRole.MEMBER);

        Space space = spaceAccessService.lockSpaceForWrite(spaceId);

        if (!Objects.equals(issue.getVersion(), request.getVersion())) {
            throw new IssueOperationException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "Công việc đã bị thay đổi bởi người khác. Vui lòng tải lại trang");
        }

        IssueStatus targetStatus = request.getTargetStatus();
        if (issue.getStatus() == targetStatus) {
            return toIssueResponse(issue, member.getRole());
        }

        if (targetStatus == IssueStatus.CANCELLED || (!targetStatus.isDoneCategory() && issue.getStatus().isDoneCategory())) {
            // Reopening or cancelling requires manager/admin
            spaceAccessService.requireRole(member, SpaceRole.ADMIN, SpaceRole.MANAGER);
        }

        // Invariant: If parent transitions to DONE, all its active subtasks must be DONE or CANCELLED
        if (targetStatus == IssueStatus.DONE) {
            List<Issue> subtasks = issueRepository.findSubtasksByParentId(issue.getId());
            List<String> blockingKeys = new ArrayList<>();
            for (Issue sub : subtasks) {
                if (!sub.getStatus().isDoneCategory()) {
                    blockingKeys.add(sub.getIssueKey());
                }
            }
            if (!blockingKeys.isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("blockingIssueKeys", blockingKeys);
                details.put("blockingCount", blockingKeys.size());
                throw new IssueOperationException(
                        HttpStatus.CONFLICT,
                        "OPEN_SUBTASKS",
                        "Còn " + blockingKeys.size() + " công việc con chưa kết thúc. Phải kết thúc các Subtask trước khi hoàn thành công việc cha.",
                        Map.of(),
                        details
                );
            }
        }

        // Invariant: Subtask cannot be reopened if parent is DONE
        if (!targetStatus.isDoneCategory() && issue.getParentIssue() != null) {
            if (issue.getParentIssue().getStatus().isDoneCategory()) {
                throw new IssueOperationException(HttpStatus.CONFLICT, "PARENT_DONE", "Công việc cha đã kết thúc. Vui lòng mở lại công việc cha trước khi mở lại Subtask");
            }
        }

        IssueStatus oldStatus = issue.getStatus();
        issue.setStatus(targetStatus);
        issue.setUpdatedBy(actor);
        issue.setVersion(issue.getVersion() + 1);
        issue = issueRepository.save(issue);

        space.setWorkRevision(space.getWorkRevision() + 1);
        spaceRepository.save(space);

        IssueAuditEvent audit = IssueAuditEvent.builder()
                .issue(issue)
                .actor(actor)
                .action("TRANSITION")
                .changes(String.format("Chuyển trạng thái: %s -> %s", oldStatus, targetStatus))
                .build();
        issueAuditEventRepository.save(audit);

        return toIssueResponse(issue, member.getRole());
    }

    @Transactional
    public void deleteIssue(Long id, Long version, String username) {
        User actor = spaceAccessService.getActiveUser(username);
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy công việc"));

        Long spaceId = issue.getSpace().getId();
        SpaceMember member = spaceAccessService.getMembershipOrThrow(spaceId, actor.getId());
        spaceAccessService.requireRole(member, SpaceRole.ADMIN, SpaceRole.MANAGER);

        Space space = spaceAccessService.lockSpaceForWrite(spaceId);

        if (version != null && !Objects.equals(issue.getVersion(), version)) {
            throw new IssueOperationException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "Công việc đã bị thay đổi bởi người khác");
        }

        // Invariant: cannot delete if issue has active children
        if (issue.getIssueType() == IssueType.EPIC) {
            long childCount = issueRepository.countIssuesByEpicId(issue.getId());
            if (childCount > 0) {
                throw new IssueOperationException(HttpStatus.CONFLICT, "ISSUE_HAS_CHILDREN", "Không thể xóa Epic đang chứa công việc con");
            }
        } else {
            long subtaskCount = issueRepository.countSubtasksByParentId(issue.getId());
            if (subtaskCount > 0) {
                throw new IssueOperationException(HttpStatus.CONFLICT, "ISSUE_HAS_CHILDREN", "Không thể xóa công việc đang chứa Subtask");
            }
        }

        // Invariant: cannot delete if linked to ACTIVE or CLOSED sprint
        if (issue.getSprint() != null) {
            if (issue.getSprint().getStatus() == SprintStatus.ACTIVE || issue.getSprint().getStatus() == SprintStatus.CLOSED) {
                throw new IssueOperationException(HttpStatus.CONFLICT, "DELETE_NOT_ALLOWED", "Không thể xóa công việc đang thuộc Sprint ACTIVE hoặc CLOSED");
            }
            // If in FUTURE sprint, close sprint association interval
            Optional<IssueSprintHistory> openHist = issueSprintHistoryRepository.findOpenHistoryByIssueId(issue.getId());
            openHist.ifPresent(h -> {
                h.setLeftAt(LocalDateTime.now());
                h.setLeftBy(actor);
                h.setLeaveReason("DELETE");
                issueSprintHistoryRepository.save(h);
            });
            issue.setSprint(null);
        }

        issue.setDeletedAt(LocalDateTime.now());
        issue.setDeletedBy(actor);
        issue.setVersion(issue.getVersion() + 1);
        issueRepository.save(issue);

        space.setWorkRevision(space.getWorkRevision() + 1);
        spaceRepository.save(space);

        IssueAuditEvent audit = IssueAuditEvent.builder()
                .issue(issue)
                .actor(actor)
                .action("DELETE")
                .changes("Xóa mềm công việc " + issue.getIssueKey())
                .build();
        issueAuditEventRepository.save(audit);
    }

    @Transactional(readOnly = true)
    public Page<IssueHistoryResponse> getIssueAuditHistory(Long issueId, String username, Pageable pageable) {
        User user = spaceAccessService.getActiveUser(username);
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(issueId)
                .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy công việc"));
        spaceAccessService.getMembershipOrThrow(issue.getSpace().getId(), user.getId());

        return issueAuditEventRepository.findAuditEventsByIssueId(issueId, pageable)
                .map(a -> IssueHistoryResponse.builder()
                        .id(a.getId())
                        .issueId(issueId)
                        .actorId(a.getActor().getId())
                        .actorUsername(a.getActor().getUsername())
                        .actorDisplayName(a.getActor().getFullname() != null ? a.getActor().getFullname() : a.getActor().getUsername())
                        .action(a.getAction())
                        .changes(a.getChanges())
                        .createdAt(a.getCreatedAt())
                        .build());
    }

    @Transactional(readOnly = true)
    public Page<IssueSprintHistoryResponse> getIssueSprintHistory(Long issueId, String username, Pageable pageable) {
        User user = spaceAccessService.getActiveUser(username);
        Issue issue = issueRepository.findByIdAndDeletedAtIsNull(issueId)
                .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy công việc"));
        spaceAccessService.getMembershipOrThrow(issue.getSpace().getId(), user.getId());

        return issueSprintHistoryRepository.findHistoriesByIssueId(issueId, pageable)
                .map(h -> IssueSprintHistoryResponse.builder()
                        .id(h.getId())
                        .issueId(issueId)
                        .sprintId(h.getSprint().getId())
                        .sprintName(h.getSprint().getName())
                        .enteredAt(h.getEnteredAt())
                        .leftAt(h.getLeftAt())
                        .enteredById(h.getEnteredBy().getId())
                        .enteredByDisplayName(h.getEnteredBy().getFullname() != null ? h.getEnteredBy().getFullname() : h.getEnteredBy().getUsername())
                        .leftById(h.getLeftBy() != null ? h.getLeftBy().getId() : null)
                        .leftByDisplayName(h.getLeftBy() != null ? (h.getLeftBy().getFullname() != null ? h.getLeftBy().getFullname() : h.getLeftBy().getUsername()) : null)
                        .enterReason(h.getEnterReason())
                        .leaveReason(h.getLeaveReason())
                        .build());
    }

    private void moveIssueAndSubtasksToSprint(Issue parent, Sprint targetSprint, User actor, Space space, List<String> auditChanges) {
        List<Issue> issuesToUpdate = new ArrayList<>();
        issuesToUpdate.add(parent);
        issuesToUpdate.addAll(issueRepository.findSubtasksByParentId(parent.getId()));

        LocalDateTime now = LocalDateTime.now();
        String sprintLabel = targetSprint != null ? targetSprint.getName() : "Backlog";
        auditChanges.add("Gán Sprint: " + sprintLabel);

        for (Issue i : issuesToUpdate) {
            Optional<IssueSprintHistory> openHist = issueSprintHistoryRepository.findOpenHistoryByIssueId(i.getId());
            openHist.ifPresent(h -> {
                h.setLeftAt(now);
                h.setLeftBy(actor);
                h.setLeaveReason("REASSIGN");
                issueSprintHistoryRepository.save(h);
            });

            if (targetSprint != null) {
                IssueSprintHistory newHist = IssueSprintHistory.builder()
                        .space(space)
                        .issue(i)
                        .sprint(targetSprint)
                        .enteredAt(now)
                        .enteredBy(actor)
                        .enterReason("ASSIGN")
                        .build();
                issueSprintHistoryRepository.save(newHist);
            }

            i.setSprint(targetSprint);
            i.setUpdatedBy(actor);
            if (!i.getId().equals(parent.getId())) {
                i.setVersion(i.getVersion() + 1);
                issueRepository.save(i);
            }
        }
    }

    private void validateUserEligibleMember(User user, Long spaceId) {
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus().name())) {
            throw new IssueOperationException(HttpStatus.BAD_REQUEST, "USER_INACTIVE", "Người dùng không hoạt động");
        }
        if (!spaceMemberRepository.existsBySpaceIdAndUserId(spaceId, user.getId())) {
            throw new IssueOperationException(HttpStatus.BAD_REQUEST, "NOT_SPACE_MEMBER", "Người dùng không thuộc Không gian");
        }
    }

    private void validateUserEligibleAssignee(User user, Long spaceId) {
        validateUserEligibleMember(user, spaceId);
        SpaceMember sm = spaceMemberRepository.findBySpaceIdAndUserId(spaceId, user.getId()).orElse(null);
        if (sm == null || sm.getRole() == SpaceRole.VIEWER) {
            throw new IssueOperationException(HttpStatus.BAD_REQUEST, "VIEWER_CANNOT_BE_ASSIGNED", "Người dùng có vai trò VIEWER không thể nhận phân công việc");
        }
    }

    private Set<String> sanitizeLabels(Set<String> rawLabels) {
        if (rawLabels == null) return new HashSet<>();
        Set<String> clean = new LinkedHashSet<>();
        for (String l : rawLabels) {
            if (l != null && !l.trim().isEmpty()) {
                String trimmed = l.trim().toLowerCase();
                if (trimmed.length() <= 50) {
                    clean.add(trimmed);
                }
                if (clean.size() >= 10) break;
            }
        }
        return clean;
    }

    private IssueResponse toIssueResponse(Issue issue, SpaceRole currentUserRole) {
        Set<String> actions = new HashSet<>();
        if (currentUserRole != null && currentUserRole != SpaceRole.VIEWER) {
            actions.add("EDIT");
            actions.add("TRANSITION");
            actions.add("ASSIGN");
            if (currentUserRole == SpaceRole.ADMIN || currentUserRole == SpaceRole.MANAGER) {
                actions.add("DELETE");
                actions.add("SCHEDULE");
            }
        }

        long childCount = 0;
        if (issue.getIssueType() == IssueType.EPIC) {
            childCount = issueRepository.countIssuesByEpicId(issue.getId());
        } else {
            childCount = issueRepository.countSubtasksByParentId(issue.getId());
        }

        Boolean assigneeEligible = null;
        if (issue.getAssignee() != null) {
            Optional<SpaceMember> sm = spaceMemberRepository.findBySpaceIdAndUserId(issue.getSpace().getId(), issue.getAssignee().getId());
            assigneeEligible = sm.isPresent() && "ACTIVE".equalsIgnoreCase(issue.getAssignee().getStatus().name()) && sm.get().getRole() != SpaceRole.VIEWER;
        }

        Long inheritedEpicId = null;
        if (issue.getParentIssue() != null && issue.getParentIssue().getEpic() != null) {
            inheritedEpicId = issue.getParentIssue().getEpic().getId();
        }

        return IssueResponse.builder()
                .id(issue.getId())
                .key(issue.getIssueKey())
                .spaceId(issue.getSpace().getId())
                .type(issue.getIssueType())
                .summary(issue.getSummary())
                .description(issue.getDescription())
                .status(issue.getStatus())
                .statusCategory(issue.getStatus().isDoneCategory() ? "DONE" : (issue.getStatus() == IssueStatus.TODO ? "TODO" : "IN_PROGRESS"))
                .priority(issue.getPriority())
                .reporter(toUserRef(issue.getReporter()))
                .assignee(toUserRef(issue.getAssignee()))
                .assigneeEligible(assigneeEligible)
                .epicId(issue.getEpic() != null ? issue.getEpic().getId() : null)
                .epicKey(issue.getEpic() != null ? issue.getEpic().getIssueKey() : null)
                .epicSummary(issue.getEpic() != null ? issue.getEpic().getSummary() : null)
                .inheritedEpicId(inheritedEpicId)
                .parentIssueId(issue.getParentIssue() != null ? issue.getParentIssue().getId() : null)
                .parentIssueKey(issue.getParentIssue() != null ? issue.getParentIssue().getIssueKey() : null)
                .parentIssueSummary(issue.getParentIssue() != null ? issue.getParentIssue().getSummary() : null)
                .sprintId(issue.getSprint() != null ? issue.getSprint().getId() : null)
                .sprintName(issue.getSprint() != null ? issue.getSprint().getName() : null)
                .estimateHours(issue.getEstimateHours())
                .dueDate(issue.getDueDate())
                .labels(issue.getLabels() != null ? new LinkedHashSet<>(issue.getLabels()) : Collections.emptySet())
                .childCount(childCount)
                .version(issue.getVersion())
                .createdBy(issue.getCreatedBy().getId())
                .updatedBy(issue.getUpdatedBy().getId())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .allowedActions(actions)
                .build();
    }

    private IssueSummaryResponse toIssueSummaryResponse(Issue issue, SpaceRole currentUserRole) {
        Set<String> actions = new HashSet<>();
        if (currentUserRole != null && currentUserRole != SpaceRole.VIEWER) {
            actions.add("EDIT");
            actions.add("TRANSITION");
            if (currentUserRole == SpaceRole.ADMIN || currentUserRole == SpaceRole.MANAGER) {
                actions.add("DELETE");
            }
        }

        Boolean assigneeEligible = null;
        if (issue.getAssignee() != null) {
            Optional<SpaceMember> sm = spaceMemberRepository.findBySpaceIdAndUserId(issue.getSpace().getId(), issue.getAssignee().getId());
            assigneeEligible = sm.isPresent() && "ACTIVE".equalsIgnoreCase(issue.getAssignee().getStatus().name()) && sm.get().getRole() != SpaceRole.VIEWER;
        }

        Long inheritedEpicId = null;
        if (issue.getParentIssue() != null && issue.getParentIssue().getEpic() != null) {
            inheritedEpicId = issue.getParentIssue().getEpic().getId();
        }

        return IssueSummaryResponse.builder()
                .id(issue.getId())
                .key(issue.getIssueKey())
                .spaceId(issue.getSpace().getId())
                .type(issue.getIssueType())
                .summary(issue.getSummary())
                .status(issue.getStatus())
                .statusCategory(issue.getStatus().isDoneCategory() ? "DONE" : (issue.getStatus() == IssueStatus.TODO ? "TODO" : "IN_PROGRESS"))
                .priority(issue.getPriority())
                .assignee(toUserRef(issue.getAssignee()))
                .assigneeEligible(assigneeEligible)
                .epicId(issue.getEpic() != null ? issue.getEpic().getId() : null)
                .epicKey(issue.getEpic() != null ? issue.getEpic().getIssueKey() : null)
                .inheritedEpicId(inheritedEpicId)
                .parentIssueId(issue.getParentIssue() != null ? issue.getParentIssue().getId() : null)
                .parentIssueKey(issue.getParentIssue() != null ? issue.getParentIssue().getIssueKey() : null)
                .sprintId(issue.getSprint() != null ? issue.getSprint().getId() : null)
                .sprintName(issue.getSprint() != null ? issue.getSprint().getName() : null)
                .estimateHours(issue.getEstimateHours())
                .dueDate(issue.getDueDate())
                .version(issue.getVersion())
                .createdAt(issue.getCreatedAt())
                .allowedActions(actions)
                .build();
    }

    private IssueResponse.UserReference toUserRef(User user) {
        if (user == null) return null;
        return IssueResponse.UserReference.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getFullname() != null ? user.getFullname() : user.getUsername())
                .build();
    }
}