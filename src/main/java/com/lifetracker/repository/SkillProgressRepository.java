package com.lifetracker.repository;

import com.lifetracker.entity.SkillProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SkillProgressRepository extends JpaRepository<SkillProgress, Long> {
    
    List<SkillProgress> findBySkillSkillIdOrderByProgressDateDesc(Long skillId);
    
    List<SkillProgress> findBySkillSkillIdAndProgressDateBetween(Long skillId, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT SUM(sp.hoursSpent) FROM SkillProgress sp WHERE sp.skill.skillId = :skillId")
    BigDecimal getTotalHoursSpentBySkillId(@Param("skillId") Long skillId);
    
    @Query("SELECT sp.progressDate, SUM(sp.hoursSpent) FROM SkillProgress sp WHERE sp.skill.skillId = :skillId GROUP BY sp.progressDate ORDER BY sp.progressDate")
    List<Object[]> getProgressOverTime(@Param("skillId") Long skillId);
}
