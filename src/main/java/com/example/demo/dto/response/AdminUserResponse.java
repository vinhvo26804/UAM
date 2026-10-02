package com.example.demo.dto.response;

import com.example.demo.entity.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public record AdminUserResponse(
        Long id,
        String username,
        String email,
        String fullname,
        LocalDate doB,
        String phone,
        UserStatus status,
        Set<String> roles,
        LocalDateTime lockUntill
) {
}
