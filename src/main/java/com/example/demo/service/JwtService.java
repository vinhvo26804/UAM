package com.example.demo.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expAccess;

    @Value("${jwt.refresh-expiration:1296000000}")
    private long expRefresh  ;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateAccessToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expAccess
        );

        return Jwts.builder()
                .subject(username)// lưu username người dùng
                .claim("type", "access")
                .issuedAt(now)     // thời gian tạo key
                .expiration(expiry)// hết hạn
                .signWith(getSigningKey()) // kí bằng key bí mật
                .compact();
    }
    public String generateRefreshToken(String username){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expRefresh);

        return  Jwts.builder()
                .subject(username)
                .id(UUID.randomUUID().toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }


    public String generateAccessToken(Long id) {
        return generateAccessToken(id.toString());
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // kiểm tra bằng khóa bí mật
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    public String extractTokenType(String token) {
        return extractClaim(
                token,
                claims -> claims.get("type", String.class)
        );
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Lấy username từ Subject của Token
    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Kiểm tra token đã hết hạn chưa
    public boolean isTokenExpired(String token) {
        Date expirationDate = extractClaim(token, Claims::getExpiration);
        return expirationDate.before(new Date());
    }

    // Xác thực Token
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

}
