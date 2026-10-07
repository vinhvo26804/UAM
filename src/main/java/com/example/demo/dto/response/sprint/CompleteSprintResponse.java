package com.example.demo.dto.response.sprint;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteSprintResponse {

    private Long sourceSprintId;
    private String sourceStatus;
    private Long sourceVersion;
    private Long targetSprintId;
    private long movedIssueCount;
    private long retainedIssueCount;
    private Long spaceRevision;
}
