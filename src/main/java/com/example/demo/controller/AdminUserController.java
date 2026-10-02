package com.example.demo.controller;

import com.example.demo.dto.request.User.AdminUpdateUserRequest;
import com.example.demo.dto.response.AdminUserResponse;
import com.example.demo.service.AdminUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin Users")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public List<AdminUserResponse> searchUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        return adminUserService.searchUsers(username, email, role, status);
    }

    @GetMapping("/{id}")
    public AdminUserResponse getUser(@PathVariable Long id) {
        return adminUserService.getUser(id);
    }

    @PutMapping("/{id}")
    public AdminUserResponse updateUser(@PathVariable Long id,
                                        @Valid @RequestBody AdminUpdateUserRequest request) {
        return adminUserService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminUserService.disableUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/lock")
    public AdminUserResponse lockUser(@PathVariable Long id) {
        return adminUserService.lockUser(id);
    }

    @PatchMapping("/{id}/unlock")
    public AdminUserResponse unlockUser(@PathVariable Long id) {
        return adminUserService.unlockUser(id);
    }

    @PatchMapping("/{id}/disable")
    public AdminUserResponse disableUser(@PathVariable Long id) {
        return adminUserService.disableUser(id);
    }

    @PatchMapping("/{id}/activate")
    public AdminUserResponse activateUser(@PathVariable Long id) {
        return adminUserService.activateUser(id);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatusException(ResponseStatusException exception) {
        String reason = exception.getReason() == null ? "Request failed" : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("error", reason));
    }
}
