package com.example.demo.service;

import com.example.demo.dto.request.*;
import com.example.demo.dto.response.LoginResponse;
import com.example.demo.entity.OTP;
import com.example.demo.entity.RefreshToken;
import com.example.demo.entity.User;
import com.example.demo.respository.ForgotPasswordRepository;
import com.example.demo.respository.RefreshTokenRepository;
import com.example.demo.respository.UserRepository;
import com.example.demo.respository.VerifyRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Random;



@Service
public class AuthService {
    private static final int TIME_LOCKED_MITUTES = 15;
    private static final long MAX_LOCKED_ATTEMPS =5;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VerifyRepository verifyRespository;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private  ForgotPasswordRepository forgotPasswordRepository;

    public String register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUserName())) {
            throw new RuntimeException("User đã tồn tại");
        }
        if (userRepository.existsByemail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        // Tạo đối tượng với mật khẩu đã mã hóa BCrypt
        User user = User.builder()
                .username(request.getUserName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User userSaved = userRepository.save(user);

        Random random = new Random();
        String otpCode = String.format("%06d", random.nextInt(1000000));
        LocalDateTime expriedAt = LocalDateTime.now().plusMinutes(5);

        OTP otp = OTP.builder()
                .otpCode(otpCode)
                .expriedAt(expriedAt)
                .purpose("verify")
                .userId(userSaved.getId())
                .build();
        verifyRespository.save(otp);

        return "Register Success";
    }



    public String Login(LoginRequest loginRequest) {
        Optional<User> userOptional = userRepository.findByUsername(
                loginRequest.getUserName()
        );

        if (userOptional.isEmpty()) {
            return " Username or Password wrong! ";
        }
        User user = userOptional.get();

        if ("PENDING".equals(user.getStatus())) {
            return "Account must not actice!";
        }
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return " Username or password wrong! ";
        }
        return "Login Success";
    }

    public String Vertify(VertifyRequest vertifyRequest)
    {
        Optional<User> userOptional = userRepository.findByUsername(vertifyRequest.getUserName());
        if (userOptional.isEmpty()) {
            return "Not valid";
        }
        User user = userOptional.get();

        Optional<OTP> otpOptional = verifyRespository.findById(user.getId());
        if (otpOptional.isEmpty()) {
            return "OTP not found";
        }
        OTP otp = otpOptional.get();
        if(!"verify".equals(otp.getPurpose()))
            throw  new HttpStatusCodeException(HttpStatus.UNAUTHORIZED, "Wrong purpose!") {
            };

            if (!vertifyRequest.getOtpCode().equals(otp.getOtpCode())) {
            return "Verify failed";
        }
        if (otp.getExpriedAt().isBefore(LocalDateTime.now())) {
            return "OTP expired!";
        }
        user.setStatus("ACTIVE");
        userRepository.save(user);
        return "Verify Success";
    }
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository
                .findByUsername(request.getUserName())
                .orElseThrow(() ->
                        new RuntimeException("Username không tồn tại")
                );

        OTP otp = forgotPasswordRepository
                .findByUserIdAndPurpose(
                        user.getId(),
                        "PASSWORD_RESET"
                )
                .orElse(new OTP());

        otp.setUserId(user.getId());
        otp.setOtpCode(otp.getOtpCode());
        otp.setPurpose("PASSWORD_RESET");
        otp.setExpriedAt(LocalDateTime.now().plusMinutes(5));

        forgotPasswordRepository.save(otp);
    }



    public LoginResponse loginWithJwt(@Valid @RequestBody LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUserName())
                .orElseThrow(() -> new HttpStatusCodeException(HttpStatus.UNAUTHORIZED  ,"user not found!") {
                });

        if(isAccountLocked(user)){
            throw new HttpStatusCodeException(HttpStatus.LOCKED, "account is Locked") {
            };
        }



        if ("PENDING".equals(user.getStatus())) {
            throw new HttpStatusCodeException( HttpStatus.UNAUTHORIZED, "User is not Active"){};
        }
try {


    authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                    loginRequest.getUserName(),
                    loginRequest.getPassword()
            )

    );

    resetFailedAttemps(user);

}catch (BadCredentialsException exception){
    handleFailedLogin(user); // ghi nhận lỗi bằng badCredential thì xử lí handle

    throw new HttpStatusCodeException(HttpStatus.UNAUTHORIZED, "Username or Password is Wrong!") {
    };

}

        // Xác thực thành công -> Tạo token chứa username
        String accessToken = jwtService.generateAccessToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        String hashToken = hashRefreshToken(refreshToken);

        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setHashToken(hashToken);
        refreshTokenEntity.setRevoke(false);
        refreshTokenEntity.setExpireAt(LocalDateTime.ofInstant(
                jwtService.extractAllClaims(refreshToken).getExpiration().toInstant(),
                ZoneId.systemDefault()
        ));
        refreshTokenEntity.setUser(user);

        refreshTokenRepository.save(refreshTokenEntity);
         // Các phương thức save, remove,... phải gọi từ repo-> phải định nghĩa các phương thwucs của JPa mới thực thi các lệnh đó dược


        return new LoginResponse(
                accessToken, refreshToken
        );
    }

    private void resetFailedAttemps(User user) {
        if(user.getFailedLoginAttemps()>0 || user.getLockUntill() !=null  ) { // Điều kiện gọi Db, vì đang set mặc định, nếu phải kiểm tra mới gọi db, optimize
            user.setLockUntill(null);
            user.setFailedLoginAttemps(0);
            user.setStatus("ACTIVE");
            userRepository.save(user);
        }
    }
    private boolean isAccountLocked(User user) {
        LocalDateTime lockedUntil = user.getLockUntill();

        if( lockedUntil == null ) {// chưa set tg khóa
            return false;
        }
        if(lockedUntil.now().isBefore(lockedUntil)){
            return true;
        }
        resetFailedAttemps(user);
        return  false;
    }
    private void handleFailedLogin(User user){
        int failedAttemps = user.getFailedLoginAttemps()+1;
        user.setFailedLoginAttemps(failedAttemps);

        if(failedAttemps >=MAX_LOCKED_ATTEMPS){
            user.setLockUntill(LocalDateTime.now().plusMinutes(TIME_LOCKED_MITUTES));
            user.setStatus("LOCKED");

        }
        userRepository.save(user);
    }
    private String hashRefreshToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
    @Transactional(readOnly = true)
    public LoginResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();
        Claims claims;

        try {
            claims = jwtService.extractAllClaims(refreshToken);
            if (!"refresh".equals(claims.get("type", String.class))
                    || claims.getSubject() == null || claims.getSubject().isBlank()
                    || claims.getExpiration() == null) {
                throw invalidRefreshToken();
            }
        } catch (JwtException | IllegalArgumentException e) {
            throw invalidRefreshToken();
        }

        RefreshToken stored = refreshTokenRepository
                .findFirstByHashTokenAndRevokeFalseAndExpireAtAfter(
                        hashRefreshToken(refreshToken), LocalDateTime.now())
                .orElseThrow(this::invalidRefreshToken);
        User owner = stored.getUser();
        if (owner == null || !claims.getSubject().equals(owner.getUsername())
                || !"ACTIVE".equals(owner.getStatus())) {
            throw invalidRefreshToken();
        }
        return new LoginResponse(jwtService.generateAccessToken(owner.getUsername()), refreshToken);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        // A token can revoke only its matching records. Unknown/already revoked tokens are a no-op.
        refreshTokenRepository.revokeByHashToken(hashRefreshToken(request.getRefreshToken()));
    }

    public  void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username).orElseThrow(()-> new RuntimeException(" Not found username"));

        if(!passwordEncoder.matches(request.getCurrentPassword(),user.getPassword()))
        {
            throw new RuntimeException("Password current  is Wrong!");
        };
        if(passwordEncoder.matches(request.getNewPassword(), user.getPassword())){
            throw new RuntimeException("Password current is different new password");

        };
        if(!request.getNewPassword().equals(request.getConfirmNewPassword())){
            throw  new RuntimeException(" new Password  not same! ");
        };
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);


    }

    private ResponseStatusException invalidRefreshToken() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or revoked refresh token");
    }



//public  boolean isLocked(User user){
//        if()
//}
}
