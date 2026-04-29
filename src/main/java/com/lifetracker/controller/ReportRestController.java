package com.lifetracker.controller;

import com.lifetracker.dto.ChartDataDTO;
import com.lifetracker.service.HealthService;
import com.lifetracker.service.TaskService;
import com.lifetracker.service.TransactionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportRestController {

    private final TransactionService transactionService;
    private final TaskService taskService;
    private final HealthService healthService;

    @GetMapping("/expenses-pie")
    public ResponseEntity<ChartDataDTO> getExpensesPieChart(
            HttpSession session,
            @RequestParam(defaultValue = "all") String period) {
        
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        ReportWindow window = resolveReportWindow(period);
        Map<String, BigDecimal> expensesByCategory = window.isAllTime()
            ? transactionService.getExpensesByCategory(userId, true)
            : transactionService.getExpensesByCategory(userId, window.startDate(), window.endDate());
        
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        
        expensesByCategory.forEach((category, amount) -> {
            labels.add(category);
            data.add(amount.doubleValue());
        });

        ChartDataDTO chartData = ChartDataDTO.builder()
                .labels(labels)
                .data(data)
                .label("Expenses by Category")
                .build();

        return ResponseEntity.ok(chartData);
    }
    
    @GetMapping("/activity-pie")
    public ResponseEntity<ChartDataDTO> getActivityPieChart(
            HttpSession session,
            @RequestParam(defaultValue = "all") String period) {
            
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        ReportWindow window = resolveReportWindow(period);
        Map<String, Integer> activitySummary = window.isAllTime()
            ? healthService.getActivitySummary(userId)
            : healthService.getActivitySummary(userId, window.startDate(), window.endDate());
        
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        
        activitySummary.forEach((activity, minutes) -> {
            labels.add(activity);
            data.add(minutes.doubleValue());
        });

        ChartDataDTO chartData = ChartDataDTO.builder()
                .labels(labels)
                .data(data)
                .label("Activity Minutes by Type")
                .build();

        return ResponseEntity.ok(chartData);
    }

    private ReportWindow resolveReportWindow(String period) {
        return switch (period.toLowerCase()) {
            case "daily" -> new ReportWindow(LocalDate.now(), LocalDate.now());
            case "weekly" -> new ReportWindow(LocalDate.now().with(DayOfWeek.MONDAY), LocalDate.now());
            case "monthly" -> new ReportWindow(LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()), LocalDate.now());
            default -> new ReportWindow(null, null);
        };
    }

    private record ReportWindow(LocalDate startDate, LocalDate endDate) {
        boolean isAllTime() {
            return startDate == null || endDate == null;
        }
    }
}
