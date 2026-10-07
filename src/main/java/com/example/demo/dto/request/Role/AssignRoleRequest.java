package com.example.demo.dto.request.Role;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignRoleRequest {

    @NotBlank(message = "roleName must not be blank")
    private String roleName;

    @NotBlank(message = "userName must not be blank")
    private String userName;

    @JsonProperty("username")
    public void setUsername(String username) {
        if (this.userName == null || this.userName.isBlank()) {
            this.userName = username;
        }
    }

    public String getUsername() {
        return this.userName;
    }
}
