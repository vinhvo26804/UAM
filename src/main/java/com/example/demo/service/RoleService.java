package com.example.demo.service;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.respository.RoleRepository;
import com.example.demo.respository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public Role getRoleByName(String roleName) {
        return roleRepository.findByName(Role.RoleType.valueOf(roleName))
                .orElseThrow(() -> new RuntimeException("Role invalid!"));
    }

    @Transactional
    public void setRoleToUser(String username, String roleName) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Role role = roleRepository.findByName(Role.RoleType.valueOf(roleName)).orElseThrow(() -> new RuntimeException("Role not found!"));
        user.getRoles().add(role);

        userRepository.save(user);
    }
    @Transactional
    public void removeRoleUser(String username, String roleName) {
        if ("USER".equals(roleName)) {
            throw new RuntimeException(
                    "ROLE_USER must not be deleted!"
            );
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        Role role = roleRepository.findByName(Role.RoleType.valueOf(roleName))
                .orElseThrow(() ->
                        new RuntimeException("Role not found!")
                );

        user.getRoles().remove(role);

        userRepository.save(user);
    }
}