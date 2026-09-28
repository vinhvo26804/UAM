package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Permission")
@Getter
@Setter

public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @NotBlank
    private String name;


    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles = new HashSet<>();
}