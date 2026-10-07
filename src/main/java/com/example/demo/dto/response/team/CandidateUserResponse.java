package com.example.demo.dto.response.team;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CandidateUserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullname;
}
