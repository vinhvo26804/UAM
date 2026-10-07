package com.example.demo.dto.request.space;

import com.example.demo.entity.IssueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSpaceRequest {

    @NotBlank(message = "Mã Không gian không được để trống")
    @Pattern(regexp = "^[A-Z][A-Z0-9]{1,9}$", message = "Mã Không gian phải viết hoa, từ 2 đến 10 ký tự chữ và số (ví dụ: PROJ, UAM)")
    private String key;

    @NotBlank(message = "Tên Không gian không được để trống")
    @Size(max = 100, message = "Tên Không gian không vượt quá 100 ký tự")
    private String name;

    private Set<IssueType> allowedIssueTypes;
}
