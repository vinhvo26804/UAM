package com.example.demo.controller;

import com.example.demo.dto.request.Role.AssignRoleRequest;
import com.example.demo.dto.request.Role.AssignRoleRequest;
import com.example.demo.entity.Role;
import com.example.demo.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {

        return ResponseEntity.ok(
                roleService.getAllRole()
        );
    }

    @GetMapping("/{roleName}")
    public ResponseEntity<Role> getRoleByName(
            @PathVariable String roleName
    ) {

        return ResponseEntity.ok(
                roleService.getRoleByName(roleName)
        );
    }

    @PostMapping("/assign")
    public ResponseEntity<String> assignRoleToUser(
            @Valid @RequestBody AssignRoleRequest request
    ) {
        roleService.setRoleToUser(request.getUsername(), request.getRoleName());
        return ResponseEntity.ok("Assign role successfully");
    }
    @DeleteMapping("/remove")
    public ResponseEntity<String> removeRoleFromUser(
            @Valid @RequestBody AssignRoleRequest request) {
        roleService.removeRoleUser(
                request.getUsername(),
                request.getRoleName()
        );

        return ResponseEntity.ok(
                "Remove role successfully"
        );
    }
}