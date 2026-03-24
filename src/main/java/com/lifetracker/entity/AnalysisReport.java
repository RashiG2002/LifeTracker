package com.lifetracker.entity;

import com.lifetracker.entity.enums.ReportType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 15)
    private ReportType reportType;
    
    @Column(name = "report_data", columnDefinition = "JSON")
    private String reportData;
    
    @Column(name = "generated_date")
    private LocalDateTime generatedDate;
    
    @PrePersist
    protected void onCreate() {
        generatedDate = LocalDateTime.now();
    }
}
