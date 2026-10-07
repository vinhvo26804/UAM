package com.example.demo.service;

import com.example.demo.entity.Space;
import com.example.demo.entity.SpaceMember;
import com.example.demo.entity.SpaceRole;
import com.example.demo.entity.User;
import com.example.demo.respository.SpaceMemberRepository;
import com.example.demo.respository.SpaceRepository;
import com.example.demo.respository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SpaceAccessService {

    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final SpaceMemberRepository spaceMemberRepository;

    @Transactional(readOnly = true)
    public User getActiveUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IssueOperationException(HttpStatus
                        .UNAUTHORIZED, "UNAUTHORIZED", "Người dùng không tồn tại"));
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus().name())) {
            throw new IssueOperationException(HttpStatus.FORBIDDEN, "USER_INACTIVE", "Tài khoản người dùng đang bị khóa hoặc chưa kích hoạt");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public Space getSpaceOrThrow(Long spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(() -> new IssueOperationException(HttpStatus
                        .NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy Không gian làm việc"));
    }

    public Space lockSpaceForWrite(Long spaceId) {
        Space space = spaceRepository.findByIdForUpdate(spaceId)
                .orElseThrow(() -> new IssueOperationException(HttpStatus
                        .NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy Không gian làm việc"));
        if (space.isArchived()) {
            throw new IssueOperationException(HttpStatus
                    .FORBIDDEN, "SPACE_ARCHIVED", "Không gian làm việc đã lưu trữ, không thể thực hiện thao tác ghi");
        }
        return space;
    }

    @Transactional(readOnly = true)
    public SpaceMember getMembershipOrThrow(Long spaceId, Long userId) {
        return spaceMemberRepository.findBySpaceIdAndUserId(spaceId, userId)
                .orElseThrow(() -> new IssueOperationException(HttpStatus
                        .NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy quyền truy cập Không gian làm việc"));
    }

    @Transactional(readOnly = true)
    public Optional<SpaceMember> findMembership(Long spaceId, Long userId) {
        return spaceMemberRepository.findBySpaceIdAndUserId(spaceId, userId);
    }

    public void requireRole(SpaceMember member, SpaceRole... allowedRoles) {
        for (SpaceRole r : allowedRoles) {
            if (member.getRole() == r) {
                return;
            }
        }
        throw new IssueOperationException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này trong Không gian");
    }

    public Set<String> getSpaceCapabilities(SpaceRole role) {
        Set<String> caps = new HashSet<>();
        if (role == null) return caps;
        caps.add("VIEW");
        if (role == SpaceRole.VIEWER) {
            return caps;
        }
        caps.add("CREATE_ISSUE");
        caps.add("EDIT_ISSUE");
        caps.add("ASSIGN");
        caps.add("TRANSITION");
        if (role == SpaceRole.MANAGER || role == SpaceRole.ADMIN) {
            caps.add("CANCEL_ISSUE");
            caps.add("REOPEN_ISSUE");
            caps.add("SCHEDULE_ISSUE");
            caps.add("DELETE_ISSUE");
            caps.add("MANAGE_SPRINT");
        }
        if (role == SpaceRole.ADMIN) {
            caps.add("CHANGE_REPORTER");
            caps.add("MANAGE_SPACE_MEMBERS");
        }
        return caps;
    }
}
