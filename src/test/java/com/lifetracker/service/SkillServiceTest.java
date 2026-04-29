package com.lifetracker.service;

import com.lifetracker.entity.Goal;
import com.lifetracker.entity.Skill;
import com.lifetracker.entity.SkillProgress;
import com.lifetracker.entity.User;
import com.lifetracker.entity.enums.GoalStatus;
import com.lifetracker.entity.enums.SkillLevel;
import com.lifetracker.repository.GoalRepository;
import com.lifetracker.repository.SkillProgressRepository;
import com.lifetracker.repository.SkillRepository;
import com.lifetracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private SkillProgressRepository skillProgressRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SkillService skillService;

    private User user;
    private Skill skill;
    private SkillProgress progress;
    private Goal goal;

    @BeforeEach
    void setUp() {
        user = User.builder().userId(1L).build();
        skill = Skill.builder()
                .skillName("Java Programming")
                .level(SkillLevel.BEGINNER)
                .build();
        progress = SkillProgress.builder()
                .hoursSpent(BigDecimal.valueOf(2.5))
                .progressDate(LocalDate.now())
                .build();
        goal = Goal.builder()
                .description("Learn Spring Boot")
                .targetDate(LocalDate.now().plusMonths(3))
                .build();
    }

    @Test
    void createSkill_shouldCreateSkillWithDefaults() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(skillRepository.save(any(Skill.class))).thenReturn(skill);

        Skill result = skillService.createSkill(1L, skill);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertEquals(SkillLevel.BEGINNER, result.getLevel());
        verify(skillRepository).save(skill);
    }

    @Test
    void createSkill_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> skillService.createSkill(1L, skill));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void addProgress_shouldAddProgressSuccessfully() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));
        when(skillProgressRepository.save(any(SkillProgress.class))).thenReturn(progress);

        SkillProgress result = skillService.addProgress(1L, progress);

        assertNotNull(result);
        assertEquals(skill, result.getSkill());
        verify(skillProgressRepository).save(progress);
    }

    @Test
    void createGoal_shouldCreateGoalWithDefaults() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(goalRepository.save(any(Goal.class))).thenReturn(goal);

        Goal result = skillService.createGoal(1L, goal);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertEquals(GoalStatus.NOT_STARTED, result.getStatus());
        verify(goalRepository).save(goal);
    }

    @Test
    void updateGoalStatus_shouldUpdateStatus() {
        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(Goal.class))).thenReturn(goal);

        Goal result = skillService.updateGoalStatus(1L, GoalStatus.IN_PROGRESS);

        assertEquals(GoalStatus.IN_PROGRESS, result.getStatus());
        verify(goalRepository).save(goal);
    }
}