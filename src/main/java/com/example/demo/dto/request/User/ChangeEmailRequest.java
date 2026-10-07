package com.example.demo.dto.request.User;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangeEmailRequest {

    @NotBlank(message = "Email không được để trống!")
    @Email(message = "Email không đúng định dạng!")
    @Size(max = 255, message = "Email tối đa 255 kí tự!")
    private String email;

    @JsonProperty("newEmail")
    public void setNewEmail(String newEmail) {
        if (this.email == null || this.email.isBlank()) {
            this.email = newEmail;
        }
    }

    public String getNewEmail() {
        return this.email;
    }
}
