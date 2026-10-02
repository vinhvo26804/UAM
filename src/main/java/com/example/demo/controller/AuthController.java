package com.example.demo.controller;

import com.example.demo.dto.request.*;
import com.example.demo.dto.response.LoginResponse;
import com.example.demo.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping
@Tag(name = "Authentication")

public class AuthController {
    @Autowired
    private AuthService authService;



    @PostMapping("/api/v1/auth/register")
    @SecurityRequirements
    public ResponseEntity<String> registerUser(@Valid @RequestBody RegisterRequest request) { // @request b Lấy dữ liệu json từ http requets chuyển thành dạng object java
                                                //@valid xác định rule validation đã được  thực thi hay chưa  Các @blank,.. trong respository
        String response = authService.register(request);
        return ResponseEntity.ok(response);
    }
//    @PostMapping("/api/v1/auth/login")
//    public ResponseEntity<String> login( @Valid @RequestBody LoginRequest request){
//        String response = authService.Login(request);
//        return ResponseEntity.ok(response);
//    }
@PostMapping("/loginWithJwt")
@SecurityRequirements
public ResponseEntity<LoginResponse> loginWithJwt(@RequestBody @Valid LoginRequest request) {
    LoginResponse response = authService.loginWithJwt(request);
    return ResponseEntity.ok(response);
}

        @PostMapping("/api/v1/auth/verify")
        @SecurityRequirements
    public ResponseEntity<String> verify(@Valid @RequestBody VertifyRequest vertifyRequest){
        String response = authService.Vertify(vertifyRequest);
        return ResponseEntity.ok(response);
}
@PostMapping("/refreshToken")
@SecurityRequirements
public ResponseEntity<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request){
        return ResponseEntity.ok(authService.refreshToken(request));

}
@PostMapping("/logout")
@SecurityRequirements
public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
}

@ExceptionHandler(ResponseStatusException.class)
public ResponseEntity<Map<String, String>> handleTokenError(ResponseStatusException exception) {
        String message = exception.getReason() != null ? exception.getReason() : "Request failed";
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("error", message));
}
@GetMapping("/test")
public String test(){
        return " Access success";
}
@PutMapping("/changePassword")
    public ResponseEntity<String> changePassword( @Valid @RequestBody ChangePasswordRequest request, Authentication authentication){
        authService.changePassword(
                authentication.getName(), request
        );

        return  ResponseEntity.ok(("Password was be changed"));
}



}
