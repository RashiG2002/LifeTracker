package com.lifetracker.repository;

import com.lifetracker.entity.Skill;
import com.lifetracker.entity.enums.SkillLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    
    List<Skill> findByUserUserId(Long userId);
    
    List<Skill> findByUserUserIdAndLevel(Long userId, SkillLevel level);
    
    List<Skill> findByUserUserIdAndSkillNameContainingIgnoreCase(Long userId, String skillName);
}
