package com.example.demo.service;

import com.example.demo.dto.request.User.ChangeEmailRequest;
import com.example.demo.dto.request.User.UpdateUserRequest;
import com.example.demo.dto.request.VerifyChangeEmailRequest;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.OTP;
import com.example.demo.entity.User;
import com.example.demo.respository.UserRepository;
import com.example.demo.respository.VerifyRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Random;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerifyRepository verifyRepository;


    // GET CURRENT USER
    public UserResponse me(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getStatus()
        );
    }


    // REQUEST CHANGE EMAIL
    @Transactional
    public void changeEmail(
            ChangeEmailRequest request,
            String username
    ) {

        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        // Email mới không được giống email hiện tại
        if (user.getEmail().equals(request.getEmail())) {
            throw new RuntimeException("New email is same as old email");
        }

        // Email mới không được thuộc user khác
        if (userRepository.existsByemail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        // Chỉ account ACTIVE mới được đổi email
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("Account is not active"
            );
        }
        /*
         * Mỗi user chỉ nên có 1 OTP CHANGE_EMAIL đang tồn tại.
         * Nếu trước đó đã yêu cầu OTP rồi thì xoá OTP cũ.
         */
        verifyRepository.deleteByUserIdAndPurpose(user.getId(), "CHANGE_EMAIL"
        );

        String otpCode = generateOTP();

        OTP otp = new OTP();

        otp.setUserId(user.getId());
        otp.setUsername(username);
        otp.setOtpCode(otpCode);
        otp.setPurpose("CHANGE_EMAIL");

        otp.setExpriedAt(LocalDateTime.now().plusMinutes(5)
        );
        otp.setTarget(request.getEmail());
        verifyRepository.save(otp);
        //Test
        System.out.println("CHANGE EMAIL OTP: " + otpCode);
    }


    // VERIFY OTP AND UPDATE EMAIL
    public void verifyChangeEmail(VerifyChangeEmailRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        OTP otp = verifyRepository
                .findByUserIdAndPurpose(user.getId(), "CHANGE_EMAIL")
                .orElseThrow(() -> new RuntimeException("OTP not found"));

        // Check OTP
        if (!otp.getOtpCode().equals(request.getOtpCode())) {
            throw new RuntimeException(
                    "OTP invalid"
            );
        }

        // Check expired
        if (otp.getExpriedAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException(
                    "OTP expired"
            );
        }

        // Check purpose
        if (!"CHANGE_EMAIL".equals(otp.getPurpose())) {
            throw new RuntimeException(
                    "OTP purpose invalid"
            );
        }


        if (userRepository.existsByemail(otp.getTarget())) {
            throw new RuntimeException(
                    "Email already exists"
            );
        }


        user.setEmail(otp.getTarget());

        userRepository.save(user);


        verifyRepository.delete(otp);
    }

    private String generateOTP() {
        int otp = 100000 + new Random().nextInt(900000);
        return String.valueOf(otp);
    }

    public void updateUser(@Valid @RequestBody UpdateUserRequest request, String username){
        User user = userRepository.findByUsername(username).
                                    orElseThrow(()->new RuntimeException("User not found!"));

//        if(request.getFullname().equals(user.getFullname()) || (request.getPhone().equals(user.getPhone())) )
//            throw new RuntimeException("Value aren't different");
        if( (request.getDoB().isAfter(LocalDate.now())))
            throw new RuntimeException("Date must smaller than present");

            user.setFullname(request.getFullname());
            user.setPhone(request.getPhone());
            user.setDoB(request.getDoB());
            userRepository.save(user);

    }
}