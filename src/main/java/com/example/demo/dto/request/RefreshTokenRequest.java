package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenRequest {
    @NotBlank(message = "refreshToken is required")
    @Size(max = 4096, message = "refreshToken is too long")
    private String refreshToken;
}
