package com.lifetracker.service;

import com.lifetracker.entity.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final TransactionService transactionService;
    private final TaskService taskService;
    private final HealthService healthService;

    public List<String> generateRecommendations(Long userId) {
        List<String> recommendations = new ArrayList<>();

        // Financial Recommendations
        BigDecimal totalIncome = transactionService.getTotalIncome(userId);
        BigDecimal totalExpenses = transactionService.getTotalExpenses(userId);
        
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal savingsRatio = totalIncome.subtract(totalExpenses)
                    .divide(totalIncome, 2, java.math.RoundingMode.HALF_UP);
            if (savingsRatio.compareTo(new BigDecimal("0.20")) < 0 && savingsRatio.compareTo(BigDecimal.ZERO) >= 0) {
                recommendations.add("Consider reducing non-essential expenses to hit a 20% savings goal.");
            } else if (savingsRatio.compareTo(BigDecimal.ZERO) < 0) {
                recommendations.add("Warning: Your expenses exceed your income. Review your budget categories!");
            } else {
                recommendations.add("Great job! You are saving a healthy percentage of your income.");
            }
        }

        // Task Recommendations
        List<Task> pendingTasks = taskService.getPendingTasks(userId);
        if (pendingTasks.size() > 5) {
            recommendations.add("You have " + pendingTasks.size() + " pending tasks. Try breaking them down into smaller, actionable steps.");
        } else if (pendingTasks.isEmpty()) {
            recommendations.add("You're all caught up on your tasks! Time to set some new goals.");
        }

        // Health Recommendations
        int totalMinutes = healthService.getTotalDuration(userId);
        if (totalMinutes < 150) {
            recommendations.add("You've logged " + totalMinutes + " minutes of physical activity. Aim for at least 150 minutes a week for optimal health.");
        } else {
            recommendations.add("Excellent work on your fitness! You've met the recommended weekly activity goals.");
        }

        return recommendations;
    }
}
