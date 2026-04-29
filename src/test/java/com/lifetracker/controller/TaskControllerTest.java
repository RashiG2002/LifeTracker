package com.lifetracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifetracker.entity.Task;
import com.lifetracker.entity.TimeLog;
import com.lifetracker.entity.enums.Priority;
import com.lifetracker.entity.enums.TaskStatus;
import com.lifetracker.service.TaskService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @Autowired
    private ObjectMapper objectMapper;

    private Task task;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        task = Task.builder()
                .taskId(1L)
                .title("Test Task")
                .priority(Priority.MEDIUM)
                .status(TaskStatus.PENDING)
                .dueDate(LocalDate.now().plusDays(1))
                .build();
        session = new MockHttpSession();
        session.setAttribute("userId", 1L);
    }

    @Test
    void getTasks_shouldReturnTasks() throws Exception {
        List<Task> tasks = Arrays.asList(task);
        when(taskService.getTasksByUserId(1L)).thenReturn(tasks);

        mockMvc.perform(get("/api/tasks").session((MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Task"));
    }

    @Test
    void addTask_shouldCreateTask() throws Exception {
        when(taskService.createTask(anyLong(), any(Task.class))).thenReturn(task);

        mockMvc.perform(post("/api/tasks")
                .session((MockHttpSession) session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Task"));
    }

    @Test
    void addTask_shouldReturn401WhenNoSession() throws Exception {
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateTaskStatus_shouldUpdateStatus() throws Exception {
        when(taskService.updateTaskStatus(1L, TaskStatus.COMPLETED)).thenReturn(task);

        mockMvc.perform(patch("/api/tasks/1/status")
                .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING")); // since task is PENDING
    }

    @Test
    void deleteTask_shouldDelete() throws Exception {
        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isOk());
    }
}