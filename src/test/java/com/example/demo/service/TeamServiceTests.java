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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTests {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TeamService teamService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .Id(1L)
                .username("testuser")
                .email("testuser@example.com")
                .fullname("Nguyễn Văn A")
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void createTeam_Success_CreatesTeamAndOwner() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));

        Team savedTeam = Team.builder()
                .id(10L)
                .name("Nhóm Phát Triển Phần Mềm")
                .description("Mô tả nhóm")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(teamRepository.save(any(Team.class))).thenReturn(savedTeam);

        CreateTeamRequest request = new CreateTeamRequest("  Nhóm Phát Triển Phần Mềm  ", "  Mô tả nhóm  ");

        TeamResponse response = teamService.createTeam(request, "testuser");

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Nhóm Phát Triển Phần Mềm", response.getName());
        assertEquals("Mô tả nhóm", response.getDescription());
        assertEquals(TeamRole.OWNER, response.getCurrentUserRole());
        assertEquals(1, response.getMemberCount());

        ArgumentCaptor<TeamMember> memberCaptor = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamMemberRepository).save(memberCaptor.capture());
        TeamMember capturedMember = memberCaptor.getValue();
        assertEquals(TeamRole.OWNER, capturedMember.getRole());
        assertEquals(activeUser, capturedMember.getUser());
        assertEquals(savedTeam, capturedMember.getTeam());
    }

    @Test
    void createTeam_BlankName_ThrowsBadRequest() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));

        CreateTeamRequest request = new CreateTeamRequest("   ", "Mô tả");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.createTeam(request, "testuser"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(teamRepository);
        verifyNoInteractions(teamMemberRepository);
    }

    @Test
    void createTeam_UserNotFound_ThrowsUnauthorized() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        CreateTeamRequest request = new CreateTeamRequest("Nhóm Alpha", "Mô tả");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.createTeam(request, "unknown"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(teamRepository);
        verifyNoInteractions(teamMemberRepository);
    }

    @Test
    void createTeam_UserNotActive_ThrowsForbidden() {
        activeUser.setStatus(UserStatus.LOCKED);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));

        CreateTeamRequest request = new CreateTeamRequest("Nhóm Alpha", "Mô tả");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.createTeam(request, "testuser"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(teamRepository);
        verifyNoInteractions(teamMemberRepository);
    }

    @Test
    void getMyTeams_ReturnsOnlyTeamsOfUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));

        Team team1 = Team.builder().id(1L).name("Team 1").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(teamRepository.findTeamsByUserId(1L)).thenReturn(List.of(team1));

        TeamMember tm = TeamMember.builder().team(team1).user(activeUser).role(TeamRole.OWNER).build();
        when(teamMemberRepository.findByTeamIdAndUserId(1L, 1L)).thenReturn(Optional.of(tm));
        when(teamMemberRepository.countByTeamId(1L)).thenReturn(3L);

        List<TeamResponse> teams = teamService.getMyTeams("testuser");

        assertEquals(1, teams.size());
        assertEquals("Team 1", teams.get(0).getName());
        assertEquals(TeamRole.OWNER, teams.get(0).getCurrentUserRole());
        assertEquals(3L, teams.get(0).getMemberCount());
    }

    @Test
    void getTeamDetail_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        Team team = Team.builder().id(10L).name("Team 10").build();
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        TeamMember tm = TeamMember.builder().team(team).user(activeUser).role(TeamRole.ADMIN).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(tm));
        when(teamMemberRepository.countByTeamId(10L)).thenReturn(5L);

        TeamResponse detail = teamService.getTeamDetail(10L, "testuser");
        assertEquals(10L, detail.getId());
        assertEquals("Team 10", detail.getName());
        assertEquals(TeamRole.ADMIN, detail.getCurrentUserRole());
        assertEquals(5L, detail.getMemberCount());
    }

    @Test
    void getTeamDetail_NonMember_ThrowsForbidden() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        Team team = Team.builder().id(10L).name("Team 10").build();
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.getTeamDetail(10L, "testuser"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void getTeamDetail_NotFound_ThrowsNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.getTeamDetail(99L, "testuser"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getTeamMembers_SuccessWithPagination() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        when(teamRepository.existsById(10L)).thenReturn(true);
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(true);

        Team team = Team.builder().id(10L).build();
        TeamMember tm = TeamMember.builder()
                .id(100L)
                .team(team)
                .user(activeUser)
                .role(TeamRole.OWNER)
                .joinedAt(LocalDateTime.now())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<TeamMember> memberPage = new PageImpl<>(List.of(tm), pageable, 1);
        when(teamMemberRepository.findByTeamId(10L, pageable)).thenReturn(memberPage);

        Page<TeamMemberResponse> responses = teamService.getTeamMembers(10L, pageable, "testuser");
        assertEquals(1, responses.getTotalElements());
        assertEquals("testuser", responses.getContent().get(0).getUsername());
        assertEquals(TeamRole.OWNER, responses.getContent().get(0).getRole());
    }

    @Test
    void getTeamMembers_NonMember_ThrowsForbidden() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        when(teamRepository.existsById(10L)).thenReturn(true);
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(false);

        Pageable pageable = PageRequest.of(0, 10);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.getTeamMembers(10L, pageable, "testuser"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void searchCandidates_SuccessForOwnerOrAdmin() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        when(teamRepository.existsById(10L)).thenReturn(true);

        TeamMember callerMembership = TeamMember.builder().role(TeamRole.OWNER).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(callerMembership));

        User candidate = User.builder().Id(2L).username("bob").email("bob@example.com").status(UserStatus.ACTIVE).build();
        when(userRepository.findCandidateUsersForTeam(eq(10L), eq("bob"), any())).thenReturn(List.of(candidate));

        List<CandidateUserResponse> result = teamService.searchCandidates(10L, "bob", "testuser");
        assertEquals(1, result.size());
        assertEquals("bob", result.get(0).getUsername());
    }

    @Test
    void searchCandidates_MemberRole_ThrowsForbidden() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        when(teamRepository.existsById(10L)).thenReturn(true);

        TeamMember callerMembership = TeamMember.builder().role(TeamRole.MEMBER).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(callerMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.searchCandidates(10L, "bob", "testuser"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void addMember_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        Team team = Team.builder().id(10L).name("Team 10").build();
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        TeamMember callerMembership = TeamMember.builder().role(TeamRole.ADMIN).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(callerMembership));

        User targetUser = User.builder().Id(2L).username("bob").email("bob@example.com").status(UserStatus.ACTIVE).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 2L)).thenReturn(false);

        TeamMember savedMember = TeamMember.builder()
                .id(20L)
                .team(team)
                .user(targetUser)
                .role(TeamRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();
        when(teamMemberRepository.save(any(TeamMember.class))).thenReturn(savedMember);

        AddTeamMemberRequest request = new AddTeamMemberRequest(2L);
        TeamMemberResponse response = teamService.addMember(10L, request, "testuser");

        assertNotNull(response);
        assertEquals(2L, response.getUserId());
        assertEquals("bob", response.getUsername());
        assertEquals(TeamRole.MEMBER, response.getRole());
    }

    @Test
    void addMember_MemberRole_ThrowsForbidden() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        Team team = Team.builder().id(10L).name("Team 10").build();
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        TeamMember callerMembership = TeamMember.builder().role(TeamRole.MEMBER).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(callerMembership));

        AddTeamMemberRequest request = new AddTeamMemberRequest(2L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.addMember(10L, request, "testuser"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void addMember_AlreadyMember_ThrowsConflict() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        Team team = Team.builder().id(10L).name("Team 10").build();
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        TeamMember callerMembership = TeamMember.builder().role(TeamRole.OWNER).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(callerMembership));

        User targetUser = User.builder().Id(2L).username("bob").email("bob@example.com").status(UserStatus.ACTIVE).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(teamMemberRepository.existsByTeamIdAndUserId(10L, 2L)).thenReturn(true);

        AddTeamMemberRequest request = new AddTeamMemberRequest(2L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.addMember(10L, request, "testuser"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void addMember_UserNotActive_ThrowsBadRequest() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        Team team = Team.builder().id(10L).name("Team 10").build();
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        TeamMember callerMembership = TeamMember.builder().role(TeamRole.OWNER).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(callerMembership));

        User targetUser = User.builder().Id(2L).username("bob").email("bob@example.com").status(UserStatus.LOCKED).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));

        AddTeamMemberRequest request = new AddTeamMemberRequest(2L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.addMember(10L, request, "testuser"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateMemberRole_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        when(teamRepository.existsById(10L)).thenReturn(true);

        TeamMember callerMembership = TeamMember.builder().role(TeamRole.OWNER).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(callerMembership));

        User targetUser = User.builder().Id(2L).username("bob").email("bob@example.com").build();
        TeamMember targetMembership = TeamMember.builder().id(20L).user(targetUser).role(TeamRole.MEMBER).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(targetMembership));
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTeamMemberRoleRequest request = new UpdateTeamMemberRoleRequest(TeamRole.ADMIN);
        TeamMemberResponse response = teamService.updateMemberRole(10L, 2L, request, "testuser");

        assertNotNull(response);
        assertEquals(TeamRole.ADMIN, response.getRole());
        assertEquals("bob", response.getUsername());
    }

    @Test
    void updateMemberRole_ChangeOwnerRole_ThrowsBadRequest() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        when(teamRepository.existsById(10L)).thenReturn(true);

        TeamMember callerMembership = TeamMember.builder().role(TeamRole.OWNER).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(callerMembership));

        User ownerUser = User.builder().Id(1L).username("testuser").build();
        TeamMember targetMembership = TeamMember.builder().id(10L).user(ownerUser).role(TeamRole.OWNER).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(targetMembership));

        UpdateTeamMemberRoleRequest request = new UpdateTeamMemberRoleRequest(TeamRole.MEMBER);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.updateMemberRole(10L, 1L, request, "testuser"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateMemberRole_AssignOwnerRole_ThrowsBadRequest() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        when(teamRepository.existsById(10L)).thenReturn(true);

        TeamMember callerMembership = TeamMember.builder().role(TeamRole.OWNER).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(callerMembership));

        UpdateTeamMemberRoleRequest request = new UpdateTeamMemberRoleRequest(TeamRole.OWNER);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.updateMemberRole(10L, 2L, request, "testuser"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void removeMember_SuccessByOwner() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        when(teamRepository.existsById(10L)).thenReturn(true);

        TeamMember callerMembership = TeamMember.builder().role(TeamRole.OWNER).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(callerMembership));

        User targetUser = User.builder().Id(2L).username("bob").build();
        TeamMember targetMembership = TeamMember.builder().id(20L).user(targetUser).role(TeamRole.MEMBER).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(targetMembership));

        assertDoesNotThrow(() -> teamService.removeMember(10L, 2L, "testuser"));
        verify(teamMemberRepository).deleteByTeamIdAndUserId(10L, 2L);
    }

    @Test
    void removeMember_OwnerCannotBeDeleted_ThrowsBadRequest() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        when(teamRepository.existsById(10L)).thenReturn(true);

        TeamMember callerMembership = TeamMember.builder().role(TeamRole.OWNER).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(callerMembership));

        User ownerUser = User.builder().Id(1L).username("testuser").build();
        TeamMember targetMembership = TeamMember.builder().id(10L).user(ownerUser).role(TeamRole.OWNER).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(targetMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.removeMember(10L, 1L, "testuser"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(teamMemberRepository, never()).deleteByTeamIdAndUserId(any(), any());
    }

    @Test
    void removeMember_AdminCannotDeleteAdmin_ThrowsForbidden() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        when(teamRepository.existsById(10L)).thenReturn(true);

        TeamMember callerMembership = TeamMember.builder().role(TeamRole.ADMIN).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(callerMembership));

        User targetUser = User.builder().Id(2L).username("charlie").build();
        TeamMember targetMembership = TeamMember.builder().id(20L).user(targetUser).role(TeamRole.ADMIN).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(targetMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.removeMember(10L, 2L, "testuser"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(teamMemberRepository, never()).deleteByTeamIdAndUserId(any(), any());
    }

    @Test
    void removeMember_MemberCannotDeleteOther_ThrowsForbidden() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        when(teamRepository.existsById(10L)).thenReturn(true);

        TeamMember callerMembership = TeamMember.builder().role(TeamRole.MEMBER).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(callerMembership));

        User targetUser = User.builder().Id(2L).username("bob").build();
        TeamMember targetMembership = TeamMember.builder().id(20L).user(targetUser).role(TeamRole.MEMBER).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(targetMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.removeMember(10L, 2L, "testuser"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(teamMemberRepository, never()).deleteByTeamIdAndUserId(any(), any());
    }

    @Test
    void removeMember_MemberCanRemoveSelf_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        when(teamRepository.existsById(10L)).thenReturn(true);

        TeamMember callerMembership = TeamMember.builder().role(TeamRole.MEMBER).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(callerMembership));

        TeamMember targetMembership = TeamMember.builder().id(10L).user(activeUser).role(TeamRole.MEMBER).build();
        when(teamMemberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(targetMembership));

        assertDoesNotThrow(() -> teamService.removeMember(10L, 1L, "testuser"));
        verify(teamMemberRepository).deleteByTeamIdAndUserId(10L, 1L);
    }
}
