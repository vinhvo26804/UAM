package com.example.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;

@Getter
@Setter
public class    RegisterRequest {
    @NotBlank(message ="Tên người dùng Không được để trống!")
    @Size(min =  3, max = 50, message = "Tên người dùng phải  từ 3 đến 50 kí tự")
    private String userName;
    @NotBlank(message ="Mật khẩu Không được để trống!")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 kí tự")
    private String password;
    @NotBlank(message ="Email Không được để trống!")
    @Email(message = "email không đúng định dạng")
    private String email;


}
