package com.example.demo.dto.request.team;

import com.example.demo.entity.TeamRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTeamMemberRoleRequest {
    @NotNull(message = "Vai trò mới không được để trống")
    private TeamRole role;
}
