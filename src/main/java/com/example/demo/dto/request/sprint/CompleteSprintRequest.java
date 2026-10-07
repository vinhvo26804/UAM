package com.example.demo.dto.request.sprint;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteSprintRequest {

    @NotNull(message = "Version không được để trống")
    private Long version;

    @NotNull(message = "Space Revision không được để trống")
    private Long spaceRevision;

    private Long targetSprintId;
    private Long targetVersion;
    private NewSprintDestination newSprint;
}
