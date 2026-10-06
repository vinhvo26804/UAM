package com.example.demo.service;

import com.example.demo.dto.request.*;
import com.example.demo.dto.response.LoginResponse;
import com.example.demo.entity.OTP;
import com.example.demo.entity.RefreshToken;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.respository.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
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
    @Autowired
    private RoleRepository roleRepository;
    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUserName())) {
            throw new RuntimeException("User đã tồn tại");
        }
        if (userRepository.existsByemail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }
        Role defaultRole = roleRepository.findByName(Role.RoleType.USER)
                .orElseThrow(() -> new IllegalStateException("Default USER role is missing; run database migrations"));
        // Tạo đối tượng với mật khẩu đã mã hóa BCrypt
        User user = User.builder()
                .username(request.getUserName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        user.getRoles().add(defaultRole);

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



    @Transactional(noRollbackFor = ResponseStatusException.class)
    public String Login(LoginRequest loginRequest) {
        Optional<User> userOptional = userRepository.findByUsernameForUpdate(
                loginRequest.getUserName()
        );

        if (userOptional.isEmpty()) {
            return " Username or Password wrong! ";
        }
        User user = userOptional.get();

        requireLoginAllowed(user);
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return " Username or password wrong! ";
        }
        return "Login Success";
    }

    @Transactional
    public String Vertify(VertifyRequest vertifyRequest)
    {
        Optional<User> userOptional = userRepository.findByUsernameForUpdate(vertifyRequest.getUserName());
        if (userOptional.isEmpty()) {
            return "Not valid";
        }
        User user = userOptional.get();
        if (user.getStatus() != UserStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Only pending accounts can be verified");
        }

        Optional<OTP> otpOptional = verifyRespository.findByUserIdAndPurpose(user.getId(), "verify");
        if (otpOptional.isEmpty()) {
            return "OTP not found";
        }
        OTP otp = otpOptional.get();
        if (!"verify".equals(otp.getPurpose())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong purpose!");
        }

            if (!vertifyRequest.getOtpCode().equals(otp.getOtpCode())) {
            return "Verify failed";
        }
        if (otp.getExpriedAt() == null || !otp.getExpriedAt().isAfter(LocalDateTime.now())) {
            return "OTP expired!";
        }
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        verifyRespository.delete(otp);
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



    @Transactional(noRollbackFor = ResponseStatusException.class)
    public LoginResponse loginWithJwt(@Valid @RequestBody LoginRequest loginRequest) {
        User user = userRepository.findByUsernameForUpdate(loginRequest.getUserName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Username or password is wrong"));

        requireLoginAllowed(user);
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    loginRequest.getUserName(), loginRequest.getPassword()));
            resetFailedAttemps(user);
        } catch (BadCredentialsException exception) {
            handleFailedLogin(user);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username or password is wrong");
        } catch (LockedException exception) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "Account is locked");
        } catch (DisabledException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is not active");
        }

        // Xác thực thành công -> Tạo token chứa username
        String accessToken = jwtService.generateAccessToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        String hashToken = hashRefreshToken(refreshToken);

        RefreshToken refreshTokenEntity =refreshTokenRepository.findByUserId(user.getId())
                .orElseGet(RefreshToken::new);

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
    private  void checkHashTokenOfUser(Long userId){

    }

    private void resetFailedAttemps(User user) {
        if(user.getFailedLoginAttemps()>0 || user.getLockUntill() !=null  ) { // Điều kiện gọi Db, vì đang set mặc định, nếu phải kiểm tra mới gọi db, optimize
            user.setLockUntill(null);
            user.setFailedLoginAttemps(0);
            userRepository.save(user);
        }
    }
    private void requireLoginAllowed(User user) {
        if (user.getStatus() == UserStatus.LOCKED) {
            LocalDateTime lockedUntil = user.getLockUntill();
            if (lockedUntil == null || lockedUntil.isAfter(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.LOCKED, "Account is locked");
            }
            // Only a timed login lock may expire automatically. Admin locks have no deadline.
            user.setStatus(UserStatus.ACTIVE);
            user.setLockUntill(null);
            user.setFailedLoginAttemps(0);
            userRepository.save(user);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is not active");
        }
    }
    private void handleFailedLogin(User user){
        int failedAttemps = user.getFailedLoginAttemps()+1;
        user.setFailedLoginAttemps(failedAttemps);

        if(failedAttemps >=MAX_LOCKED_ATTEMPS){
            user.setLockUntill(LocalDateTime.now().plusMinutes(TIME_LOCKED_MITUTES));
            user.setStatus(UserStatus.LOCKED);

        }
        userRepository.save(user);
        if (user.getStatus() == UserStatus.LOCKED) {
            refreshTokenRepository.revokeByUserId(user.getId());
        }
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
                || owner.getStatus() != UserStatus.ACTIVE) {
            throw invalidRefreshToken();
        }
        return new LoginResponse(jwtService.generateAccessToken(owner.getUsername()), refreshToken);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        // A token can revoke only its matching records. Unknown/already revoked tokens are a no-op.
        refreshTokenRepository.revokeByHashToken(hashRefreshToken(request.getRefreshToken()));
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsernameForUpdate(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        // Recheck after acquiring the same row lock used by admin status changes.
        // Password changes must never clear even an expired login lock.
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "Account is locked");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is not active");
        }

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




}
