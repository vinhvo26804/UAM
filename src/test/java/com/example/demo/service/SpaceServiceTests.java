package com.example.demo.service;

import com.example.demo.dto.request.space.AddSpaceMemberRequest;
import com.example.demo.dto.request.space.CreateSpaceRequest;
import com.example.demo.dto.request.space.UpdateSpaceMemberRoleRequest;
import com.example.demo.dto.response.space.SpaceMemberResponse;
import com.example.demo.dto.response.space.SpaceResponse;
import com.example.demo.entity.Space;
import com.example.demo.entity.SpaceMember;
import com.example.demo.entity.SpaceRole;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.respository.SpaceMemberRepository;
import com.example.demo.respository.SpaceRepository;
import com.example.demo.respository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SpaceServiceTests {

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private SpaceMemberRepository spaceMemberRepository;

    @Mock
    private SpaceAccessService spaceAccessService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SpaceService spaceService;

    private User adminUser;
    private Space testSpace;
    private SpaceMember adminMember;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .Id(1L)
                .username("admin_user")
                .status(UserStatus.ACTIVE)
                .build();

        testSpace = Space.builder()
                .id(100L)
                .key("PROJ")
                .name("Project Alpha")
                .createdBy(adminUser)
                .build();

        adminMember = SpaceMember.builder()
                .id(1L)
                .space(testSpace)
                .user(adminUser)
                .role(SpaceRole.ADMIN)
                .build();
    }

    @Test
    void createSpace_Success() {
        CreateSpaceRequest request = CreateSpaceRequest.builder()
                .key("PROJ")
                .name("Project Alpha")
                .build();

        when(spaceAccessService.getActiveUser("admin_user")).thenReturn(adminUser);
        when(spaceRepository.existsByKey("PROJ")).thenReturn(false);
        when(spaceRepository.save(any(Space.class))).thenReturn(testSpace);
        when(spaceMemberRepository.save(any(SpaceMember.class))).thenReturn(adminMember);

        SpaceResponse response = spaceService.createSpace(request, "admin_user");

        assertNotNull(response);
        assertEquals("PROJ", response.getKey());
        assertEquals(SpaceRole.ADMIN, response.getCurrentUserRole());
        verify(spaceMemberRepository).save(any(SpaceMember.class));
    }

    @Test
    void createSpace_ThrowsConflict_WhenKeyExists() {
        CreateSpaceRequest request = CreateSpaceRequest.builder()
                .key("PROJ")
                .name("Project Alpha")
                .build();

        when(spaceAccessService.getActiveUser("admin_user")).thenReturn(adminUser);
        when(spaceRepository.existsByKey("PROJ")).thenReturn(true);

        IssueOperationException ex = assertThrows(IssueOperationException.class, () ->
                spaceService.createSpace(request, "admin_user")
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("SPACE_KEY_EXISTS", ex.getCode());
    }

    @Test
    void getMySpaces_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(spaceAccessService.getActiveUser("admin_user")).thenReturn(adminUser);
        when(spaceRepository.findSpacesByUserId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(testSpace)));
        when(spaceMemberRepository.findBySpaceIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(adminMember));

        Page<SpaceResponse> result = spaceService.getMySpaces("admin_user", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("PROJ", result.getContent().get(0).getKey());
        assertEquals(SpaceRole.ADMIN, result.getContent().get(0).getCurrentUserRole());
    }

    @Test
    void addMember_Success() {
        User newUser = User.builder().Id(2L).username("new_member").status(UserStatus.ACTIVE).build();
        AddSpaceMemberRequest request = AddSpaceMemberRequest.builder()
                .userId(2L)
                .role(SpaceRole.MEMBER)
                .build();

        when(spaceAccessService.getActiveUser("admin_user")).thenReturn(adminUser);
        when(spaceAccessService.getMembershipOrThrow(100L, 1L)).thenReturn(adminMember);
        when(spaceAccessService.lockSpaceForWrite(100L)).thenReturn(testSpace);
        when(userRepository.findById(2L)).thenReturn(Optional.of(newUser));
        when(spaceMemberRepository.existsBySpaceIdAndUserId(100L, 2L)).thenReturn(false);

        SpaceMember savedMember = SpaceMember.builder()
                .id(2L)
                .space(testSpace)
                .user(newUser)
                .role(SpaceRole.MEMBER)
                .build();
        when(spaceMemberRepository.save(any(SpaceMember.class))).thenReturn(savedMember);

        SpaceMemberResponse response = spaceService.addMember(100L, request, "admin_user");

        assertNotNull(response);
        assertEquals(2L, response.getUserId());
        assertEquals(SpaceRole.MEMBER, response.getRole());
    }

    @Test
    void updateMemberRole_ThrowsConflict_WhenDemotingLastAdmin() {
        UpdateSpaceMemberRoleRequest request = UpdateSpaceMemberRoleRequest.builder()
                .role(SpaceRole.MEMBER)
                .build();

        when(spaceAccessService.getActiveUser("admin_user")).thenReturn(adminUser);
        when(spaceAccessService.getMembershipOrThrow(100L, 1L)).thenReturn(adminMember);
        when(spaceAccessService.lockSpaceForWrite(100L)).thenReturn(testSpace);
        when(spaceMemberRepository.findBySpaceIdAndUserId(100L, 1L)).thenReturn(Optional.of(adminMember));
        when(spaceMemberRepository.countBySpaceIdAndRole(100L, SpaceRole.ADMIN)).thenReturn(1L);

        IssueOperationException ex = assertThrows(IssueOperationException.class, () ->
                spaceService.updateMemberRole(100L, 1L, request, "admin_user")
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("LAST_ADMIN", ex.getCode());
    }
}
