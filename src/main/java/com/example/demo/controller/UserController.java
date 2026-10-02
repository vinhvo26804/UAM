package com.example.demo.controller;

import com.example.demo.dto.request.User.ChangeEmailRequest;
import com.example.demo.dto.request.User.UpdateUserRequest;
import com.example.demo.dto.request.VerifyChangeEmailRequest;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@Tag(name = "Users")

public class UserController {
    @Autowired
    private UserService userService;


    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(  Authentication authentication){
//     userService user =    userService.me(authentication.getName());

//UserResponse response = authentication.getName()
        String username = authentication.getName();
        UserResponse response = userService.me(username);
    return ResponseEntity.ok( response);
    }
    @PutMapping("/me/email")
    public ResponseEntity<String> changeEmail(
            @Valid @RequestBody ChangeEmailRequest request,
            Authentication authentication
    ) {
        userService.changeEmail(request, authentication.getName()
        );

        return ResponseEntity.ok("OTP sent to new email");
    }
    @PostMapping("/me/email/verify")
    public ResponseEntity<String> verifyChangeEmail(
            @Valid @RequestBody VerifyChangeEmailRequest request, Authentication authentication
    ) {
        userService.verifyChangeEmail(request, authentication.getName()
        );
        return ResponseEntity.ok("Email changed successfully");
    }
    @PutMapping("me/update")
    public ResponseEntity<String> updateUser(@Valid @RequestBody UpdateUserRequest request, Authentication authentication)
    {
        userService.updateUser(request, authentication.getName());
        return ResponseEntity.ok("Update user successfully");
    }
    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(

            @RequestParam(required = false)
            String username,

            @RequestParam(required = false)
            String email,

            @RequestParam(required = false)
            String role,

            @RequestParam(required = false)
            String status
    ) {

        return ResponseEntity.ok(
                userService.searchUsers(
                        username,
                        email,
                        role,
                        status
                )
        );
    }
}
