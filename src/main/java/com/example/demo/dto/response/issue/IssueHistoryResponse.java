package com.example.demo.dto.response.issue;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueHistoryResponse {

    private Long id;
    private Long issueId;
    private Long actorId;
    private String actorUsername;
    private String actorDisplayName;
    private String action;
    private String changes;
    private LocalDateTime createdAt;
}
