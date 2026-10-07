package com.example.demo.dto.request.User;

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
public class AdminUpdateUserRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Fullname is required")
    @Size(max = 255, message = "Fullname must not exceed 255 characters")
    private String fullname;

    @PastOrPresent(message = "Date of birth must be in the past or present")
    private LocalDate doB;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    @Pattern(regexp = "^(|\\+?[0-9][0-9 ()-]{5,18}[0-9])$",
            message = "Phone must contain a valid phone number")
    private String phone;
}
