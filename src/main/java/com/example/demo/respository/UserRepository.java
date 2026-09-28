package com.example.demo.respository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    //    Optional<User> findById(Long id);
    boolean existsByUsername(String username);
    boolean existsByemail(String email);

}

