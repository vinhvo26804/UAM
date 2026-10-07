package com.example.demo.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyChangeEmailRequest {

    @NotBlank(message = "Mã OTP không được để trống!")
    @Pattern(regexp = "^\\d{6}$", message = "Mã OTP phải gồm 6 chữ số")
    private String otpCode;

    @JsonProperty("otp")
    public void setOtp(String otp) {
        if (this.otpCode == null || this.otpCode.isBlank()) {
            this.otpCode = otp;
        }
    }

    public String getOtp() {
        return this.otpCode;
    }
}
