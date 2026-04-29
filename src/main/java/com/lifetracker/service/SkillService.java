package com.lifetracker.service;

import com.lifetracker.entity.Goal;
import com.lifetracker.entity.Skill;
import com.lifetracker.entity.SkillProgress;
import com.lifetracker.entity.User;
import com.lifetracker.entity.enums.GoalStatus;
import com.lifetracker.entity.enums.SkillLevel;
import com.lifetracker.repository.GoalRepository;
import com.lifetracker.repository.SkillProgressRepository;
import com.lifetracker.repository.SkillRepository;
import com.lifetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SkillService {
    
    private final SkillRepository skillRepository;
    private final SkillProgressRepository skillProgressRepository;
    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    
    // ===== SKILL METHODS =====
    
    public Skill createSkill(Long userId, Skill skill) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        skill.setUser(user);
        if (skill.getLevel() == null) skill.setLevel(SkillLevel.BEGINNER);
        return skillRepository.save(skill);
    }
    
    public List<Skill> getSkillsByUserId(Long userId) {
        return skillRepository.findByUserUserId(userId);
    }
    
    public List<Skill> getSkillsByLevel(Long userId, SkillLevel level) {
        return skillRepository.findByUserUserIdAndLevel(userId, level);
    }
    
    public List<Skill> searchSkills(Long userId, String searchTerm) {
        return skillRepository.findByUserUserIdAndSkillNameContainingIgnoreCase(userId, searchTerm);
    }
    
    public Optional<Skill> getSkillById(Long skillId) {
        return skillRepository.findById(skillId);
    }
    
    public Skill updateSkill(Skill skill) {
        return skillRepository.save(skill);
    }
    
    public void deleteSkill(Long skillId) {
        skillRepository.deleteById(skillId);
    }
    
    // ===== SKILL PROGRESS METHODS =====
    
    public SkillProgress addProgress(Long skillId, SkillProgress progress) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found"));
        progress.setSkill(skill);
        return skillProgressRepository.save(progress);
    }
    
    public List<SkillProgress> getProgressBySkillId(Long skillId) {
        return skillProgressRepository.findBySkillSkillIdOrderByProgressDateDesc(skillId);
    }
    
    public List<SkillProgress> getProgressByDateRange(Long skillId, LocalDate startDate, LocalDate endDate) {
        return skillProgressRepository.findBySkillSkillIdAndProgressDateBetween(skillId, startDate, endDate);
    }
    
    public BigDecimal getTotalHoursSpent(Long skillId) {
        BigDecimal total = skillProgressRepository.getTotalHoursSpentBySkillId(skillId);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    public Map<LocalDate, BigDecimal> getProgressOverTime(Long skillId) {
        List<Object[]> results = skillProgressRepository.getProgressOverTime(skillId);
        return results.stream()
                .collect(Collectors.toMap(
                        r -> (LocalDate) r[0],
                        r -> (BigDecimal) r[1]
                ));
    }
    
    public void deleteProgress(Long progressId) {
        skillProgressRepository.deleteById(progressId);
    }
    
    // ===== GOAL METHODS =====
    
    public Goal createGoal(Long userId, Goal goal) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        goal.setUser(user);
        if (goal.getStatus() == null) goal.setStatus(GoalStatus.NOT_STARTED);
        return goalRepository.save(goal);
    }
    
    public Goal createGoalForSkill(Long userId, Long skillId, Goal goal) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found"));
        
        goal.setUser(user);
        goal.setSkill(skill);
        if (goal.getStatus() == null) goal.setStatus(GoalStatus.NOT_STARTED);
        return goalRepository.save(goal);
    }
    
    public List<Goal> getGoalsByUserId(Long userId) {
        return goalRepository.findByUserUserId(userId);
    }
    
    public List<Goal> getGoalsByStatus(Long userId, GoalStatus status) {
        return goalRepository.findByUserUserIdAndStatus(userId, status);
    }
    
    public List<Goal> getGoalsBySkillId(Long skillId) {
        return goalRepository.findBySkillSkillId(skillId);
    }
    
    public Optional<Goal> getGoalById(Long goalId) {
        return goalRepository.findById(goalId);
    }
    
    public Goal updateGoal(Goal goal) {
        return goalRepository.save(goal);
    }
    
    public Goal updateGoalStatus(Long goalId, GoalStatus status) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        goal.setStatus(status);
        return goalRepository.save(goal);
    }
    
    public void deleteGoal(Long goalId) {
        goalRepository.deleteById(goalId);
    }
    
    public Map<GoalStatus, Long> getGoalStatistics(Long userId) {
        List<Object[]> results = goalRepository.getGoalCountByStatus(userId);
        return results.stream()
                .collect(Collectors.toMap(
                        r -> (GoalStatus) r[0],
                        r -> (Long) r[1]
                ));
    }
}
