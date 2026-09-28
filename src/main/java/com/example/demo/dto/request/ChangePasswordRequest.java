package com.example.demo.dto.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class ChangePasswordRequest {
    @NotBlank(message = "must not blank!")
    @Column(nullable = false)
    private  String currentPassword;
    @NotBlank(message = "must not blank!")
    @Size(min = 8)
    @Column(nullable = false)
    private String newPassword;
    @Column(nullable = false)
    @NotBlank(message = "must not blank!")
    private  String confirmNewPassword;

}
