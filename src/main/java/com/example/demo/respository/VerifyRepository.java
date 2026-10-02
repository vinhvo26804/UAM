package com.example.demo.respository;

import com.example.demo.entity.OTP;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerifyRepository extends JpaRepository<OTP, Long> {
    Optional<OTP> findByUserIdAndPurpose(Long userId, String purpose);
    Optional<OTP> findByUsername(String username);
    Optional<OTP> findByUserId(Long userId);
    long deleteByUserIdAndPurpose(Long userId, String purpose);






}
