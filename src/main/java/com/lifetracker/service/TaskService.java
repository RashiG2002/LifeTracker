package com.lifetracker.service;

import com.lifetracker.entity.Task;
import com.lifetracker.entity.TimeLog;
import com.lifetracker.entity.User;
import com.lifetracker.entity.enums.Priority;
import com.lifetracker.entity.enums.TaskStatus;
import com.lifetracker.repository.TaskRepository;
import com.lifetracker.repository.TimeLogRepository;
import com.lifetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final TimeLogRepository timeLogRepository;
    private final UserRepository userRepository;
    
    // ===== TASK METHODS =====
    
    public Task createTask(Long userId, Task task) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        task.setUser(user);
        if (task.getPriority() == null) task.setPriority(Priority.MEDIUM);
        if (task.getStatus() == null) task.setStatus(TaskStatus.PENDING);
        return taskRepository.save(task);
    }
    
    public List<Task> getTasksByUserId(Long userId) {
        return taskRepository.findByUserUserIdOrderByDueDateAsc(userId);
    }
    
    public List<Task> getTasksByStatus(Long userId, TaskStatus status) {
        return taskRepository.findByUserUserIdAndStatus(userId, status);
    }
    
    public List<Task> getTasksByPriority(Long userId, Priority priority) {
        return taskRepository.findByUserUserIdAndPriority(userId, priority);
    }
    
    public List<Task> getTasksByDate(Long userId, LocalDate date) {
        return taskRepository.findByUserUserIdAndDueDate(userId, date);
    }
    
    public List<Task> getTasksByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return taskRepository.findByUserUserIdAndDueDateBetween(userId, startDate, endDate);
    }
    
    public List<Task> getPendingTasks(Long userId) {
        return taskRepository.findByUserUserIdAndStatusNotOrderByDueDateAsc(userId, TaskStatus.COMPLETED);
    }
    
    public Optional<Task> getTaskById(Long taskId) {
        return taskRepository.findById(taskId);
    }
    
    public Task updateTask(Task task) {
        return taskRepository.save(task);
    }
    
    public Task updateTaskStatus(Long taskId, TaskStatus status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setStatus(status);
        return taskRepository.save(task);
    }
    
    public void deleteTask(Long taskId) {
        taskRepository.deleteById(taskId);
    }
    
    public Long getTaskCountByStatus(Long userId, TaskStatus status) {
        return taskRepository.countByUserIdAndStatus(userId, status);
    }
    
    public Map<TaskStatus, Long> getTaskStatistics(Long userId) {
        List<Object[]> results = taskRepository.getTaskCountByStatus(userId);
        return results.stream()
                .collect(Collectors.toMap(
                        r -> (TaskStatus) r[0],
                        r -> (Long) r[1]
                ));
    }
    
    // ===== TIME LOG METHODS =====
    
    public TimeLog startTimeLog(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        TimeLog timeLog = TimeLog.builder()
                .task(task)
                .startTime(LocalDateTime.now())
                .build();
        
        // Update task status to IN_PROGRESS if it's PENDING
        if (task.getStatus() == TaskStatus.PENDING) {
            task.setStatus(TaskStatus.IN_PROGRESS);
            taskRepository.save(task);
        }
        
        return timeLogRepository.save(timeLog);
    }
    
    public TimeLog stopTimeLog(Long timeLogId) {
        TimeLog timeLog = timeLogRepository.findById(timeLogId)
                .orElseThrow(() -> new RuntimeException("TimeLog not found"));
        
        timeLog.setEndTime(LocalDateTime.now());
        timeLog.calculateDuration();
        
        return timeLogRepository.save(timeLog);
    }
    
    public List<TimeLog> getTimeLogsByTaskId(Long taskId) {
        return timeLogRepository.findByTaskTaskIdOrderByStartTimeDesc(taskId);
    }
    
    public Integer getTotalTimeSpentOnTask(Long taskId) {
        Integer total = timeLogRepository.getTotalDurationByTaskId(taskId);
        return total != null ? total : 0;
    }
}
