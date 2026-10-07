package com.example.demo.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "Mật khẩu hiện tại không được để trống!")
    private String currentPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống!")
    @Size(min = 8, max = 128, message = "Mật khẩu mới phải từ 8 đến 128 kí tự")
    private String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống!")
    @Size(min = 8, max = 128, message = "Xác nhận mật khẩu phải từ 8 đến 128 kí tự")
    private String confirmNewPassword;

    @JsonProperty("oldPassword")
    public void setOldPassword(String oldPassword) {
        if (this.currentPassword == null || this.currentPassword.isBlank()) {
            this.currentPassword = oldPassword;
        }
    }

    @JsonProperty("confirmPassword")
    public void setConfirmPassword(String confirmPassword) {
        if (this.confirmNewPassword == null || this.confirmNewPassword.isBlank()) {
            this.confirmNewPassword = confirmPassword;
        }
    }
}
