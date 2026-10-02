package com.example.demo.respository;

import com.example.demo.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findFirstByHashTokenAndRevokeFalseAndExpireAtAfter(
            String hashToken, LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken token set token.revoke = true "
            + "where token.hashToken = :hashToken and token.revoke = false")
    int revokeByHashToken(@Param("hashToken") String hashToken);

    @Modifying(flushAutomatically = true)
    @Query("update RefreshToken token set token.revoke = true "
            + "where token.user.Id = :userId and token.revoke = false")
    int revokeByUserId(@Param("userId") Long userId);

}
