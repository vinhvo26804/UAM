package com.example.demo.dto.request.issue;

import com.example.demo.entity.IssuePriority;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIssueRequest {

    @NotNull(message = "Version không được để trống")
    private Long version;

    private String summary;
    private String description;
    private Long reporterId;
    private Long assigneeId;
    private IssuePriority priority;
    private Long epicId;
    private Long sprintId;
    private BigDecimal estimateHours;
    private LocalDate dueDate;
    private Set<String> labels;

    // Presence tracking flags to distinguish between "omitted" and "explicitly set to null"
    @Builder.Default
    private boolean summaryPresent = false;
    @Builder.Default
    private boolean descriptionPresent = false;
    @Builder.Default
    private boolean reporterIdPresent = false;
    @Builder.Default
    private boolean assigneeIdPresent = false;
    @Builder.Default
    private boolean priorityPresent = false;
    @Builder.Default
    private boolean epicIdPresent = false;
    @Builder.Default
    private boolean sprintIdPresent = false;
    @Builder.Default
    private boolean estimateHoursPresent = false;
    @Builder.Default
    private boolean dueDatePresent = false;
    @Builder.Default
    private boolean labelsPresent = false;

    public void setSummary(String summary) {
        this.summary = summary;
        this.summaryPresent = true;
    }

    public void setDescription(String description) {
        this.description = description;
        this.descriptionPresent = true;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
        this.reporterIdPresent = true;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
        this.assigneeIdPresent = true;
    }

    public void setPriority(IssuePriority priority) {
        this.priority = priority;
        this.priorityPresent = true;
    }

    public void setEpicId(Long epicId) {
        this.epicId = epicId;
        this.epicIdPresent = true;
    }

    public void setSprintId(Long sprintId) {
        this.sprintId = sprintId;
        this.sprintIdPresent = true;
    }

    public void setEstimateHours(BigDecimal estimateHours) {
        this.estimateHours = estimateHours;
        this.estimateHoursPresent = true;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
        this.dueDatePresent = true;
    }

    public void setLabels(Set<String> labels) {
        this.labels = labels;
        this.labelsPresent = true;
    }
}
