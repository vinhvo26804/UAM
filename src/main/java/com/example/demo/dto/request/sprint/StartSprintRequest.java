package com.example.demo.dto.request.sprint;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartSprintRequest {

    @NotNull(message = "Version không được để trống")
    private Long version;
}
