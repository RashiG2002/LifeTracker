package com.lifetracker.repository;

import com.lifetracker.entity.AnalysisReport;
import com.lifetracker.entity.enums.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {
    
    List<AnalysisReport> findByUserUserIdOrderByGeneratedDateDesc(Long userId);
    
    List<AnalysisReport> findByUserUserIdAndReportType(Long userId, ReportType reportType);
}
