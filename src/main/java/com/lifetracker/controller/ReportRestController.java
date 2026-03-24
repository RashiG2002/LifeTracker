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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        // For simplicity, using the existing getExpensesByCategory. 
        // In a real app, we'd filter by 'period' (daily/weekly/monthly).
        Map<String, BigDecimal> expensesByCategory = transactionService.getExpensesByCategory(userId, true);
        
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

        Map<String, Integer> activitySummary = healthService.getActivitySummary(userId);
        
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
}
