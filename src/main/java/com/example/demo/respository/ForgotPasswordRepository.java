package com.example.demo.respository;

import com.example.demo.entity.OTP;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForgotPasswordRepository extends JpaRepository<OTP, Long> {
    Optional<OTP>findByUserIdAndPurpose(Long userId, String purpose);

}
