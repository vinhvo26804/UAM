package com.example.demo.dto.request.sprint;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewSprintDestination {
    private String name;
    private String goal;
    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
}
