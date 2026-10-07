package com.example.demo.dto.request.sprint;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewSprintCompletionRequest {
    private Long targetSprintId;
    private NewSprintDestination newSprint;
}
