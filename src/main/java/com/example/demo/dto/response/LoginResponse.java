package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;

    public LoginResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
    }

    public String getAccessToken() {
        return accessToken;
    }
    public String getRefreshToken(){
        return refreshToken;
    }
    public String getTokenType() {
        return tokenType;
    }

}

