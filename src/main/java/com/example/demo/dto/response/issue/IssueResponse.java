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
public class IssueResponse {

    private Long id;
    private String key;
    private Long spaceId;
    private IssueType type;
    private String summary;
    private String description;
    private IssueStatus status;
    private String statusCategory;
    private IssuePriority priority;
    private UserReference reporter;
    private UserReference assignee;
    private Boolean assigneeEligible;
    private Long epicId;
    private String epicKey;
    private String epicSummary;
    private Long inheritedEpicId;
    private Long parentIssueId;
    private String parentIssueKey;
    private String parentIssueSummary;
    private Long sprintId;
    private String sprintName;
    private BigDecimal estimateHours;
    private LocalDate dueDate;
    private Set<String> labels;
    private long childCount;
    private Long version;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<String> allowedActions;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserReference {
        private Long id;
        private String username;
        private String displayName;
    }
}
