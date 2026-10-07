package com.example.demo.dto.response.issue;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueSprintHistoryResponse {

    private Long id;
    private Long issueId;
    private Long sprintId;
    private String sprintName;
    private LocalDateTime enteredAt;
    private LocalDateTime leftAt;
    private Long enteredById;
    private String enteredByDisplayName;
    private Long leftById;
    private String leftByDisplayName;
    private String enterReason;
    private String leaveReason;
}
