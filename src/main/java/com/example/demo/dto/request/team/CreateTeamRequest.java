package com.example.demo.dto.request.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateTeamRequest {

    @NotBlank(message = "Tên nhóm không được để trống!")
    @Size(min = 2, max = 100, message = "Tên nhóm phải từ 2 đến 100 ký tự!")
    private String name;

    @Size(max = 500, message = "Mô tả nhóm tối đa 500 ký tự!")
    private String description;
}
