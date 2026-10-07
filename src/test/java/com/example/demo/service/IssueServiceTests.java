package com.example.demo.service;

import com.example.demo.dto.request.issue.ChangeIssueStatusRequest;
import com.example.demo.dto.request.issue.CreateIssueRequest;
import com.example.demo.dto.request.issue.UpdateIssueRequest;
import com.example.demo.dto.response.issue.IssueResponse;
import com.example.demo.entity.*;
import com.example.demo.respository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IssueServiceTests {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private SpaceMemberRepository spaceMemberRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private IssueSprintHistoryRepository issueSprintHistoryRepository;

    @Mock
    private IssueAuditEventRepository issueAuditEventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SpaceAccessService spaceAccessService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private IssueService issueService;

    private User adminUser;
    private Space testSpace;
    private SpaceMember adminMember;
    private Issue parentStory;

    @BeforeEach
    void setUp() {
        adminUser = User.builder().Id(1L).username("admin_user").status(UserStatus.ACTIVE).build();
        testSpace = Space.builder().id(100L).key("PROJ").nextIssueNumber(1L).workRevision(0L).build();
        adminMember = SpaceMember.builder().id(1L).space(testSpace).user(adminUser).role(SpaceRole.ADMIN).build();

        parentStory = Issue.builder()
                .id(10L)
                .space(testSpace)
                .issueNumber(1L)
                .issueKey("PROJ-1")
                .issueType(IssueType.STORY)
                .summary("Story 1")
                .status(IssueStatus.TODO)
                .reporter(adminUser)
                .createdBy(adminUser)
                .updatedBy(adminUser)
                .version(0L)
                .build();
    }

    @Test
    void testExactFrontendPayload() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        
        String jsonPriority = "{" +
                "\"version\":0," +
                "\"summary\":null," +
                "\"description\":null," +
                "\"reporterId\":null," +
                "\"assigneeId\":null," +
                "\"priority\":\"HIGH\"," +
                "\"epicId\":null," +
                "\"sprintId\":null," +
                "\"estimateHours\":null," +
                "\"dueDate\":null," +
                "\"labels\":null," +
                "\"summaryPresent\":false," +
                "\"descriptionPresent\":false," +
                "\"reporterIdPresent\":false," +
                "\"assigneeIdPresent\":false," +
                "\"priorityPresent\":true," +
                "\"epicIdPresent\":false," +
                "\"sprintIdPresent\":false," +
                "\"estimateHoursPresent\":false," +
                "\"dueDatePresent\":false," +
                "\"labelsPresent\":false" +
                "}";

        UpdateIssueRequest req1 = mapper.readValue(jsonPriority, UpdateIssueRequest.class);
        assertEquals("HIGH", req1.getPriority().name());
        assertTrue(req1.isPriorityPresent());
        assertFalse(req1.isSummaryPresent());
        assertFalse(req1.isAssigneeIdPresent());
        assertFalse(req1.isSprintIdPresent());

        String jsonUnassign = "{" +
                "\"version\":0," +
                "\"summary\":null," +
                "\"description\":null," +
                "\"reporterId\":null," +
                "\"assigneeId\":null," +
                "\"priority\":null," +
                "\"epicId\":null," +
                "\"sprintId\":null," +
                "\"estimateHours\":null," +
                "\"dueDate\":null," +
                "\"labels\":null," +
                "\"summaryPresent\":false," +
                "\"descriptionPresent\":false," +
                "\"reporterIdPresent\":false," +
                "\"assigneeIdPresent\":true," +
                "\"priorityPresent\":false," +
                "\"epicIdPresent\":false," +
                "\"sprintIdPresent\":false," +
                "\"estimateHoursPresent\":false," +
                "\"dueDatePresent\":false," +
                "\"labelsPresent\":false" +
                "}";

        UpdateIssueRequest req2 = mapper.readValue(jsonUnassign, UpdateIssueRequest.class);
        assertNull(req2.getAssigneeId());
        assertTrue(req2.isAssigneeIdPresent());
        assertFalse(req2.isPriorityPresent());

        String jsonBacklog = "{" +
                "\"version\":0," +
                "\"summary\":null," +
                "\"description\":null," +
                "\"reporterId\":null," +
                "\"assigneeId\":null," +
                "\"priority\":null," +
                "\"epicId\":null," +
                "\"sprintId\":null," +
                "\"estimateHours\":null," +
                "\"dueDate\":null," +
                "\"labels\":null," +
                "\"summaryPresent\":false," +
                "\"descriptionPresent\":false," +
                "\"reporterIdPresent\":false," +
                "\"assigneeIdPresent\":false," +
                "\"priorityPresent\":false," +
                "\"epicIdPresent\":false," +
                "\"sprintIdPresent\":true," +
                "\"estimateHoursPresent\":false," +
                "\"dueDatePresent\":false," +
                "\"labelsPresent\":false" +
                "}";

        UpdateIssueRequest req3 = mapper.readValue(jsonBacklog, UpdateIssueRequest.class);
        assertNull(req3.getSprintId());
        assertTrue(req3.isSprintIdPresent());
        assertFalse(req3.isPriorityPresent());
    }

    @Test
    void createIssue_Epic_Success() {
        CreateIssueRequest request = CreateIssueRequest.builder()
                .spaceId(100L)
                .type(IssueType.EPIC)
                .summary("Epic 1")
                .build();

        when(spaceAccessService.getActiveUser("admin_user")).thenReturn(adminUser);
        when(spaceAccessService.getMembershipOrThrow(100L, 1L)).thenReturn(adminMember);
        when(spaceAccessService.lockSpaceForWrite(100L)).thenReturn(testSpace);
        when(idempotencyService.computeHash(anyString())).thenReturn("hash123");

        Issue savedEpic = Issue.builder()
                .id(5L)
                .space(testSpace)
                .issueNumber(1L)
                .issueKey("PROJ-1")
                .issueType(IssueType.EPIC)
                .summary("Epic 1")
                .status(IssueStatus.TODO)
                .reporter(adminUser)
                .createdBy(adminUser)
                .updatedBy(adminUser)
                .version(0L)
                .build();

        when(issueRepository.save(any(Issue.class))).thenReturn(savedEpic);

        IssueResponse response = issueService.createIssue(request, null, "admin_user");

        assertNotNull(response);
        assertEquals("PROJ-1", response.getKey());
        assertEquals(IssueType.EPIC, response.getType());
        verify(issueRepository).save(any(Issue.class));
    }

    @Test
    void createIssue_Epic_ThrowsBadRequest_WhenHasParentOrSprint() {
        CreateIssueRequest request = CreateIssueRequest.builder()
                .spaceId(100L)
                .type(IssueType.EPIC)
                .summary("Epic Invalid")
                .parentIssueId(10L)
                .build();

        when(spaceAccessService.getActiveUser("admin_user")).thenReturn(adminUser);
        when(spaceAccessService.getMembershipOrThrow(100L, 1L)).thenReturn(adminMember);
        when(spaceAccessService.lockSpaceForWrite(100L)).thenReturn(testSpace);
        when(idempotencyService.computeHash(anyString())).thenReturn("hash123");

        IssueOperationException ex = assertThrows(IssueOperationException.class, () ->
                issueService.createIssue(request, null, "admin_user")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_FIELD", ex.getCode());
    }

    @Test
    void createIssue_Subtask_ThrowsConflict_WhenParentIsDone() {
        parentStory.setStatus(IssueStatus.DONE);

        CreateIssueRequest request = CreateIssueRequest.builder()
                .spaceId(100L)
                .type(IssueType.SUBTASK)
                .summary("Subtask 1")
                .parentIssueId(10L)
                .build();

        when(spaceAccessService.getActiveUser("admin_user")).thenReturn(adminUser);
        when(spaceAccessService.getMembershipOrThrow(100L, 1L)).thenReturn(adminMember);
        when(spaceAccessService.lockSpaceForWrite(100L)).thenReturn(testSpace);
        when(idempotencyService.computeHash(anyString())).thenReturn("hash123");
        when(issueRepository.findById(10L)).thenReturn(Optional.of(parentStory));

        IssueOperationException ex = assertThrows(IssueOperationException.class, () ->
                issueService.createIssue(request, null, "admin_user")
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("PARENT_DONE", ex.getCode());
    }

    @Test
    void transitionStatus_ParentToDone_ThrowsConflict_WhenSubtasksAreOpen() {
        ChangeIssueStatusRequest request = ChangeIssueStatusRequest.builder()
                .targetStatus(IssueStatus.DONE)
                .version(0L)
                .build();

        Issue openSubtask = Issue.builder()
                .id(11L)
                .issueKey("PROJ-2")
                .issueType(IssueType.SUBTASK)
                .status(IssueStatus.IN_PROGRESS)
                .build();

        when(spaceAccessService.getActiveUser("admin_user")).thenReturn(adminUser);
        when(issueRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(parentStory));
        when(spaceAccessService.getMembershipOrThrow(100L, 1L)).thenReturn(adminMember);
        when(spaceAccessService.lockSpaceForWrite(100L)).thenReturn(testSpace);
        when(issueRepository.findSubtasksByParentId(10L)).thenReturn(List.of(openSubtask));

        IssueOperationException ex = assertThrows(IssueOperationException.class, () ->
                issueService.transitionStatus(10L, request, "admin_user")
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("OPEN_SUBTASKS", ex.getCode());
    }

    @Test
    void transitionStatus_SubtaskReopen_ThrowsConflict_WhenParentIsDone() {
        parentStory.setStatus(IssueStatus.DONE);

        Issue doneSubtask = Issue.builder()
                .id(11L)
                .space(testSpace)
                .issueKey("PROJ-2")
                .issueType(IssueType.SUBTASK)
                .status(IssueStatus.DONE)
                .parentIssue(parentStory)
                .version(0L)
                .build();

        ChangeIssueStatusRequest request = ChangeIssueStatusRequest.builder()
                .targetStatus(IssueStatus.IN_PROGRESS)
                .version(0L)
                .build();

        when(spaceAccessService.getActiveUser("admin_user")).thenReturn(adminUser);
        when(issueRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(doneSubtask));
        when(spaceAccessService.getMembershipOrThrow(100L, 1L)).thenReturn(adminMember);
        when(spaceAccessService.lockSpaceForWrite(100L)).thenReturn(testSpace);

        IssueOperationException ex = assertThrows(IssueOperationException.class, () ->
                issueService.transitionStatus(11L, request, "admin_user")
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("PARENT_DONE", ex.getCode());
    }
}
