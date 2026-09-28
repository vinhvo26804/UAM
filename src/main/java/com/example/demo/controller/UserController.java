package com.example.demo.controller;

import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping

public class UserController {
    @Autowired
    private UserService userService;


    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(  Authentication authentication){
//     userService user =    userService.me(authentication.getName());

//UserResponse response = authentication.getName()
        String username = authentication.getName();
        UserResponse response =userService.me(username);
    return ResponseEntity.ok( response);
    }
}
