package com.example.demo.dto.request.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateUserRequest {
    // Optional for clients sending a full profile; JWT subjects must remain stable.
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(max = 255)
    private String fullname;

    @PastOrPresent
    private LocalDate doB;

    @Size(max = 20)
    @Pattern(regexp = "^\\+?[0-9][0-9 ()-]{5,18}[0-9]$",
            message = "Phone must contain a valid phone number")
    private String phone;
}
