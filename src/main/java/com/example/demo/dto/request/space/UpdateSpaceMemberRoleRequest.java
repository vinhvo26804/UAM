package com.example.demo.dto.request.space;

import com.example.demo.entity.SpaceRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSpaceMemberRoleRequest {

    @NotNull(message = "Vai trò mới không được để trống")
    private SpaceRole role;

    private SpaceRole expectedRole;
}
