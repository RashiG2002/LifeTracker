package com.lifetracker.repository;

import com.lifetracker.entity.Task;
import com.lifetracker.entity.enums.Priority;
import com.lifetracker.entity.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    List<Task> findByUserUserIdOrderByDueDateAsc(Long userId);
    
    List<Task> findByUserUserIdAndStatus(Long userId, TaskStatus status);
    
    List<Task> findByUserUserIdAndPriority(Long userId, Priority priority);
    
    List<Task> findByUserUserIdAndDueDate(Long userId, LocalDate dueDate);
    
    List<Task> findByUserUserIdAndDueDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.user.userId = :userId AND t.status = :status")
    Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") TaskStatus status);
    
    @Query("SELECT t.status, COUNT(t) FROM Task t WHERE t.user.userId = :userId GROUP BY t.status")
    List<Object[]> getTaskCountByStatus(@Param("userId") Long userId);
    
    List<Task> findByUserUserIdAndStatusNotOrderByDueDateAsc(Long userId, TaskStatus status);
}
