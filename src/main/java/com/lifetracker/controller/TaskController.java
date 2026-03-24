package com.lifetracker.controller;

import com.lifetracker.entity.Task;
import com.lifetracker.entity.TimeLog;
import com.lifetracker.entity.enums.Priority;
import com.lifetracker.entity.enums.TaskStatus;
import com.lifetracker.service.TaskService;
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
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    
    private final TaskService taskService;
    
    @GetMapping
    public ResponseEntity<List<Task>> getTasks(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(taskService.getTasksByUserId(userId));
    }
    
    @PostMapping
    public ResponseEntity<Task> addTask(@RequestBody Task task, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(taskService.createTask(userId, task));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody Task task) {
        task.setTaskId(id);
        return ResponseEntity.ok(taskService.updateTask(task));
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<Task> updateTaskStatus(@PathVariable Long id, @RequestParam TaskStatus status) {
        return ResponseEntity.ok(taskService.updateTaskStatus(id, status));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Task>> getTasksByStatus(@PathVariable TaskStatus status, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(taskService.getTasksByStatus(userId, status));
    }
    
    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<Task>> getTasksByPriority(@PathVariable Priority priority, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(taskService.getTasksByPriority(userId, priority));
    }
    
    @GetMapping("/pending")
    public ResponseEntity<List<Task>> getPendingTasks(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(taskService.getPendingTasks(userId));
    }
    
    @GetMapping("/today")
    public ResponseEntity<List<Task>> getTodayTasks(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(taskService.getTasksByDate(userId, LocalDate.now()));
    }
    
    @GetMapping("/daterange")
    public ResponseEntity<List<Task>> getTasksByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(taskService.getTasksByDateRange(userId, startDate, endDate));
    }
    
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getTaskSummary(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("taskStats", taskService.getTaskStatistics(userId));
        summary.put("pendingCount", taskService.getPendingTasks(userId).size());
        
        return ResponseEntity.ok(summary);
    }
    
    // ===== TIME LOG ENDPOINTS =====
    
    @PostMapping("/{taskId}/timelog/start")
    public ResponseEntity<TimeLog> startTimeLog(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.startTimeLog(taskId));
    }
    
    @PostMapping("/timelog/{timeLogId}/stop")
    public ResponseEntity<TimeLog> stopTimeLog(@PathVariable Long timeLogId) {
        return ResponseEntity.ok(taskService.stopTimeLog(timeLogId));
    }
    
    @GetMapping("/{taskId}/timelogs")
    public ResponseEntity<List<TimeLog>> getTimeLogs(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.getTimeLogsByTaskId(taskId));
    }
}
