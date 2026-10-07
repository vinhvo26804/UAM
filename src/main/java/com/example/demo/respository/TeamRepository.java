package com.example.demo.respository;

import com.example.demo.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query("SELECT tm.team FROM TeamMember tm WHERE tm.user.Id = :userId ORDER BY tm.team.createdAt DESC")
    List<Team> findTeamsByUserId(@Param("userId") Long userId);
}
