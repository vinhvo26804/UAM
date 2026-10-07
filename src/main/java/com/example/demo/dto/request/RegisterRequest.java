package com.example.demo.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống!")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3 đến 50 kí tự!")
    private String userName;

    @NotBlank(message = "Mật khẩu không được để trống!")
    @Size(min = 8, max = 128, message = "Mật khẩu phải từ 8 đến 128 kí tự")
    private String password;

    @NotBlank(message = "Email không được để trống!")
    @Email(message = "Email không đúng định dạng!")
    @Size(max = 255, message = "Email tối đa 255 kí tự!")
    private String email;

    @Size(max = 20, message = "Số điện thoại tối đa 20 kí tự!")
    @Pattern(regexp = "^(|\\+?[0-9][0-9 ()-]{5,18}[0-9])$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @PastOrPresent(message = "Ngày sinh phải nhỏ hơn hoặc bằng hiện tại")
    private LocalDate doB;

    @JsonProperty("dob")
    public void setDob(LocalDate dob) {
        this.doB = dob;
    }

    public LocalDate getDoB() {
        return this.doB;
    }
    public void setDoB(LocalDate doB) {
        this.doB = doB;
    }
    public LocalDate getDob() {
        return this.doB;
    }
}
