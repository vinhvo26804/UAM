package com.example.demo.dto.request.issue;

import com.example.demo.entity.IssueStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeIssueStatusRequest {

    @NotNull(message = "Trạng thái mới không được để trống")
    private IssueStatus targetStatus;

    @NotNull(message = "Version không được để trống")
    private Long version;
}
