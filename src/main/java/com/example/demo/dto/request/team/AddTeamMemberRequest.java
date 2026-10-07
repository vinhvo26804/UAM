package com.example.demo.dto.request.team;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddTeamMemberRequest {
    @NotNull(message = "ID người dùng không được để trống")
    private Long userId;
}
