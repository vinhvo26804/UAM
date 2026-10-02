package com.example.demo.respository;

import com.example.demo.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.Id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findByUsernameForUpdate(@Param("username") String username);
    //    Optional<User> findById(Long id);
    boolean existsByUsername(String username);
    boolean existsByemail(String email);

    // Explicit text casts keep optional null filters from being bound as bytea on PostgreSQL.
    @Query("""
        SELECT DISTINCT u
        FROM User u
        LEFT JOIN u.roles r
        WHERE (CAST(:username AS String) IS NULL
               OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:username AS String), '%')))
          AND (CAST(:email AS String) IS NULL
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:email AS String), '%')))
          AND (CAST(:role AS String) IS NULL
               OR LOWER(r.name) = LOWER(CAST(:role AS String)))
          AND (CAST(:status AS String) IS NULL
               OR LOWER(u.status) = LOWER(CAST(:status AS String)))
        """)
    List<User> searchUsers(
            @Param("username") String username,
            @Param("email") String email,
            @Param("role") String role,
            @Param("status") String status
    );

}
