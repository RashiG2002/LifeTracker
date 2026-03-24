package com.lifetracker.entity;

import com.lifetracker.entity.enums.SkillLevel;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "skills")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Skill {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skill_id")
    private Long skillId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private SkillLevel level = SkillLevel.BEGINNER;
    
    @Column(name = "target_hours")
    private Integer targetHours = 0;
    
    @OneToMany(mappedBy = "skill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SkillProgress> progressRecords;
    
    @OneToMany(mappedBy = "skill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Goal> goals;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (level == null) level = SkillLevel.BEGINNER;
        if (targetHours == null) targetHours = 0;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public double getTotalHoursSpent() {
        if (progressRecords == null || progressRecords.isEmpty()) {
            return 0.0;
        }
        return progressRecords.stream()
                .mapToDouble(p -> p.getHoursSpent().doubleValue())
                .sum();
    }
}
