package com.example.demo.dto.response.sprint;

import com.example.demo.entity.SprintStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintResponse {

    private Long id;
    private Long spaceId;
    private String name;
    private String goal;
    private SprintStatus status;
    private Long position;
    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long version;
    private Set<String> allowedActions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
