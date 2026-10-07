package com.example.demo.dto.response.issue;

import com.example.demo.entity.IssuePriority;
import com.example.demo.entity.IssueStatus;
import com.example.demo.entity.IssueType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueSummaryResponse {

    private Long id;
    private String key;
    private Long spaceId;
    private IssueType type;
    private String summary;
    private IssueStatus status;
    private String statusCategory;
    private IssuePriority priority;
    private IssueResponse.UserReference assignee;
    private Boolean assigneeEligible;
    private Long epicId;
    private String epicKey;
    private Long inheritedEpicId;
    private Long parentIssueId;
    private String parentIssueKey;
    private Long sprintId;
    private String sprintName;
    private BigDecimal estimateHours;
    private LocalDate dueDate;
    private Long version;
    private LocalDateTime createdAt;
    private Set<String> allowedActions;
}
