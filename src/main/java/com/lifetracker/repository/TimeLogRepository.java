package com.lifetracker.repository;

import com.lifetracker.entity.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {
    
    List<TimeLog> findByTaskTaskIdOrderByStartTimeDesc(Long taskId);
    
    @Query("SELECT SUM(t.duration) FROM TimeLog t WHERE t.task.taskId = :taskId")
    Integer getTotalDurationByTaskId(@Param("taskId") Long taskId);
    
    @Query("SELECT SUM(t.duration) FROM TimeLog t WHERE t.task.user.userId = :userId")
    Integer getTotalTimeLoggedByUserId(@Param("userId") Long userId);
}
