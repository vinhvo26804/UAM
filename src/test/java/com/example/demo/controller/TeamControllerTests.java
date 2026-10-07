package com.example.demo.controller;

import com.example.demo.dto.request.team.AddTeamMemberRequest;
import com.example.demo.dto.request.team.CreateTeamRequest;
import com.example.demo.dto.request.team.UpdateTeamMemberRoleRequest;
import com.example.demo.dto.response.team.CandidateUserResponse;
import com.example.demo.dto.response.team.TeamMemberResponse;
import com.example.demo.dto.response.team.TeamResponse;
import com.example.demo.entity.TeamRole;
import com.example.demo.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TeamControllerTests {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TeamService teamService;

    @InjectMocks
    private TeamController teamController;

    private Authentication auth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(teamController).build();
        objectMapper = new ObjectMapper();
        auth = new UsernamePasswordAuthenticationToken("alice", null, Collections.emptyList());
    }

    @Test
    void createTeam_ValidInput_ReturnsCreated() throws Exception {
        CreateTeamRequest request = new CreateTeamRequest("Nhóm Thiết Kế", "Mô tả nhóm");

        TeamResponse mockResponse = TeamResponse.builder()
                .id(1L)
                .name("Nhóm Thiết Kế")
                .description("Mô tả nhóm")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .currentUserRole(TeamRole.OWNER)
                .memberCount(1)
                .build();

        when(teamService.createTeam(any(CreateTeamRequest.class), eq("alice"))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/teams")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Nhóm Thiết Kế"))
                .andExpect(jsonPath("$.currentUserRole").value("OWNER"))
                .andExpect(jsonPath("$.memberCount").value(1));
    }

    @Test
    void createTeam_BlankName_ReturnsBadRequest() throws Exception {
        CreateTeamRequest request = new CreateTeamRequest("", "Mô tả nhóm");

        mockMvc.perform(post("/api/v1/teams")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getMyTeams_ReturnsList() throws Exception {
        TeamResponse t1 = TeamResponse.builder()
                .id(1L)
                .name("Team Alpha")
                .currentUserRole(TeamRole.OWNER)
                .memberCount(2)
                .build();

        when(teamService.getMyTeams("alice")).thenReturn(List.of(t1));

        mockMvc.perform(get("/api/v1/teams")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Team Alpha"))
                .andExpect(jsonPath("$[0].currentUserRole").value("OWNER"));
    }

    @Test
    void getTeamDetail_Success() throws Exception {
        TeamResponse detail = TeamResponse.builder()
                .id(5L)
                .name("Team Beta")
                .currentUserRole(TeamRole.ADMIN)
                .memberCount(4)
                .build();

        when(teamService.getTeamDetail(5L, "alice")).thenReturn(detail);

        mockMvc.perform(get("/api/v1/teams/5")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.name").value("Team Beta"))
                .andExpect(jsonPath("$.currentUserRole").value("ADMIN"));
    }

    @Test
    void getTeamDetail_Forbidden_Returns403() throws Exception {
        when(teamService.getTeamDetail(5L, "alice"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập nhóm này"));

        mockMvc.perform(get("/api/v1/teams/5")
                        .principal(auth))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Bạn không có quyền truy cập nhóm này"));
    }

    @Test
    void getTeamMembers_ReturnsPagedMembers() throws Exception {
        TeamMemberResponse m1 = TeamMemberResponse.builder()
                .id(10L)
                .teamId(5L)
                .userId(1L)
                .username("alice")
                .role(TeamRole.OWNER)
                .build();

        when(teamService.getTeamMembers(eq(5L), any(), eq("alice")))
                .thenReturn(new PageImpl<>(List.of(m1), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/teams/5/members")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("alice"))
                .andExpect(jsonPath("$.content[0].role").value("OWNER"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void searchCandidates_Success() throws Exception {
        CandidateUserResponse c1 = CandidateUserResponse.builder()
                .id(2L)
                .username("bob")
                .email("bob@example.com")
                .build();

        when(teamService.searchCandidates(5L, "bob", "alice")).thenReturn(List.of(c1));

        mockMvc.perform(get("/api/v1/teams/5/candidate-users")
                        .param("query", "bob")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2L))
                .andExpect(jsonPath("$[0].username").value("bob"));
    }

    @Test
    void addMember_Success() throws Exception {
        AddTeamMemberRequest request = new AddTeamMemberRequest(2L);

        TeamMemberResponse m1 = TeamMemberResponse.builder()
                .id(20L)
                .teamId(5L)
                .userId(2L)
                .username("bob")
                .role(TeamRole.MEMBER)
                .build();

        when(teamService.addMember(eq(5L), any(AddTeamMemberRequest.class), eq("alice"))).thenReturn(m1);

        mockMvc.perform(post("/api/v1/teams/5/members")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20L))
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void updateMemberRole_Success() throws Exception {
        UpdateTeamMemberRoleRequest request = new UpdateTeamMemberRoleRequest(TeamRole.ADMIN);

        TeamMemberResponse m1 = TeamMemberResponse.builder()
                .id(20L)
                .teamId(5L)
                .userId(2L)
                .username("bob")
                .role(TeamRole.ADMIN)
                .build();

        when(teamService.updateMemberRole(eq(5L), eq(2L), any(UpdateTeamMemberRoleRequest.class), eq("alice"))).thenReturn(m1);

        mockMvc.perform(patch("/api/v1/teams/5/members/2/role")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void removeMember_Success() throws Exception {
        doNothing().when(teamService).removeMember(5L, 2L, "alice");

        mockMvc.perform(delete("/api/v1/teams/5/members/2")
                        .principal(auth))
                .andExpect(status().isNoContent());
    }
}
