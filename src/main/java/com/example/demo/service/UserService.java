package com.example.demo.service;

import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.respository.UserRepository;
import jakarta.persistence.Id;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;


        public UserResponse me(@Valid @RequestBody String userName) {
        User user = userRepository.findByUsername(userName)
                .orElseThrow(()-> new RuntimeException(" User not found!"));

        return  new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getStatus()


        );
        }
}

