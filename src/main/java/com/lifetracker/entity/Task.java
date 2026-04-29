package com.lifetracker.entity;

import com.lifetracker.entity.enums.Priority;
import com.lifetracker.entity.enums.TaskStatus;
import com.lifetracker.validation.FutureDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FutureDateTime(message = "Please enter future date and time")
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 100)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;
    
    @Column(name = "due_date")
    @Future(message = "Due date must be in the future")
    private LocalDate dueDate;
    
    @Column(name = "due_time")
    private LocalTime dueTime;
    
    @Column(name = "location", length = 100)
    private String location;
    
    @Column(name = "is_outdoor")
    @Builder.Default
    private Boolean isOutdoor = false;
    
    @Column(name = "weather_condition")
    private String weatherCondition;
    
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TimeLog> timeLogs;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (priority == null) priority = Priority.MEDIUM;
        if (status == null) status = TaskStatus.PENDING;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
