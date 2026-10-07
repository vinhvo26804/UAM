package com.example.demo.service;

import com.example.demo.dto.request.User.ChangeEmailRequest;
import com.example.demo.dto.request.User.UpdateUserRequest;
import com.example.demo.dto.request.VerifyChangeEmailRequest;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.OTP;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.respository.UserRepository;
import com.example.demo.respository.VerifyRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerifyRepository verifyRepository;

    @Transactional(readOnly = true)
    public UserResponse me(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
                );

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                user.getDoB(),
                user.getStatus(),
                user.getFullname(),
                user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .map(Enum::name)
                        .collect(Collectors.toSet())
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(
            String username,
            String email,
            String role,
            String status
    ) {
        username = normalize(username);
        email = normalize(email);
        role = normalize(role);
        status = normalize(status);

        List<User> users = userRepository.searchUsers(
                username,
                email,
                role,
                status
        );

        return users.stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getPhone(),
                        user.getDoB(),
                        user.getStatus(),
                        user.getFullname(),
                        user.getRoles()
                                .stream()
                                .map(Role::getName)
                                .map(Enum::name)
                                .collect(Collectors.toSet())
                ))
                .toList();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @Transactional
    public void changeEmail(
            ChangeEmailRequest request,
            String username
    ) {
        User user = activeUserForUpdate(username);

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không được để trống");
        }

        String newEmail = request.getEmail().trim();

        if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(newEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New email is same as old email");
        }

        if (userRepository.existsByemail(newEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        verifyRepository.deleteByUserIdAndPurpose(user.getId(), "CHANGE_EMAIL");

        String otpCode = generateOTP();

        OTP otp = new OTP();
        otp.setUserId(user.getId());
        otp.setUsername(username);
        otp.setOtpCode(otpCode);
        otp.setPurpose("CHANGE_EMAIL");
        otp.setExpriedAt(LocalDateTime.now().plusMinutes(5));
        otp.setTarget(newEmail);
        verifyRepository.save(otp);
    }

    @Transactional
    public void verifyChangeEmail(VerifyChangeEmailRequest request, String username) {
        User user = activeUserForUpdate(username);

        OTP otp = verifyRepository
                .findByUserIdAndPurpose(user.getId(), "CHANGE_EMAIL")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP not found"));

        if (request.getOtpCode() == null || request.getOtpCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP invalid");
        }

        if (!otp.getOtpCode().equals(request.getOtpCode().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP invalid");
        }

        if (otp.getExpriedAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP expired");
        }

        if (!"CHANGE_EMAIL".equals(otp.getPurpose())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP purpose invalid");
        }

        if (otp.getTarget() == null || otp.getTarget().isBlank() || userRepository.existsByemail(otp.getTarget())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        user.setEmail(otp.getTarget());
        userRepository.save(user);
        verifyRepository.delete(otp);
    }

    private String generateOTP() {
        SecureRandom secureRandom = new SecureRandom();
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    @Transactional
    public void updateUser(UpdateUserRequest request, String username) {
        User user = activeUserForUpdate(username);

        if (request.getDoB() != null && request.getDoB().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date must smaller than present");
        }

        user.setFullname(request.getFullname());
        user.setPhone(request.getPhone());
        user.setDoB(request.getDoB());
        userRepository.save(user);
    }

    private User activeUserForUpdate(String username) {
        User user = userRepository.findByUsernameForUpdate(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "Account is locked");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is not active");
        }
        return user;
    }
}
