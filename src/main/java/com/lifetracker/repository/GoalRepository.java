package com.lifetracker.repository;

import com.lifetracker.entity.Goal;
import com.lifetracker.entity.enums.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    
    List<Goal> findByUserUserId(Long userId);
    
    List<Goal> findByUserUserIdAndStatus(Long userId, GoalStatus status);
    
    List<Goal> findBySkillSkillId(Long skillId);
    
    @Query("SELECT g.status, COUNT(g) FROM Goal g WHERE g.user.userId = :userId GROUP BY g.status")
    List<Object[]> getGoalCountByStatus(@Param("userId") Long userId);
}
