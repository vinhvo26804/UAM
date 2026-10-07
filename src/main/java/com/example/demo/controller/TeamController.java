package com.example.demo.controller;

import com.example.demo.dto.request.team.AddTeamMemberRequest;
import com.example.demo.dto.request.team.CreateTeamRequest;
import com.example.demo.dto.request.team.UpdateTeamMemberRoleRequest;
import com.example.demo.dto.response.team.CandidateUserResponse;
import com.example.demo.dto.response.team.TeamMemberResponse;
import com.example.demo.dto.response.team.TeamResponse;
import com.example.demo.service.TeamService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@Tag(name = "Team Management")
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(
            @Valid @RequestBody CreateTeamRequest request,
            Authentication authentication
    ) {
        TeamResponse response = teamService.createTeam(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TeamResponse>> getMyTeams(Authentication authentication) {
        List<TeamResponse> teams = teamService.getMyTeams(authentication.getName());
        return ResponseEntity.ok(teams);
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponse> getTeamDetail(
            @PathVariable Long teamId,
            Authentication authentication
    ) {
        TeamResponse response = teamService.getTeamDetail(teamId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{teamId}/members")
    public ResponseEntity<Page<TeamMemberResponse>> getTeamMembers(
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        int validatedPage = Math.max(0, page);
        int validatedSize = Math.min(100, Math.max(1, size));
        Pageable pageable = PageRequest.of(validatedPage, validatedSize);

        Page<TeamMemberResponse> response = teamService.getTeamMembers(teamId, pageable, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{teamId}/candidate-users")
    public ResponseEntity<List<CandidateUserResponse>> searchCandidates(
            @PathVariable Long teamId,
            @RequestParam(required = false) String query,
            Authentication authentication
    ) {
        List<CandidateUserResponse> candidates = teamService.searchCandidates(teamId, query, authentication.getName());
        return ResponseEntity.ok(candidates);
    }

    @PostMapping("/{teamId}/members")
    public ResponseEntity<TeamMemberResponse> addMember(
            @PathVariable Long teamId,
            @Valid @RequestBody AddTeamMemberRequest request,
            Authentication authentication
    ) {
        TeamMemberResponse response = teamService.addMember(teamId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{teamId}/members/{userId}/role")
    public ResponseEntity<TeamMemberResponse> updateMemberRole(
            @PathVariable Long teamId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateTeamMemberRoleRequest request,
            Authentication authentication
    ) {
        TeamMemberResponse response = teamService.updateMemberRole(teamId, userId, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long teamId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        teamService.removeMember(teamId, userId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : "Yêu cầu thất bại";
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Dữ liệu không hợp lệ");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }
}
