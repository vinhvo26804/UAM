package com.example.demo.dto.response;

import com.example.demo.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class UserResponse {
    private long id;
    private String username;
    private String phone;
    private LocalDate dob;
    private UserStatus status;
    private String fullname;
    private Set<String> roles;

}
