package com.lifetracker.system;

import com.lifetracker.entity.Task;
import com.lifetracker.entity.User;
import com.lifetracker.entity.enums.Priority;
import com.lifetracker.entity.enums.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class SystemIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void fullSystemTest_userRegistrationLoginAndTaskManagement() {
        // 1. Register a new user
        MultiValueMap<String, String> registerData = new LinkedMultiValueMap<>();
        registerData.add("name", "Test User");
        registerData.add("email", "test@example.com");
        registerData.add("password", "password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> registerEntity = new HttpEntity<>(registerData, headers);

        ResponseEntity<String> registerResponse = restTemplate.postForEntity("/register", registerEntity, String.class);
        assertEquals(HttpStatus.OK, registerResponse.getStatusCode()); // Follows redirect to login page

        // 2. Login
        MultiValueMap<String, String> loginData = new LinkedMultiValueMap<>();
        loginData.add("email", "test@example.com");
        loginData.add("password", "password123");

        HttpEntity<MultiValueMap<String, String>> loginEntity = new HttpEntity<>(loginData, headers);
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/login", loginEntity, String.class);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode()); // Follows redirect to dashboard

        // 3. Create a task (requires session, TestRestTemplate maintains cookies)
        Map<String, Object> taskData = Map.of(
                "title", "Integration Test Task",
                "description", "Testing the full system",
                "priority", "HIGH",
                "dueDate", LocalDate.now().plusDays(7).toString()
        );

        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> taskEntity = new HttpEntity<>(taskData, jsonHeaders);

        ResponseEntity<Task> taskResponse = restTemplate.postForEntity("/api/tasks", taskEntity, Task.class);
        assertEquals(HttpStatus.OK, taskResponse.getStatusCode());
        assertNotNull(taskResponse.getBody());
        assertEquals("Integration Test Task", taskResponse.getBody().getTitle());
        assertEquals(TaskStatus.PENDING, taskResponse.getBody().getStatus());

        // 4. Get tasks
        ResponseEntity<Task[]> tasksResponse = restTemplate.getForEntity("/api/tasks", Task[].class);
        assertEquals(HttpStatus.OK, tasksResponse.getStatusCode());
        assertTrue(tasksResponse.getBody().length > 0);

        // 5. Update task status
        Long taskId = taskResponse.getBody().getTaskId();
        ResponseEntity<Task> updateResponse = restTemplate.exchange(
                "/api/tasks/" + taskId + "/status?status=COMPLETED",
                HttpMethod.PATCH,
                null,
                Task.class);
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals(TaskStatus.COMPLETED, updateResponse.getBody().getStatus());

        // 6. Delete task
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/tasks/" + taskId,
                HttpMethod.DELETE,
                null,
                Void.class);
        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    }
}