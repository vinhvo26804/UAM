package com.example.demo.dto.response.space;

import com.example.demo.entity.IssueType;
import com.example.demo.entity.SpaceRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceResponse {

    private Long id;
    private String key;
    private String name;
    private boolean archived;
    private Long workRevision;
    private Set<IssueType> allowedIssueTypes;
    private SpaceRole currentUserRole;
    private Set<String> capabilities;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
