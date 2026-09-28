package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@Table (name = "OTP") // Mặc định tên bảng
@Entity // Tạo entity
@Getter // Tự động tạo getter
@Setter // Tự động setter
@Builder // Tự động biuld dựa theo object đã tạo, Sử dụng được biulder
@NoArgsConstructor
@AllArgsConstructor

public class OTP {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    @Column(unique = true, length = 6)
    private String otpCode;
    private LocalDateTime expriedAt;
    private String purpose;
}
//    @ManyToMany(fetch =  FetchType.EAGER)
//    @JoinTable(
//            name = "user_ otp",
//            joinColumns = @JoinColumn(name = "otp_id"),
//            inverseJoinColumns = @JoinColumn(name = "user_id")
//    )
//private Set<OTP> otps = new HashSet<>();
//}
