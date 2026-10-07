package com.example.demo.dto.request.issue;

import com.example.demo.entity.IssuePriority;
import com.example.demo.entity.IssueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateIssueRequest {

    @NotNull(message = "Space ID không được để trống")
    private Long spaceId;

    @NotNull(message = "Loại công việc không được để trống")
    private IssueType type;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề không vượt quá 255 ký tự")
    private String summary;

    @Size(max = 20000, message = "Mô tả không vượt quá 20000 ký tự")
    private String description;

    private Long reporterId;
    private Long assigneeId;
    private IssuePriority priority;
    private Long epicId;
    private Long parentIssueId;
    private Long sprintId;
    private BigDecimal estimateHours;
    private LocalDate dueDate;
    private Set<String> labels;
}
