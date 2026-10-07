package com.example.demo.respository;

import com.example.demo.entity.Space;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpaceRepository extends JpaRepository<Space, Long> {

    Optional<Space> findByKey(String key);

    boolean existsByKey(String key);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Space s WHERE s.id = :id")
    Optional<Space> findByIdForUpdate(@Param("id") Long id);

    @Query(
        value = "SELECT s FROM Space s JOIN SpaceMember sm ON sm.space = s WHERE sm.user.Id = :userId",
        countQuery = "SELECT count(s) FROM Space s JOIN SpaceMember sm ON sm.space = s WHERE sm.user.Id = :userId"
    )
    Page<Space> findSpacesByUserId(@Param("userId") Long userId, Pageable pageable);
}
