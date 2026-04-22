package com.lifetracker.controller;

import com.lifetracker.entity.Goal;
import com.lifetracker.entity.Skill;
import com.lifetracker.entity.SkillProgress;
import com.lifetracker.entity.enums.GoalStatus;
import com.lifetracker.entity.enums.SkillLevel;
import com.lifetracker.service.SkillService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {
    
    private final SkillService skillService;
    
    // ===== SKILL ENDPOINTS =====
    
    @GetMapping
    public ResponseEntity<List<Skill>> getSkills(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(skillService.getSkillsByUserId(userId));
    }
    
    @PostMapping
    public ResponseEntity<Skill> addSkill(@RequestBody Skill skill, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(skillService.createSkill(userId, skill));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Skill> updateSkill(@PathVariable Long id, @RequestBody Skill skill) {
        skill.setSkillId(id);
        return ResponseEntity.ok(skillService.updateSkill(skill));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        skillService.deleteSkill(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/level/{level}")
    public ResponseEntity<List<Skill>> getSkillsByLevel(@PathVariable SkillLevel level, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(skillService.getSkillsByLevel(userId, level));
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Skill>> searchSkills(@RequestParam String q, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(skillService.searchSkills(userId, q));
    }
    
    // ===== SKILL PROGRESS ENDPOINTS =====
    
    @GetMapping("/{skillId}/progress")
    public ResponseEntity<List<SkillProgress>> getSkillProgress(@PathVariable Long skillId) {
        return ResponseEntity.ok(skillService.getProgressBySkillId(skillId));
    }
    
    @PostMapping("/{skillId}/progress")
    public ResponseEntity<SkillProgress> addSkillProgress(@PathVariable Long skillId, @RequestBody SkillProgress progress) {
        return ResponseEntity.ok(skillService.addProgress(skillId, progress));
    }
    
    @DeleteMapping("/progress/{progressId}")
    public ResponseEntity<Void> deleteProgress(@PathVariable Long progressId) {
        skillService.deleteProgress(progressId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{skillId}/total-hours")
    public ResponseEntity<Map<String, Object>> getTotalHours(@PathVariable Long skillId) {
        Map<String, Object> response = new HashMap<>();
        response.put("totalHours", skillService.getTotalHoursSpent(skillId));
        return ResponseEntity.ok(response);
    }
    
    // ===== GOAL ENDPOINTS =====
    
    @GetMapping("/goals")
    public ResponseEntity<List<Goal>> getGoals(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(skillService.getGoalsByUserId(userId));
    }
    
    @PostMapping("/goals")
    public ResponseEntity<Goal> addGoal(@RequestBody Goal goal, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(skillService.createGoal(userId, goal));
    }
    
    @PostMapping("/{skillId}/goals")
    public ResponseEntity<Goal> addGoalForSkill(@PathVariable Long skillId, @RequestBody Goal goal, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(skillService.createGoalForSkill(userId, skillId, goal));
    }
    
    @PutMapping("/goals/{goalId}")
    public ResponseEntity<Goal> updateGoal(@PathVariable Long goalId, @RequestBody Goal goal) {
        goal.setGoalId(goalId);
        return ResponseEntity.ok(skillService.updateGoal(goal));
    }
    
    @PatchMapping("/goals/{goalId}/status")
    public ResponseEntity<Goal> updateGoalStatus(@PathVariable Long goalId, @RequestParam GoalStatus status) {
        return ResponseEntity.ok(skillService.updateGoalStatus(goalId, status));
    }
    
    @DeleteMapping("/goals/{goalId}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long goalId) {
        skillService.deleteGoal(goalId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/goals/status/{status}")
    public ResponseEntity<List<Goal>> getGoalsByStatus(@PathVariable GoalStatus status, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(skillService.getGoalsByStatus(userId, status));
    }
    
    // ===== SUMMARY =====
    
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSkillSummary(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalSkills", skillService.getSkillsByUserId(userId).size());
        summary.put("goalStats", skillService.getGoalStatistics(userId));
        
        return ResponseEntity.ok(summary);
    }
}
