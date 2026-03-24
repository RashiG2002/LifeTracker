package com.lifetracker.controller;

import com.lifetracker.entity.HealthActivity;
import com.lifetracker.service.HealthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {
    
    private final HealthService healthService;
    
    @GetMapping
    public ResponseEntity<List<HealthActivity>> getActivities(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(healthService.getActivitiesByUserId(userId));
    }
    
    @PostMapping
    public ResponseEntity<HealthActivity> addActivity(@RequestBody HealthActivity activity, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(healthService.addHealthActivity(userId, activity));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<HealthActivity> updateActivity(@PathVariable Long id, @RequestBody HealthActivity activity) {
        activity.setHealthId(id);
        return ResponseEntity.ok(healthService.updateActivity(activity));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivity(@PathVariable Long id) {
        healthService.deleteActivity(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/type/{type}")
    public ResponseEntity<List<HealthActivity>> getActivitiesByType(@PathVariable String type, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(healthService.getActivitiesByType(userId, type));
    }
    
    @GetMapping("/daterange")
    public ResponseEntity<List<HealthActivity>> getActivitiesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(healthService.getActivitiesByDateRange(userId, startDate, endDate));
    }
    
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getHealthSummary(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalDuration", healthService.getTotalDuration(userId));
        summary.put("totalCalories", healthService.getTotalCaloriesBurned(userId));
        summary.put("activitySummary", healthService.getActivitySummary(userId));
        
        return ResponseEntity.ok(summary);
    }
}
