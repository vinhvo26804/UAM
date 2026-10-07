package com.example.demo.service;

import com.example.demo.dto.request.team.AddTeamMemberRequest;
import com.example.demo.dto.request.team.CreateTeamRequest;
import com.example.demo.dto.request.team.UpdateTeamMemberRoleRequest;
import com.example.demo.dto.response.team.CandidateUserResponse;
import com.example.demo.dto.response.team.TeamMemberResponse;
import com.example.demo.dto.response.team.TeamResponse;
import com.example.demo.entity.*;
import com.example.demo.respository.TeamMemberRepository;
import com.example.demo.respository.TeamRepository;
import com.example.demo.respository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public User getActiveUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản chưa được kích hoạt hoặc đã bị khóa");
        }
        return user;
    }

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request, String username) {
        User creator = getActiveUser(username);

        String trimmedName = request.getName() != null ? request.getName().trim() : "";
        if (trimmedName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên nhóm không được để trống!");
        }

        Team team = Team.builder()
                .name(trimmedName)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .build();

        Team savedTeam = teamRepository.save(team);

        TeamMember ownerMember = TeamMember.builder()
                .team(savedTeam)
                .user(creator)
                .role(TeamRole.OWNER)
                .build();

        teamMemberRepository.save(ownerMember);

        return TeamResponse.builder()
                .id(savedTeam.getId())
                .name(savedTeam.getName())
                .description(savedTeam.getDescription())
                .createdAt(savedTeam.getCreatedAt())
                .updatedAt(savedTeam.getUpdatedAt())
                .currentUserRole(TeamRole.OWNER)
                .memberCount(1)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getMyTeams(String username) {
        User user = getActiveUser(username);
        List<Team> teams = teamRepository.findTeamsByUserId(user.getId());
        List<TeamResponse> result = new ArrayList<>();

        for (Team team : teams) {
            TeamMember membership = teamMemberRepository.findByTeamIdAndUserId(team.getId(), user.getId())
                    .orElse(null);
            TeamRole role = membership != null ? membership.getRole() : TeamRole.MEMBER;
            long memberCount = teamMemberRepository.countByTeamId(team.getId());

            result.add(TeamResponse.builder()
                    .id(team.getId())
                    .name(team.getName())
                    .description(team.getDescription())
                    .createdAt(team.getCreatedAt())
                    .updatedAt(team.getUpdatedAt())
                    .currentUserRole(role)
                    .memberCount(memberCount)
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeamDetail(Long teamId, String username) {
        User user = getActiveUser(username);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm làm việc"));

        TeamMember membership = teamMemberRepository.findByTeamIdAndUserId(teamId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập nhóm này"));

        long memberCount = teamMemberRepository.countByTeamId(teamId);

        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .currentUserRole(membership.getRole())
                .memberCount(memberCount)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<TeamMemberResponse> getTeamMembers(Long teamId, Pageable pageable, String username) {
        User user = getActiveUser(username);
        if (!teamRepository.existsById(teamId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm làm việc");
        }

        boolean isMember = teamMemberRepository.existsByTeamIdAndUserId(teamId, user.getId());
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem thành viên của nhóm này");
        }

        Page<TeamMember> membersPage = teamMemberRepository.findByTeamId(teamId, pageable);

        return membersPage.map(tm -> TeamMemberResponse.builder()
                .id(tm.getId())
                .teamId(teamId)
                .userId(tm.getUser().getId())
                .username(tm.getUser().getUsername())
                .email(tm.getUser().getEmail())
                .fullname(tm.getUser().getFullname())
                .role(tm.getRole())
                .joinedAt(tm.getJoinedAt())
                .build());
    }

    @Transactional(readOnly = true)
    public List<CandidateUserResponse> searchCandidates(Long teamId, String query, String username) {
        User user = getActiveUser(username);
        if (!teamRepository.existsById(teamId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm làm việc");
        }

        TeamMember callerMembership = teamMemberRepository.findByTeamIdAndUserId(teamId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền tìm kiếm thành viên cho nhóm này"));

        if (callerMembership.getRole() != TeamRole.OWNER && callerMembership.getRole() != TeamRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Chủ sở hữu hoặc Quản trị viên nhóm mới có quyền thêm thành viên");
        }

        String trimmedQuery = query != null ? query.trim() : null;
        List<User> candidates = userRepository.findCandidateUsersForTeam(teamId, trimmedQuery, PageRequest.of(0, 10));

        return candidates.stream()
                .map(u -> CandidateUserResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .email(u.getEmail())
                        .fullname(u.getFullname())
                        .build())
                .toList();
    }

    @Transactional
    public TeamMemberResponse addMember(Long teamId, AddTeamMemberRequest request, String username) {
        User caller = getActiveUser(username);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm làm việc"));

        TeamMember callerMembership = teamMemberRepository.findByTeamIdAndUserId(teamId, caller.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"));

        if (callerMembership.getRole() != TeamRole.OWNER && callerMembership.getRole() != TeamRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Chủ sở hữu hoặc Quản trị viên nhóm mới có quyền thêm thành viên");
        }

        if (request.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID người dùng không được để trống");
        }

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng cần thêm"));

        if (targetUser.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể thêm người dùng có tài khoản đang hoạt động (ACTIVE)");
        }

        if (teamMemberRepository.existsByTeamIdAndUserId(teamId, targetUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Người dùng đã là thành viên của nhóm");
        }

        TeamMember newMember = TeamMember.builder()
                .team(team)
                .user(targetUser)
                .role(TeamRole.MEMBER)
                .build();

        TeamMember savedMember = teamMemberRepository.save(newMember);

        return TeamMemberResponse.builder()
                .id(savedMember.getId())
                .teamId(teamId)
                .userId(targetUser.getId())
                .username(targetUser.getUsername())
                .email(targetUser.getEmail())
                .fullname(targetUser.getFullname())
                .role(savedMember.getRole())
                .joinedAt(savedMember.getJoinedAt())
                .build();
    }

    @Transactional
    public TeamMemberResponse updateMemberRole(Long teamId, Long targetUserId, UpdateTeamMemberRoleRequest request, String username) {
        User caller = getActiveUser(username);
        if (!teamRepository.existsById(teamId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm làm việc");
        }

        TeamMember callerMembership = teamMemberRepository.findByTeamIdAndUserId(teamId, caller.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"));

        if (callerMembership.getRole() != TeamRole.OWNER && callerMembership.getRole() != TeamRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Chủ sở hữu hoặc Quản trị viên nhóm mới có quyền thay đổi vai trò");
        }

        if (request.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vai trò mới không được để trống");
        }

        if (request.getRole() == TeamRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể chuyển quyền Chủ sở hữu qua chức năng này");
        }

        TeamMember targetMembership = teamMemberRepository.findByTeamIdAndUserId(teamId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thành viên không tồn tại trong nhóm"));

        if (targetMembership.getRole() == TeamRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể thay đổi vai trò của Chủ sở hữu nhóm");
        }

        if (callerMembership.getRole() == TeamRole.ADMIN && targetMembership.getRole() == TeamRole.ADMIN && !caller.getId().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Quản trị viên không thể thay đổi vai trò của Quản trị viên khác");
        }

        targetMembership.setRole(request.getRole());
        TeamMember savedMember = teamMemberRepository.save(targetMembership);

        return TeamMemberResponse.builder()
                .id(savedMember.getId())
                .teamId(teamId)
                .userId(targetMembership.getUser().getId())
                .username(targetMembership.getUser().getUsername())
                .email(targetMembership.getUser().getEmail())
                .fullname(targetMembership.getUser().getFullname())
                .role(savedMember.getRole())
                .joinedAt(savedMember.getJoinedAt())
                .build();
    }

    @Transactional
    public void removeMember(Long teamId, Long targetUserId, String username) {
        User caller = getActiveUser(username);
        if (!teamRepository.existsById(teamId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm làm việc");
        }

        TeamMember callerMembership = teamMemberRepository.findByTeamIdAndUserId(teamId, caller.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"));

        TeamMember targetMembership = teamMemberRepository.findByTeamIdAndUserId(teamId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thành viên không tồn tại trong nhóm"));

        if (targetMembership.getRole() == TeamRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể xóa Chủ sở hữu khỏi nhóm làm việc");
        }

        boolean isSelf = caller.getId().equals(targetUserId);

        if (!isSelf) {
            if (callerMembership.getRole() == TeamRole.MEMBER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xóa thành viên khác khỏi nhóm");
            }
            if (callerMembership.getRole() == TeamRole.ADMIN && targetMembership.getRole() == TeamRole.ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Quản trị viên không có quyền xóa Quản trị viên khác");
            }
        }

        teamMemberRepository.deleteByTeamIdAndUserId(teamId, targetUserId);
    }
}
