package com.lifetracker.repository;

import com.lifetracker.entity.HealthActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HealthActivityRepository extends JpaRepository<HealthActivity, Long> {
    
    List<HealthActivity> findByUserUserIdOrderByRecordDateDesc(Long userId);
    
    List<HealthActivity> findByUserUserIdAndActivityType(Long userId, String activityType);
    
    List<HealthActivity> findByUserUserIdAndRecordDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT SUM(h.duration) FROM HealthActivity h WHERE h.user.userId = :userId")
    Integer getTotalDurationByUserId(@Param("userId") Long userId);
    
    @Query("SELECT SUM(h.caloriesBurned) FROM HealthActivity h WHERE h.user.userId = :userId")
    Integer getTotalCaloriesBurnedByUserId(@Param("userId") Long userId);
    
    @Query("SELECT h.activityType, SUM(h.duration) FROM HealthActivity h WHERE h.user.userId = :userId GROUP BY h.activityType")
    List<Object[]> getActivitySummaryByUserId(@Param("userId") Long userId);
}
