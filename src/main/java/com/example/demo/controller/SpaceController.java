package com.example.demo.controller;

import com.example.demo.dto.request.space.AddSpaceMemberRequest;
import com.example.demo.dto.request.space.CreateSpaceRequest;
import com.example.demo.dto.request.space.UpdateSpaceMemberRoleRequest;
import com.example.demo.dto.response.space.SpaceMemberResponse;
import com.example.demo.dto.response.space.SpaceResponse;
import com.example.demo.entity.SpaceRole;
import com.example.demo.service.SpaceService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;

    @PostMapping
    public ResponseEntity<SpaceResponse> createSpace(
            @Valid @RequestBody CreateSpaceRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        SpaceResponse response = spaceService.createSpace(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<SpaceResponse>> getMySpaces(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(spaceService.getMySpaces(userDetails.getUsername(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpaceResponse> getSpaceDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(spaceService.getSpaceDetail(id, userDetails.getUsername()));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<Page<SpaceMemberResponse>> getMembers(
            @PathVariable Long id,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(spaceService.getMembers(id, query, userDetails.getUsername(), pageable));
    }

    @GetMapping("/{id}/candidate-users")
    public ResponseEntity<List<Map<String, Object>>> searchCandidateUsers(
            @PathVariable Long id,
            @RequestParam(required = false) String query,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(spaceService.searchCandidateUsers(id, query, userDetails.getUsername()));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<SpaceMemberResponse> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddSpaceMemberRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(spaceService.addMember(id, request, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/members/{userId}/role")
    public ResponseEntity<SpaceMemberResponse> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateSpaceMemberRoleRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(spaceService.updateMemberRole(id, userId, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestParam(required = false) SpaceRole expectedRole,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        spaceService.removeMember(id, userId, expectedRole, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
