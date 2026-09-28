package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class VertifyRequest {
   @NotBlank(message = " user valid!")
//   @Email(message = "Email format wrong!")
    private String userName;
    @NotBlank(message = "Otp valid!")
    private String otpCode;

}
