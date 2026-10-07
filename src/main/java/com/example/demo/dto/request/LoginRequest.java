package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống!")
    private String userName;
    @NotBlank(message = "Mật khẩu không được để trống!")
    private String password;
}
