package com.lifetracker.service;

import com.lifetracker.entity.HealthActivity;
import com.lifetracker.entity.User;
import com.lifetracker.repository.HealthActivityRepository;
import com.lifetracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthServiceTest {

    @Mock
    private HealthActivityRepository healthActivityRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private HealthService healthService;

    private User user;
    private HealthActivity activity;

    @BeforeEach
    void setUp() {
        user = User.builder().userId(1L).build();
        activity = HealthActivity.builder()
                .activityType("Running")
                .duration(30)
                .caloriesBurned(300)
                .recordDate(LocalDate.now())
                .build();
    }

    @Test
    void addHealthActivity_shouldAddActivitySuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(healthActivityRepository.save(any(HealthActivity.class))).thenReturn(activity);

        HealthActivity result = healthService.addHealthActivity(1L, activity);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        verify(healthActivityRepository).save(activity);
    }

    @Test
    void addHealthActivity_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> healthService.addHealthActivity(1L, activity));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getTotalDuration_shouldReturnTotalDuration() {
        when(healthActivityRepository.getTotalDurationByUserId(1L)).thenReturn(150);

        Integer total = healthService.getTotalDuration(1L);

        assertEquals(150, total);
    }

    @Test
    void getTotalCaloriesBurned_shouldReturnTotalCalories() {
        when(healthActivityRepository.getTotalCaloriesBurnedByUserId(1L)).thenReturn(1000);

        Integer total = healthService.getTotalCaloriesBurned(1L);

        assertEquals(1000, total);
    }
}