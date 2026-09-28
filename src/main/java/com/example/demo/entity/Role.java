package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "role")
@Getter
@Setter


public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //Tự động tăng
    private  long Id;
    @Column(nullable = false, unique = true,length = 50) // Cấu hình cho thộc tính
    private String name;



    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
}
