package com.example.demo.service;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.respository.RoleRepository;
import com.example.demo.respository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RoleService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public List<Role> getAllRole() {
        return roleRepository.findAll();
    }

    private Role.RoleType parseRoleType(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role name must not be blank!");
        }
        String normalized = roleName.trim();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        try {
            return Role.RoleType.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role invalid: " + roleName);
        }
    }

    public Role getRoleByName(String roleName) {
        Role.RoleType roleType = parseRoleType(roleName);
        return roleRepository.findByName(roleType)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found!"));
    }

    @Transactional
    public void setRoleToUser(String username, String roleName) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Role.RoleType roleType = parseRoleType(roleName);
        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found!"));
        user.getRoles().add(role);

        userRepository.save(user);
    }

    @Transactional
    public void removeRoleUser(String username, String roleName) {
        Role.RoleType roleType = parseRoleType(roleName);
        if (roleType == Role.RoleType.USER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ROLE_USER must not be deleted!"
            );
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
                );

        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found!")
                );

        user.getRoles().remove(role);

        userRepository.save(user);
    }
}
