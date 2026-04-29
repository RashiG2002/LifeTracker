package com.lifetracker.service;

import com.lifetracker.entity.Task;
import com.lifetracker.entity.User;
import com.lifetracker.entity.enums.Priority;
import com.lifetracker.entity.enums.TaskStatus;
import com.lifetracker.repository.TaskRepository;
import com.lifetracker.repository.TimeLogRepository;
import com.lifetracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TimeLogRepository timeLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    private User user;
    private Task task;

    @BeforeEach
    void setUp() {
        user = User.builder().userId(1L).build();
        task = Task.builder().title("Test Task").build();
    }

    @Test
    void createTask_shouldCreateTaskWithDefaults() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Act
        Task result = taskService.createTask(1L, task);

        // Assert
        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertEquals(Priority.MEDIUM, result.getPriority());
        assertEquals(TaskStatus.PENDING, result.getStatus());
        verify(taskRepository).save(task);
    }

    @Test
    void createTask_shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> taskService.createTask(1L, task));
        assertEquals("User not found", exception.getMessage());
    }
}