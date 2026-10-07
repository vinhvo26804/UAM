package com.example.demo.dto.request.User;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class UpdateUserRequest {

    @NotBlank(message = "Họ và tên không được để trống!")
    @Size(max = 255, message = "Họ và tên tối đa 255 kí tự!")
    private String fullname;

    @PastOrPresent(message = "Ngày sinh phải nhỏ hơn hoặc bằng hiện tại")
    private LocalDate doB;

    @Size(max = 20, message = "Số điện thoại tối đa 20 kí tự!")
    @Pattern(regexp = "^(|\\+?[0-9][0-9 ()-]{5,18}[0-9])$", message = "Số điện thoại không hợp lệ")
    private String phone;

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
