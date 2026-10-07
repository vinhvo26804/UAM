package com.example.demo.dto.request.space;

import com.example.demo.entity.SpaceRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddSpaceMemberRequest {

    @NotNull(message = "ID người dùng không được để trống")
    private Long userId;

    @NotNull(message = "Vai trò không được để trống")
    private SpaceRole role;
}
