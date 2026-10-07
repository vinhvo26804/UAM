package com.example.demo.dto.response.space;

import com.example.demo.entity.SpaceRole;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceMemberResponse {

    private Long id;
    private Long userId;
    private String username;
    private String fullname;
    private SpaceRole role;
    private boolean active;
    private boolean eligibleForAssign;
    private LocalDateTime joinedAt;
}
