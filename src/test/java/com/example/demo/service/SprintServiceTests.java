package com.example.demo.service;

import com.example.demo.dto.request.sprint.CompleteSprintRequest;
import com.example.demo.dto.request.sprint.CreateSprintRequest;
import com.example.demo.dto.request.sprint.StartSprintRequest;
import com.example.demo.dto.response.sprint.CompleteSprintResponse;
import com.example.demo.dto.response.sprint.SprintResponse;
import com.example.demo.entity.*;
import com.example.demo.respository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SprintServiceTests {

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private IssueSprintHistoryRepository issueSprintHistoryRepository;

    @Mock
    private IssueAuditEventRepository issueAuditEventRepository;

    @Mock
    private SpaceAccessService spaceAccessService;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private SprintService sprintService;

    private User adminUser;
    private Space testSpace;
    private SpaceMember adminMember;
    private Sprint futureSprint;

    @BeforeEach
    void setUp() {
        adminUser = User.builder().Id(1L).username("admin_user").status(UserStatus.ACTIVE).build();
        testSpace = Space.builder().id(100L).key("PROJ").workRevision(0L).build();
        adminMember = SpaceMember.builder().id(1L).space(testSpace).user(adminUser).role(SpaceRole.ADMIN).build();

        futureSprint = Sprint.builder()
                .id(20L)
                .space(testSpace)
                .name("Sprint 1")
                .status(SprintStatus.FUTURE)
                .position(1L)
                .version(0L)
                .createdBy(adminUser)
                .updatedBy(adminUser)
                .build();
    }

    @Test
    void createSprint_Success() {
        CreateSprintRequest request = CreateSprintRequest.builder()
                .spaceId(100L)
                .name("Sprint 1")
                .build();

        when(spaceAccessService.getActiveUser("admin_user")).thenReturn(adminUser);
        when(spaceAccessService.getMembershipOrThrow(100L, 1L)).thenReturn(adminMember);
        when(spaceAccessService.lockSpaceForWrite(100L)).thenReturn(testSpace);
        when(sprintRepository.findMaxPositionBySpaceId(100L)).thenReturn(null);
        when(sprintRepository.save(any(Sprint.class))).thenReturn(futureSprint);

        SprintResponse response = sprintService.createSprint(request, "admin_user");

        assertNotNull(response);
        assertEquals("Sprint 1", response.getName());
        assertEquals(SprintStatus.FUTURE, response.getStatus());
        verify(sprintRepository).save(any(Sprint.class));
    }

    @Test
    void startSprint_ThrowsConflict_WhenActiveSprintAlreadyExists() {
        StartSprintRequest request = StartSprintRequest.builder()
                .version(0L)
                .build();

        when(spaceAccessService.getActiveUser("admin_user")).thenReturn(adminUser);
        when(sprintRepository.findById(20L)).thenReturn(Optional.of(futureSprint));
        when(spaceAccessService.getMembershipOrThrow(100L, 1L)).thenReturn(adminMember);
        when(spaceAccessService.lockSpaceForWrite(100L)).thenReturn(testSpace);
        when(sprintRepository.existsBySpaceIdAndStatus(100L, SprintStatus.ACTIVE)).thenReturn(true);

        IssueOperationException ex = assertThrows(IssueOperationException.class, () ->
                sprintService.startSprint(20L, request, "admin_user")
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("SPRINT_STATE_CONFLICT", ex.getCode());
    }

    @Test
    void completeSprint_Success_MovesIncompleteIssuesAndTheirDoneSubtasks() {
        Sprint activeSprint = Sprint.builder()
                .id(20L)
                .space(testSpace)
                .name("Sprint 1")
                .status(SprintStatus.ACTIVE)
                .position(1L)
                .version(0L)
                .build();

        Sprint targetFutureSprint = Sprint.builder()
                .id(21L)
                .space(testSpace)
                .name("Sprint 2")
                .status(SprintStatus.FUTURE)
                .position(2L)
                .version(0L)
                .build();

        Issue openStory = Issue.builder()
                .id(30L)
                .space(testSpace)
                .issueKey("PROJ-10")
                .issueType(IssueType.STORY)
                .status(IssueStatus.IN_PROGRESS)
                .sprint(activeSprint)
                .version(0L)
                .build();

        Issue doneSubtaskOfOpenStory = Issue.builder()
                .id(31L)
                .space(testSpace)
                .issueKey("PROJ-11")
                .issueType(IssueType.SUBTASK)
                .status(IssueStatus.DONE)
                .parentIssue(openStory)
                .sprint(activeSprint)
                .version(0L)
                .build();

        CompleteSprintRequest request = CompleteSprintRequest.builder()
                .version(0L)
                .spaceRevision(0L)
                .targetSprintId(21L)
                .build();

        when(spaceAccessService.getActiveUser("admin_user")).thenReturn(adminUser);
        when(sprintRepository.findById(20L)).thenReturn(Optional.of(activeSprint));
        when(spaceAccessService.getMembershipOrThrow(100L, 1L)).thenReturn(adminMember);
        when(spaceAccessService.lockSpaceForWrite(100L)).thenReturn(testSpace);
        when(idempotencyService.computeHash(anyString())).thenReturn("hash_comp");
        when(issueRepository.findRootIssuesBySprintId(20L)).thenReturn(List.of(openStory));
        when(issueRepository.findSubtasksByParentId(30L)).thenReturn(List.of(doneSubtaskOfOpenStory));
        when(sprintRepository.findById(21L)).thenReturn(Optional.of(targetFutureSprint));
        when(sprintRepository.save(any(Sprint.class))).thenReturn(activeSprint);

        CompleteSprintResponse response = sprintService.completeSprint(20L, request, null, "admin_user");

        assertNotNull(response);
        assertEquals("CLOSED", response.getSourceStatus());
        assertEquals(2L, response.getMovedIssueCount()); // Both open parent AND its done subtask moved
        assertEquals(21L, response.getTargetSprintId());
        assertEquals(targetFutureSprint, openStory.getSprint());
        assertEquals(targetFutureSprint, doneSubtaskOfOpenStory.getSprint());
    }
}
