package com.lifetracker.service;

import com.lifetracker.entity.HealthActivity;
import com.lifetracker.entity.User;
import com.lifetracker.repository.HealthActivityRepository;
import com.lifetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class HealthService {
    
    private final HealthActivityRepository healthActivityRepository;
    private final UserRepository userRepository;
    
    public HealthActivity addHealthActivity(Long userId, HealthActivity activity) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        activity.setUser(user);
        return healthActivityRepository.save(activity);
    }
    
    public List<HealthActivity> getActivitiesByUserId(Long userId) {
        return healthActivityRepository.findByUserUserIdOrderByRecordDateDesc(userId);
    }
    
    public List<HealthActivity> getActivitiesByType(Long userId, String activityType) {
        return healthActivityRepository.findByUserUserIdAndActivityType(userId, activityType);
    }
    
    public List<HealthActivity> getActivitiesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return healthActivityRepository.findByUserUserIdAndRecordDateBetween(userId, startDate, endDate);
    }
    
    public Optional<HealthActivity> getActivityById(Long healthId) {
        return healthActivityRepository.findById(healthId);
    }
    
    public HealthActivity updateActivity(HealthActivity activity) {
        return healthActivityRepository.save(activity);
    }
    
    public void deleteActivity(Long healthId) {
        healthActivityRepository.deleteById(healthId);
    }
    
    public Integer getTotalDuration(Long userId) {
        Integer total = healthActivityRepository.getTotalDurationByUserId(userId);
        return total != null ? total : 0;
    }
    
    public Integer getTotalCaloriesBurned(Long userId) {
        Integer total = healthActivityRepository.getTotalCaloriesBurnedByUserId(userId);
        return total != null ? total : 0;
    }
    
    public Map<String, Integer> getActivitySummary(Long userId) {
        List<Object[]> results = healthActivityRepository.getActivitySummaryByUserId(userId);
        return results.stream()
                .collect(Collectors.toMap(
                        r -> (String) r[0],
                        r -> ((Number) r[1]).intValue()
                ));
    }
}
