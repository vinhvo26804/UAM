package com.example.demo.dto.response.sprint;

import com.example.demo.entity.IssueStatus;
import com.example.demo.entity.IssueType;
import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintCompletionPreviewResponse {

    private Long sourceSprintId;
    private Long sourceVersion;
    private Long spaceRevision;
    private Long targetSprintId;
    private Long targetVersion;
    private Long suggestedTargetSprintId;
    private long moveRootCount;
    private long moveIssueCount;
    private long includedDoneSubtaskCount;
    private long stayCount;
    private boolean canComplete;
    private List<String> blockers;
    private Page<PreviewItem> items;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreviewItem {
        private Long id;
        private String key;
        private IssueType type;
        private IssueStatus status;
        private Long parentIssueId;
        private String reason; // OPEN_ROOT, OPEN_PARENT, FOLLOW_PARENT
    }
}
