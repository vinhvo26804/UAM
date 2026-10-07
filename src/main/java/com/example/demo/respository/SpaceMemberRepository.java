package com.example.demo.respository;

import com.example.demo.entity.SpaceMember;
import com.example.demo.entity.SpaceRole;
import com.example.demo.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpaceMemberRepository extends JpaRepository<SpaceMember, Long> {

    Optional<SpaceMember> findBySpaceIdAndUserId(Long spaceId, Long userId);

    boolean existsBySpaceIdAndUserId(Long spaceId, Long userId);

    long countBySpaceIdAndRole(Long spaceId, SpaceRole role);

    @Query("SELECT sm FROM SpaceMember sm WHERE sm.space.id = :spaceId " +
           "AND (CAST(:query AS string) IS NULL OR LOWER(sm.user.fullname) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) OR LOWER(sm.user.username) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%'))) " +
           "ORDER BY sm.joinedAt ASC")
    Page<SpaceMember> findMembersBySpaceId(@Param("spaceId") Long spaceId, @Param("query") String query, Pageable pageable);

    @Query("SELECT sm FROM SpaceMember sm WHERE sm.space.id = :spaceId " +
           "AND sm.user.status = 'ACTIVE' " +
           "AND sm.role IN ('ADMIN', 'MANAGER', 'MEMBER') " +
           "ORDER BY sm.user.fullname ASC")
    List<SpaceMember> findEligibleAssignees(@Param("spaceId") Long spaceId);

    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' " +
           "AND u.Id NOT IN (SELECT sm.user.Id FROM SpaceMember sm WHERE sm.space.id = :spaceId) " +
           "AND (CAST(:query AS string) IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) OR LOWER(u.fullname) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%'))) " +
           "ORDER BY u.fullname ASC")
    List<User> findCandidateUsers(@Param("spaceId") Long spaceId, @Param("query") String query, Pageable pageable);

    @Modifying
    @Query("DELETE FROM SpaceMember sm WHERE sm.space.id = :spaceId AND sm.user.Id = :userId")
    int deleteBySpaceIdAndUserId(@Param("spaceId") Long spaceId, @Param("userId") Long userId);
}
