package com.example.demo.service;

import com.example.demo.dto.request.sprint.*;
import com.example.demo.dto.response.sprint.CompleteSprintResponse;
import com.example.demo.dto.response.sprint.SprintCompletionPreviewResponse;
import com.example.demo.dto.response.sprint.SprintResponse;
import com.example.demo.entity.*;
import com.example.demo.respository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SprintService {

    private final SprintRepository sprintRepository;
    private final SpaceRepository spaceRepository;
    private final IssueRepository issueRepository;
    private final IssueSprintHistoryRepository issueSprintHistoryRepository;
    private final IssueAuditEventRepository issueAuditEventRepository;
    private final SpaceAccessService spaceAccessService;
    private final IdempotencyService idempotencyService;

    @Transactional
    public SprintResponse createSprint(CreateSprintRequest request, String username) {
        User user = spaceAccessService.getActiveUser(username);
        SpaceMember member = spaceAccessService.getMembershipOrThrow(request.getSpaceId(), user.getId());
        spaceAccessService.requireRole(member, SpaceRole.ADMIN, SpaceRole.MANAGER);

        Space space = spaceAccessService.lockSpaceForWrite(request.getSpaceId());

        if (request.getPlannedStartAt() != null || request.getPlannedEndAt() != null) {
            if (request.getPlannedStartAt() == null || request.getPlannedEndAt() == null) {
                throw new IssueOperationException(HttpStatus.BAD_REQUEST, "INVALID_FIELD", "Ngày bắt đầu và kết thúc dự kiến phải cùng có hoặc cùng để trống");
            }
            if (!request.getPlannedStartAt().isBefore(request.getPlannedEndAt())) {
                throw new IssueOperationException(HttpStatus.BAD_REQUEST, "INVALID_FIELD", "Ngày bắt đầu dự kiến phải trước ngày kết thúc dự kiến");
            }
        }

        Long maxPos = sprintRepository.findMaxPositionBySpaceId(space.getId());
        long nextPos = (maxPos == null ? 0L : maxPos) + 1L;

        Sprint sprint = Sprint.builder()
                .space(space)
                .name(request.getName().trim())
                .goal(request.getGoal() != null ? request.getGoal().trim() : null)
                .status(SprintStatus.FUTURE)
                .position(nextPos)
                .plannedStartAt(request.getPlannedStartAt())
                .plannedEndAt(request.getPlannedEndAt())
                .createdBy(user)
                .updatedBy(user)
                .build();
        sprint = sprintRepository.save(sprint);

        return toSprintResponse(sprint, member.getRole());
    }

    @Transactional(readOnly = true)
    public Page<SprintResponse> getSprints(Long spaceId, SprintStatus status, String username, Pageable pageable) {
        User user = spaceAccessService.getActiveUser(username);
        SpaceMember member = spaceAccessService.getMembershipOrThrow(spaceId, user.getId());

        return sprintRepository.findSprintsBySpaceIdAndStatus(spaceId, status, pageable)
                .map(s -> toSprintResponse(s, member.getRole()));
    }

    @Transactional(readOnly = true)
    public SprintResponse getSprintDetail(Long sprintId, String username) {
        User user = spaceAccessService.getActiveUser(username);
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy Sprint"));
        SpaceMember member = spaceAccessService.getMembershipOrThrow(sprint.getSpace().getId(), user.getId());
        return toSprintResponse(sprint, member.getRole());
    }

    @Transactional
    public SprintResponse startSprint(Long sprintId, StartSprintRequest request, String username) {
        User user = spaceAccessService.getActiveUser(username);
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy Sprint"));
        SpaceMember member = spaceAccessService.getMembershipOrThrow(sprint.getSpace().getId(), user.getId());
        spaceAccessService.requireRole(member, SpaceRole.ADMIN, SpaceRole.MANAGER);

        spaceAccessService.lockSpaceForWrite(sprint.getSpace().getId());

        if (sprint.getStatus() != SprintStatus.FUTURE) {
            throw new IssueOperationException(HttpStatus.CONFLICT, "SPRINT_STATE_CONFLICT", "Chỉ có thể bắt đầu Sprint đang ở trạng thái FUTURE");
        }

        if (!Objects.equals(sprint.getVersion(), request.getVersion())) {
            throw new IssueOperationException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "Sprint đã bị chỉnh sửa bởi người khác. Vui lòng tải lại trang");
        }

        boolean hasActiveSprint = sprintRepository.existsBySpaceIdAndStatus(sprint.getSpace().getId(), SprintStatus.ACTIVE);
        if (hasActiveSprint) {
            throw new IssueOperationException(HttpStatus.CONFLICT, "SPRINT_STATE_CONFLICT", "Không gian đã có một Sprint đang ACTIVE. Mỗi thời điểm chỉ cho phép 1 Sprint chạy");
        }

        sprint.setStatus(SprintStatus.ACTIVE);
        sprint.setStartedAt(LocalDateTime.now());
        sprint.setUpdatedBy(user);
        sprint = sprintRepository.save(sprint);

        return toSprintResponse(sprint, member.getRole());
    }

    @Transactional(readOnly = true)
    public SprintCompletionPreviewResponse previewSprintCompletion(
            Long sprintId,
            PreviewSprintCompletionRequest request,
            String username,
            Pageable pageable
    ) {
        User user = spaceAccessService.getActiveUser(username);
        Sprint sourceSprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy Sprint"));
        SpaceMember member = spaceAccessService.getMembershipOrThrow(sourceSprint.getSpace().getId(), user.getId());
        spaceAccessService.requireRole(member, SpaceRole.ADMIN, SpaceRole.MANAGER);

        if (sourceSprint.getStatus() != SprintStatus.ACTIVE) {
            throw new IssueOperationException(HttpStatus.CONFLICT, "SPRINT_STATE_CONFLICT", "Chỉ có thể xem preview kết thúc cho Sprint đang ACTIVE");
        }

        Long spaceId = sourceSprint.getSpace().getId();
        Space space = spaceAccessService.getSpaceOrThrow(spaceId);

        // Analyze issues in this sprint
        List<Issue> rootIssues = issueRepository.findRootIssuesBySprintId(sprintId);
        List<SprintCompletionPreviewResponse.PreviewItem> itemsToMove = new ArrayList<>();
        long stayCount = 0;
        long moveRootCount = 0;
        long doneSubtasksFollowingParent = 0;

        for (Issue root : rootIssues) {
            if (root.getStatus().isDoneCategory()) {
                // Done root issues stay in closed sprint
                stayCount++;
            } else {
                // Open root issue moves with all its subtasks
                moveRootCount++;
                itemsToMove.add(SprintCompletionPreviewResponse.PreviewItem.builder()
                        .id(root.getId())
                        .key(root.getIssueKey())
                        .type(root.getIssueType())
                        .status(root.getStatus())
                        .parentIssueId(null)
                        .reason("OPEN_ROOT")
                        .build());

                List<Issue> subtasks = issueRepository.findSubtasksByParentId(root.getId());
                for (Issue sub : subtasks) {
                    if (sub.getStatus().isDoneCategory()) {
                        doneSubtasksFollowingParent++;
                    }
                    itemsToMove.add(SprintCompletionPreviewResponse.PreviewItem.builder()
                            .id(sub.getId())
                            .key(sub.getIssueKey())
                            .type(sub.getIssueType())
                            .status(sub.getStatus())
                            .parentIssueId(root.getId())
                            .reason(sub.getStatus().isDoneCategory() ? "FOLLOW_PARENT" : "OPEN_PARENT")
                            .build());
                }
            }
        }

        List<String> blockers = new ArrayList<>();
        boolean targetRequired = !itemsToMove.isEmpty();
        boolean hasTarget = (request != null && (request.getTargetSprintId() != null || request.getNewSprint() != null));

        Long resolvedTargetSprintId = null;
        Long targetVersion = null;

        if (targetRequired && !hasTarget) {
            blockers.add("TARGET_REQUIRED: Còn công việc chưa hoàn thành, cần chỉ định Sprint đích hoặc tạo Sprint mới để chuyển");
        } else if (request != null && request.getTargetSprintId() != null) {
            Sprint targetSprint = sprintRepository.findById(request.getTargetSprintId())
                    .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy Sprint đích"));
            if (!targetSprint.getSpace().getId().equals(spaceId)) {
                blockers.add("INVALID_RELATION: Sprint đích phải thuộc cùng Không gian");
            } else if (targetSprint.getStatus() != SprintStatus.FUTURE) {
                blockers.add("SPRINT_STATE_CONFLICT: Sprint đích phải ở trạng thái FUTURE");
            } else if (targetSprint.getId().equals(sourceSprint.getId())) {
                blockers.add("INVALID_RELATION: Sprint đích không thể trùng với Sprint nguồn");
            } else {
                resolvedTargetSprintId = targetSprint.getId();
                targetVersion = targetSprint.getVersion();
            }
        }

        // Suggested next sprint
        Long suggestedTargetSprintId = null;
        Page<Sprint> nextSprints = sprintRepository.findNextFutureSprint(spaceId, sourceSprint.getPosition(), PageRequest.of(0, 1));
        if (nextSprints.hasContent()) {
            suggestedTargetSprintId = nextSprints.getContent().get(0).getId();
        }

        // Paginate preview items
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), itemsToMove.size());
        List<SprintCompletionPreviewResponse.PreviewItem> pagedItems = (start <= end && start < itemsToMove.size())
                ? itemsToMove.subList(start, end)
                : Collections.emptyList();
        Page<SprintCompletionPreviewResponse.PreviewItem> page = new PageImpl<>(pagedItems, pageable, itemsToMove.size());

        return SprintCompletionPreviewResponse.builder()
                .sourceSprintId(sourceSprint.getId())
                .sourceVersion(sourceSprint.getVersion())
                .spaceRevision(space.getWorkRevision())
                .targetSprintId(resolvedTargetSprintId)
                .targetVersion(targetVersion)
                .suggestedTargetSprintId(suggestedTargetSprintId)
                .moveRootCount(moveRootCount)
                .moveIssueCount(itemsToMove.size())
                .includedDoneSubtaskCount(doneSubtasksFollowingParent)
                .stayCount(stayCount)
                .canComplete(blockers.isEmpty())
                .blockers(blockers)
                .items(page)
                .build();
    }

    @Transactional
    public CompleteSprintResponse completeSprint(
            Long sprintId,
            CompleteSprintRequest request,
            UUID idempotencyKey,
            String username
    ) {
        User actor = spaceAccessService.getActiveUser(username);
        Sprint sourceSprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy Sprint"));
        SpaceMember member = spaceAccessService.getMembershipOrThrow(sourceSprint.getSpace().getId(), actor.getId());
        spaceAccessService.requireRole(member, SpaceRole.ADMIN, SpaceRole.MANAGER);

        Long spaceId = sourceSprint.getSpace().getId();
        Space space = spaceAccessService.lockSpaceForWrite(spaceId);

        String canonicalPayload = String.format("COMPLETE_SPRINT:%d:%d:%d:%s",
                sprintId, request.getVersion(), request.getSpaceRevision(),
                request.getTargetSprintId() != null ? request.getTargetSprintId().toString() : "new");
        String payloadHash = idempotencyService.computeHash(canonicalPayload);

        if (idempotencyKey != null) {
            Optional<IdempotencyRecord> existingRecord = idempotencyService.findRecord(actor, "SPRINT_COMPLETE", idempotencyKey);
            if (existingRecord.isPresent()) {
                IdempotencyRecord rec = existingRecord.get();
                idempotencyService.validateNotReusedWithDifferentPayload(rec, payloadHash);
                // Return cached replay response
                return CompleteSprintResponse.builder()
                        .sourceSprintId(sourceSprint.getId())
                        .sourceStatus("CLOSED")
                        .sourceVersion(sourceSprint.getVersion())
                        .targetSprintId(request.getTargetSprintId())
                        .movedIssueCount(0)
                        .retainedIssueCount(0)
                        .spaceRevision(space.getWorkRevision())
                        .build();
            }
        }

        if (sourceSprint.getStatus() != SprintStatus.ACTIVE) {
            throw new IssueOperationException(HttpStatus.CONFLICT, "SPRINT_STATE_CONFLICT", "Chỉ có thể kết thúc Sprint đang ở trạng thái ACTIVE");
        }

        if (!Objects.equals(sourceSprint.getVersion(), request.getVersion())) {
            throw new IssueOperationException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "Sprint đã bị chỉnh sửa bởi người khác. Vui lòng tải lại trang");
        }

        if (!Objects.equals(space.getWorkRevision(), request.getSpaceRevision())) {
            throw new IssueOperationException(HttpStatus.CONFLICT, "PREVIEW_STALE", "Dữ liệu công việc trong Không gian đã thay đổi kể từ khi bạn xem preview. Vui lòng kiểm tra lại");
        }

        // Recompute items to move
        List<Issue> rootIssues = issueRepository.findRootIssuesBySprintId(sprintId);
        List<Issue> issuesToMove = new ArrayList<>();
        long retainedCount = 0;

        for (Issue root : rootIssues) {
            if (root.getStatus().isDoneCategory()) {
                retainedCount++;
            } else {
                issuesToMove.add(root);
                List<Issue> subtasks = issueRepository.findSubtasksByParentId(root.getId());
                issuesToMove.addAll(subtasks);
            }
        }

        Sprint targetSprint = null;
        if (!issuesToMove.isEmpty()) {
            if (request.getTargetSprintId() == null && request.getNewSprint() == null) {
                throw new IssueOperationException(HttpStatus.BAD_REQUEST, "TARGET_REQUIRED", "Còn công việc chưa hoàn thành, cần chỉ định Sprint đích hoặc tạo Sprint mới");
            }
            if (request.getTargetSprintId() != null && request.getNewSprint() != null) {
                throw new IssueOperationException(HttpStatus.BAD_REQUEST, "INVALID_FIELD", "Chỉ được chọn một trong hai: Sprint đích có sẵn hoặc tạo mới");
            }

            if (request.getTargetSprintId() != null) {
                targetSprint = sprintRepository.findById(request.getTargetSprintId())
                        .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy Sprint đích"));
                if (!targetSprint.getSpace().getId().equals(spaceId)) {
                    throw new IssueOperationException(HttpStatus.BAD_REQUEST, "INVALID_RELATION", "Sprint đích phải thuộc cùng Không gian");
                }
                if (targetSprint.getStatus() != SprintStatus.FUTURE) {
                    throw new IssueOperationException(HttpStatus.CONFLICT, "SPRINT_STATE_CONFLICT", "Sprint đích phải ở trạng thái FUTURE");
                }
                if (request.getTargetVersion() != null && !Objects.equals(targetSprint.getVersion(), request.getTargetVersion())) {
                    throw new IssueOperationException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "Sprint đích đã bị cập nhật bởi người khác");
                }
            } else {
                // Create new destination sprint
                NewSprintDestination nsd = request.getNewSprint();
                if (nsd.getName() == null || nsd.getName().trim().isEmpty()) {
                    throw new IssueOperationException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Tên Sprint đích mới không được để trống");
                }
                Long maxPos = sprintRepository.findMaxPositionBySpaceId(spaceId);
                long nextPos = (maxPos == null ? 0L : maxPos) + 1L;

                targetSprint = Sprint.builder()
                        .space(space)
                        .name(nsd.getName().trim())
                        .goal(nsd.getGoal() != null ? nsd.getGoal().trim() : null)
                        .status(SprintStatus.FUTURE)
                        .position(nextPos)
                        .plannedStartAt(nsd.getPlannedStartAt())
                        .plannedEndAt(nsd.getPlannedEndAt())
                        .createdBy(actor)
                        .updatedBy(actor)
                        .build();
                targetSprint = sprintRepository.save(targetSprint);
            }
        }

        // Move issues
        LocalDateTime now = LocalDateTime.now();
        for (Issue issue : issuesToMove) {
            // Close old history
            Optional<IssueSprintHistory> openHist = issueSprintHistoryRepository.findOpenHistoryByIssueId(issue.getId());
            openHist.ifPresent(h -> {
                h.setLeftAt(now);
                h.setLeftBy(actor);
                h.setLeaveReason("COMPLETE_SPRINT");
                issueSprintHistoryRepository.save(h);
            });

            // Open new history
            IssueSprintHistory newHist = IssueSprintHistory.builder()
                    .space(space)
                    .issue(issue)
                    .sprint(targetSprint)
                    .enteredAt(now)
                    .enteredBy(actor)
                    .enterReason("COMPLETE_SPRINT")
                    .build();
            issueSprintHistoryRepository.save(newHist);

            issue.setSprint(targetSprint);
            issue.setUpdatedBy(actor);
            issue.setVersion(issue.getVersion() + 1);
            issueRepository.save(issue);

            // Audit
            IssueAuditEvent audit = IssueAuditEvent.builder()
                    .issue(issue)
                    .actor(actor)
                    .action("SPRINT_MOVE")
                    .changes(String.format("Chuyển từ Sprint %s sang Sprint %s do kết thúc Sprint", sourceSprint.getName(), targetSprint.getName()))
                    .build();
            issueAuditEventRepository.save(audit);
        }

        // Close source sprint
        sourceSprint.setStatus(SprintStatus.CLOSED);
        sourceSprint.setCompletedAt(now);
        sourceSprint.setUpdatedBy(actor);
        sourceSprint = sprintRepository.save(sourceSprint);

        // Update space revision
        space.setWorkRevision(space.getWorkRevision() + 1);
        spaceRepository.save(space);

        CompleteSprintResponse response = CompleteSprintResponse.builder()
                .sourceSprintId(sourceSprint.getId())
                .sourceStatus("CLOSED")
                .sourceVersion(sourceSprint.getVersion())
                .targetSprintId(targetSprint != null ? targetSprint.getId() : null)
                .movedIssueCount(issuesToMove.size())
                .retainedIssueCount(retainedCount)
                .spaceRevision(space.getWorkRevision())
                .build();

        if (idempotencyKey != null) {
            idempotencyService.saveRecord(actor, space, "SPRINT_COMPLETE", idempotencyKey, payloadHash, 200, response, sourceSprint.getId());
        }

        return response;
    }

    private SprintResponse toSprintResponse(Sprint sprint, SpaceRole currentUserRole) {
        Set<String> actions = new HashSet<>();
        if (currentUserRole == SpaceRole.ADMIN || currentUserRole == SpaceRole.MANAGER) {
            if (sprint.getStatus() == SprintStatus.FUTURE) {
                actions.add("START");
                actions.add("EDIT");
            } else if (sprint.getStatus() == SprintStatus.ACTIVE) {
                actions.add("COMPLETE");
                actions.add("EDIT");
            }
        }

        return SprintResponse.builder()
                .id(sprint.getId())
                .spaceId(sprint.getSpace().getId())
                .name(sprint.getName())
                .goal(sprint.getGoal())
                .status(sprint.getStatus())
                .position(sprint.getPosition())
                .plannedStartAt(sprint.getPlannedStartAt())
                .plannedEndAt(sprint.getPlannedEndAt())
                .startedAt(sprint.getStartedAt())
                .completedAt(sprint.getCompletedAt())
                .version(sprint.getVersion())
                .allowedActions(actions)
                .createdAt(sprint.getCreatedAt())
                .updatedAt(sprint.getUpdatedAt())
                .build();
    }
}
