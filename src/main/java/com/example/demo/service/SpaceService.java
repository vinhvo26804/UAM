package com.example.demo.service;

import com.example.demo.dto.request.space.AddSpaceMemberRequest;
import com.example.demo.dto.request.space.CreateSpaceRequest;
import com.example.demo.dto.request.space.UpdateSpaceMemberRoleRequest;
import com.example.demo.dto.response.space.SpaceMemberResponse;
import com.example.demo.dto.response.space.SpaceResponse;
import com.example.demo.entity.IssueType;
import com.example.demo.entity.Space;
import com.example.demo.entity.SpaceMember;
import com.example.demo.entity.SpaceRole;
import com.example.demo.entity.User;
import com.example.demo.respository.SpaceMemberRepository;
import com.example.demo.respository.SpaceRepository;
import com.example.demo.respository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final SpaceMemberRepository spaceMemberRepository;
    private final SpaceAccessService spaceAccessService;
    private final UserRepository userRepository;

    @Transactional
    public SpaceResponse createSpace(CreateSpaceRequest request, String username) {
        User user = spaceAccessService.getActiveUser(username);

        String trimmedKey = request.getKey().trim().toUpperCase();
        if (spaceRepository.existsByKey(trimmedKey)) {
            throw new IssueOperationException(HttpStatus.CONFLICT, "SPACE_KEY_EXISTS", "Mã Không gian đã tồn tại");
        }

        Set<IssueType> allowedTypes = request.getAllowedIssueTypes();
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            allowedTypes = new HashSet<>(Arrays.asList(IssueType.values()));
        } else if (allowedTypes.contains(IssueType.SUBTASK)) {
            boolean hasParentType = allowedTypes.contains(IssueType.STORY) ||
                                    allowedTypes.contains(IssueType.TASK) ||
                                    allowedTypes.contains(IssueType.BUG);
            if (!hasParentType) {
                throw new IssueOperationException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_FIELD",
                        "Nếu cho phép SUBTASK thì Không gian phải hỗ trợ ít nhất một loại công việc cha (STORY, TASK hoặc BUG)"
                );
            }
        }

        Space space = Space.builder()
                .key(trimmedKey)
                .name(request.getName().trim())
                .createdBy(user)
                .allowedIssueTypes(allowedTypes)
                .build();
        space = spaceRepository.save(space);

        SpaceMember adminMember = SpaceMember.builder()
                .space(space)
                .user(user)
                .role(SpaceRole.ADMIN)
                .build();
        spaceMemberRepository.save(adminMember);

        return toSpaceResponse(space, SpaceRole.ADMIN);
    }

    @Transactional(readOnly = true)
    public Page<SpaceResponse> getMySpaces(String username, Pageable pageable) {
        User user = spaceAccessService.getActiveUser(username);
        return spaceRepository.findSpacesByUserId(user.getId(), pageable)
                .map(s -> {
                    SpaceRole role = spaceMemberRepository.findBySpaceIdAndUserId(s.getId(), user.getId())
                            .map(SpaceMember::getRole)
                            .orElse(null);
                    return toSpaceResponse(s, role);
                });
    }

    @Transactional(readOnly = true)
    public SpaceResponse getSpaceDetail(Long spaceId, String username) {
        User user = spaceAccessService.getActiveUser(username);
        Space space = spaceAccessService.getSpaceOrThrow(spaceId);
        SpaceMember member = spaceAccessService.getMembershipOrThrow(spaceId, user.getId());
        return toSpaceResponse(space, member.getRole());
    }

    @Transactional(readOnly = true)
    public Page<SpaceMemberResponse> getMembers(Long spaceId, String query, String username, Pageable pageable) {
        User user = spaceAccessService.getActiveUser(username);
        spaceAccessService.getMembershipOrThrow(spaceId, user.getId());

        String trimmedQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        return spaceMemberRepository.findMembersBySpaceId(spaceId, trimmedQuery, pageable)
                .map(this::toSpaceMemberResponse);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchCandidateUsers(Long spaceId, String query, String username) {
        User user = spaceAccessService.getActiveUser(username);
        SpaceMember currentMember = spaceAccessService.getMembershipOrThrow(spaceId, user.getId());
        spaceAccessService.requireRole(currentMember, SpaceRole.ADMIN);

        if (query == null || query.trim().length() < 2) {
            return Collections.emptyList();
        }

        List<User> candidates = spaceMemberRepository.findCandidateUsers(spaceId, query.trim(), PageRequest.of(0, 10));
        List<Map<String, Object>> results = new ArrayList<>();
        for (User u : candidates) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("username", u.getUsername());
            item.put("fullname", u.getFullname() != null ? u.getFullname() : u.getUsername());
            results.add(item);
        }
        return results;
    }

    @Transactional
    public SpaceMemberResponse addMember(Long spaceId, AddSpaceMemberRequest request, String username) {
        User actor = spaceAccessService.getActiveUser(username);
        SpaceMember currentMember = spaceAccessService.getMembershipOrThrow(spaceId, actor.getId());
        spaceAccessService.requireRole(currentMember, SpaceRole.ADMIN);

        Space space = spaceAccessService.lockSpaceForWrite(spaceId);

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy người dùng"));

        if (!"ACTIVE".equalsIgnoreCase(targetUser.getStatus().name())) {
            throw new IssueOperationException(HttpStatus.BAD_REQUEST, "USER_INACTIVE", "Chỉ có thể thêm người dùng có tài khoản ACTIVE vào Không gian");
        }

        if (spaceMemberRepository.existsBySpaceIdAndUserId(spaceId, targetUser.getId())) {
            throw new IssueOperationException(HttpStatus.CONFLICT, "MEMBERSHIP_CONFLICT", "Người dùng đã là thành viên của Không gian");
        }

        SpaceMember newMember = SpaceMember.builder()
                .space(space)
                .user(targetUser)
                .role(request.getRole())
                .build();
        newMember = spaceMemberRepository.save(newMember);

        return toSpaceMemberResponse(newMember);
    }

    @Transactional
    public SpaceMemberResponse updateMemberRole(Long spaceId, Long targetUserId, UpdateSpaceMemberRoleRequest request, String username) {
        User actor = spaceAccessService.getActiveUser(username);
        SpaceMember currentMember = spaceAccessService.getMembershipOrThrow(spaceId, actor.getId());
        spaceAccessService.requireRole(currentMember, SpaceRole.ADMIN);

        spaceAccessService.lockSpaceForWrite(spaceId);

        SpaceMember targetMember = spaceMemberRepository.findBySpaceIdAndUserId(spaceId, targetUserId)
                .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy thành viên trong Không gian"));

        if (request.getExpectedRole() != null && targetMember.getRole() != request.getExpectedRole()) {
            throw new IssueOperationException(HttpStatus.CONFLICT, "MEMBERSHIP_CONFLICT", "Vai trò của thành viên đã bị thay đổi bởi người khác");
        }

        if (targetMember.getRole() == SpaceRole.ADMIN && request.getRole() != SpaceRole.ADMIN) {
            long adminCount = spaceMemberRepository.countBySpaceIdAndRole(spaceId, SpaceRole.ADMIN);
            if (adminCount <= 1) {
                throw new IssueOperationException(HttpStatus.CONFLICT, "LAST_ADMIN", "Không thể hạ quyền Quản trị viên cuối cùng của Không gian");
            }
        }

        targetMember.setRole(request.getRole());
        targetMember = spaceMemberRepository.save(targetMember);

        return toSpaceMemberResponse(targetMember);
    }

    @Transactional
    public void removeMember(Long spaceId, Long targetUserId, SpaceRole expectedRole, String username) {
        User actor = spaceAccessService.getActiveUser(username);
        SpaceMember currentMember = spaceAccessService.getMembershipOrThrow(spaceId, actor.getId());
        spaceAccessService.requireRole(currentMember, SpaceRole.ADMIN);

        spaceAccessService.lockSpaceForWrite(spaceId);

        SpaceMember targetMember = spaceMemberRepository.findBySpaceIdAndUserId(spaceId, targetUserId)
                .orElseThrow(() -> new IssueOperationException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy thành viên trong Không gian"));

        if (expectedRole != null && targetMember.getRole() != expectedRole) {
            throw new IssueOperationException(HttpStatus.CONFLICT, "MEMBERSHIP_CONFLICT", "Vai trò của thành viên đã bị thay đổi trước khi xóa");
        }

        if (targetMember.getRole() == SpaceRole.ADMIN) {
            long adminCount = spaceMemberRepository.countBySpaceIdAndRole(spaceId, SpaceRole.ADMIN);
            if (adminCount <= 1) {
                throw new IssueOperationException(HttpStatus.CONFLICT, "LAST_ADMIN", "Không thể xóa Quản trị viên cuối cùng của Không gian");
            }
        }

        spaceMemberRepository.deleteBySpaceIdAndUserId(spaceId, targetUserId);
    }

    private SpaceResponse toSpaceResponse(Space space, SpaceRole currentUserRole) {
        return SpaceResponse.builder()
                .id(space.getId())
                .key(space.getKey())
                .name(space.getName())
                .archived(space.isArchived())
                .workRevision(space.getWorkRevision())
                .allowedIssueTypes(space.getAllowedIssueTypes())
                .currentUserRole(currentUserRole)
                .capabilities(spaceAccessService.getSpaceCapabilities(currentUserRole))
                .createdAt(space.getCreatedAt())
                .updatedAt(space.getUpdatedAt())
                .build();
    }

    private SpaceMemberResponse toSpaceMemberResponse(SpaceMember member) {
        boolean active = "ACTIVE".equalsIgnoreCase(member.getUser().getStatus().name());
        boolean eligible = active && (member.getRole() == SpaceRole.ADMIN || member.getRole() == SpaceRole.MANAGER || member.getRole() == SpaceRole.MEMBER);
        return SpaceMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .username(member.getUser().getUsername())
                .fullname(member.getUser().getFullname() != null ? member.getUser().getFullname() : member.getUser().getUsername())
                .role(member.getRole())
                .active(active)
                .eligibleForAssign(eligible)
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
