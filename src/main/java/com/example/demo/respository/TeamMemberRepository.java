package com.example.demo.respository;

import com.example.demo.entity.TeamMember;
import com.example.demo.entity.TeamRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    @Query("SELECT tm FROM TeamMember tm JOIN FETCH tm.team JOIN FETCH tm.user WHERE tm.team.id = :teamId AND tm.user.Id = :userId")
    Optional<TeamMember> findByTeamIdAndUserId(@Param("teamId") Long teamId, @Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(tm) > 0 THEN true ELSE false END FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.user.Id = :userId")
    boolean existsByTeamIdAndUserId(@Param("teamId") Long teamId, @Param("userId") Long userId);

    @Query(value = "SELECT tm FROM TeamMember tm JOIN FETCH tm.user WHERE tm.team.id = :teamId ORDER BY tm.joinedAt ASC",
           countQuery = "SELECT COUNT(tm) FROM TeamMember tm WHERE tm.team.id = :teamId")
    Page<TeamMember> findByTeamId(@Param("teamId") Long teamId, Pageable pageable);

    @Query("SELECT COUNT(tm) FROM TeamMember tm WHERE tm.team.id = :teamId")
    long countByTeamId(@Param("teamId") Long teamId);

    void deleteByTeamIdAndUserId(Long teamId, Long userId);
}
