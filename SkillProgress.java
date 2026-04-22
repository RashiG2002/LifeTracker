package com.lifetracker.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "skill_progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "progress_id")
    private Long progressId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;
    
    @Column(name = "hours_spent", nullable = false, precision = 5, scale = 2)
    private BigDecimal hoursSpent;
    
    @Column(length = 255)
    private String notes;
    
    @Column(name = "progress_date", nullable = false)
    private LocalDate progressDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
