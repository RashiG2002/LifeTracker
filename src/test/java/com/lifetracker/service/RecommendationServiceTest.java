package com.lifetracker.service;

import com.lifetracker.entity.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private TaskService taskService;

    @Mock
    private HealthService healthService;

    @InjectMocks
    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        // Setup mocks
    }

    @Test
    void generateRecommendations_shouldGenerateFinancialRecommendations() {
        when(transactionService.getTotalIncome(1L)).thenReturn(BigDecimal.valueOf(1000.00));
        when(transactionService.getTotalExpenses(1L)).thenReturn(BigDecimal.valueOf(900.00));
        when(taskService.getPendingTasks(1L)).thenReturn(Arrays.asList());
        when(healthService.getTotalDuration(1L)).thenReturn(200);

        List<String> recommendations = recommendationService.generateRecommendations(1L);

        assertTrue(recommendations.size() > 0);
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("saving")));
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("caught up")));
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("Excellent work")));
    }

    @Test
    void generateRecommendations_shouldWarnAboutOverspending() {
        when(transactionService.getTotalIncome(1L)).thenReturn(BigDecimal.valueOf(1000.00));
        when(transactionService.getTotalExpenses(1L)).thenReturn(BigDecimal.valueOf(1100.00));
        when(taskService.getPendingTasks(1L)).thenReturn(Arrays.asList());
        when(healthService.getTotalDuration(1L)).thenReturn(100);

        List<String> recommendations = recommendationService.generateRecommendations(1L);

        assertTrue(recommendations.stream().anyMatch(r -> r.contains("expenses exceed")));
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("150 minutes")));
    }

    @Test
    void generateRecommendations_shouldRecommendTaskManagement() {
        when(transactionService.getTotalIncome(1L)).thenReturn(BigDecimal.valueOf(1000.00));
        when(transactionService.getTotalExpenses(1L)).thenReturn(BigDecimal.valueOf(800.00));
        when(taskService.getPendingTasks(1L)).thenReturn(Arrays.asList(new Task(), new Task(), new Task(), new Task(), new Task(), new Task()));
        when(healthService.getTotalDuration(1L)).thenReturn(100);

        List<String> recommendations = recommendationService.generateRecommendations(1L);

        assertTrue(recommendations.stream().anyMatch(r -> r.contains("pending tasks")));
    }
}