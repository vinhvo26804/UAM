package com.example.demo.dto.response.team;

import com.example.demo.entity.TeamRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamMemberResponse {
    private Long id;
    private Long teamId;
    private Long userId;
    private String username;
    private String email;
    private String fullname;
    private TeamRole role;
    private LocalDateTime joinedAt;
}
