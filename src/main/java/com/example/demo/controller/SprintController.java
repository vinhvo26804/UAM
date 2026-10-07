package com.example.demo.controller;

import com.example.demo.dto.request.sprint.CompleteSprintRequest;
import com.example.demo.dto.request.sprint.CreateSprintRequest;
import com.example.demo.dto.request.sprint.PreviewSprintCompletionRequest;
import com.example.demo.dto.request.sprint.StartSprintRequest;
import com.example.demo.dto.response.sprint.CompleteSprintResponse;
import com.example.demo.dto.response.sprint.SprintCompletionPreviewResponse;
import com.example.demo.dto.response.sprint.SprintResponse;
import com.example.demo.entity.SprintStatus;
import com.example.demo.service.SprintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sprints")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    @PostMapping
    public ResponseEntity<SprintResponse> createSprint(
            @Valid @RequestBody CreateSprintRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sprintService.createSprint(request, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<Page<SprintResponse>> getSprints(
            @RequestParam Long spaceId,
            @RequestParam(required = false) SprintStatus status,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(sprintService.getSprints(spaceId, status, userDetails.getUsername(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SprintResponse> getSprintDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(sprintService.getSprintDetail(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<SprintResponse> startSprint(
            @PathVariable Long id,
            @Valid @RequestBody StartSprintRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(sprintService.startSprint(id, request, userDetails.getUsername()));
    }

    @PostMapping("/{id}/complete-preview")
    public ResponseEntity<SprintCompletionPreviewResponse> previewSprintCompletion(
            @PathVariable Long id,
            @RequestBody(required = false) PreviewSprintCompletionRequest request,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(sprintService.previewSprintCompletion(id, request, userDetails.getUsername(), pageable));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<CompleteSprintResponse> completeSprint(
            @PathVariable Long id,
            @Valid @RequestBody CompleteSprintRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(sprintService.completeSprint(id, request, idempotencyKey, userDetails.getUsername()));
    }
}
