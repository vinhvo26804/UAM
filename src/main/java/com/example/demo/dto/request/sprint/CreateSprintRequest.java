package com.example.demo.dto.request.sprint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSprintRequest {

    @NotNull(message = "Space ID không được để trống")
    private Long spaceId;

    @NotBlank(message = "Tên Sprint không được để trống")
    @Size(max = 100, message = "Tên Sprint không vượt quá 100 ký tự")
    private String name;

    @Size(max = 2000, message = "Mục tiêu Sprint không vượt quá 2000 ký tự")
    private String goal;

    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
}
