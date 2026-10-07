package com.example.demo.respository;

import com.example.demo.entity.Sprint;
import com.example.demo.entity.SprintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {

    Optional<Sprint> findByIdAndSpaceId(Long id, Long spaceId);

    @Query("SELECT MAX(s.position) FROM Sprint s WHERE s.space.id = :spaceId")
    Long findMaxPositionBySpaceId(@Param("spaceId") Long spaceId);

    Optional<Sprint> findFirstBySpaceIdAndStatus(Long spaceId, SprintStatus status);

    boolean existsBySpaceIdAndStatus(Long spaceId, SprintStatus status);

    @Query("SELECT s FROM Sprint s WHERE s.space.id = :spaceId " +
           "AND (:status IS NULL OR s.status = :status) " +
           "ORDER BY s.position ASC, s.id ASC")
    Page<Sprint> findSprintsBySpaceIdAndStatus(@Param("spaceId") Long spaceId, @Param("status") SprintStatus status, Pageable pageable);

    @Query("SELECT s FROM Sprint s WHERE s.space.id = :spaceId AND s.status = 'FUTURE' AND s.position > :position ORDER BY s.position ASC")
    Page<Sprint> findNextFutureSprint(@Param("spaceId") Long spaceId, @Param("position") Long position, Pageable pageable);
}
