package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "issue_sprint_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueSprintHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    private Sprint sprint;

    @Column(name = "entered_at", nullable = false, updatable = false)
    private LocalDateTime enteredAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entered_by", nullable = false)
    private User enteredBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "left_by")
    private User leftBy;

    @Column(name = "enter_reason", nullable = false, length = 40)
    private String enterReason;

    @Column(name = "leave_reason", length = 40)
    private String leaveReason;

    @PrePersist
    protected void onCreate() {
        if (this.enteredAt == null) {
            this.enteredAt = LocalDateTime.now();
        }
    }
}
