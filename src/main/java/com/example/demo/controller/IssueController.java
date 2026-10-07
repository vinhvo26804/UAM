package com.example.demo.controller;

import com.example.demo.dto.request.issue.ChangeIssueStatusRequest;
import com.example.demo.dto.request.issue.CreateIssueRequest;
import com.example.demo.dto.request.issue.UpdateIssueRequest;
import com.example.demo.dto.response.issue.IssueHistoryResponse;
import com.example.demo.dto.response.issue.IssueResponse;
import com.example.demo.dto.response.issue.IssueSprintHistoryResponse;
import com.example.demo.dto.response.issue.IssueSummaryResponse;
import com.example.demo.entity.IssuePriority;
import com.example.demo.entity.IssueStatus;
import com.example.demo.entity.IssueType;
import com.example.demo.service.IssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    @PostMapping
    public ResponseEntity<IssueResponse> createIssue(
            @Valid @RequestBody CreateIssueRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        IssueResponse response = issueService.createIssue(request, idempotencyKey, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<IssueSummaryResponse>> searchIssues(
            @RequestParam Long spaceId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) IssueType type,
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Long epicId,
            @RequestParam(required = false) Long parentIssueId,
            @RequestParam(required = false) Long sprintId,
            @RequestParam(required = false) Boolean backlog,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(issueService.searchIssues(
                spaceId, q, type, status, assigneeId, epicId, parentIssueId, sprintId, backlog,
                userDetails.getUsername(), pageable
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IssueResponse> getIssueDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(issueService.getIssueDetail(id, userDetails.getUsername()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<IssueResponse> updateIssue(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIssueRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(issueService.updateIssue(id, request, userDetails.getUsername()));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<IssueResponse> transitionStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeIssueStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(issueService.transitionStatus(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIssue(
            @PathVariable Long id,
            @RequestParam(required = false) Long version,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        issueService.deleteIssue(id, version, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<Page<IssueHistoryResponse>> getIssueAuditHistory(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(issueService.getIssueAuditHistory(id, userDetails.getUsername(), pageable));
    }

    @GetMapping("/{id}/sprint-history")
    public ResponseEntity<Page<IssueSprintHistoryResponse>> getIssueSprintHistory(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(issueService.getIssueSprintHistory(id, userDetails.getUsername(), pageable));
    }
}
