package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table (name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(nullable = false, unique = false)
    private String username;
    @Column(nullable = false)
    private String password;
    @Column(nullable = false, unique = false)
    private String email;
    @Builder.Default
    private String status = "PENDING";

    @Column(nullable = false,columnDefinition = "integer default 0")
    private int failedLoginAttemps = 0;
    private LocalDateTime lockUntill;


    private String role;


    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();


}
