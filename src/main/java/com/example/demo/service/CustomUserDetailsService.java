package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.respository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + username)
                );

//        var authorities = (user.getRoles() != null && !user.getRoles().isEmpty())
//                ? user.getRoles().stream()
//                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
//                        .toList()
//                : List.of(new SimpleGrantedAuthority("ROLE_USER"));

        var authorities = user.getRoles().stream()
                .flatMap(role -> {
                    var roleAuthority =
                            Stream.of(new SimpleGrantedAuthority("ROLE_" + role.getName()));

                    var permissionAuthorities =
                            role.getPermissions().stream()
                                    .map(permission ->
                                            new SimpleGrantedAuthority(permission.getName()));

                    return Stream.concat(roleAuthority, permissionAuthorities);
                })
                .distinct()
                .toList();
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }
}
