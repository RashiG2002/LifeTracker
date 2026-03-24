package com.lifetracker.controller;

import com.lifetracker.entity.HealthActivity;
import com.lifetracker.entity.Skill;
import com.lifetracker.entity.SkillProgress;
import com.lifetracker.entity.Task;
import com.lifetracker.entity.enums.Priority;
import com.lifetracker.entity.enums.SkillLevel;
import com.lifetracker.entity.enums.TaskStatus;
import com.lifetracker.service.HealthService;
import com.lifetracker.service.SkillService;
import com.lifetracker.service.TaskService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Handles HTML form submissions for Tasks, Health, and Skills pages.
 * These endpoints accept POST from Thymeleaf forms and redirect back to the page.
 */
@Controller
@RequestMapping("/api")
@RequiredArgsConstructor
public class FormController {

    private final TaskService taskService;
    private final HealthService healthService;
    private final SkillService skillService;

    // ===================== TASK FORM ENDPOINTS =====================

    @PostMapping("/tasks/form")
    public String addTask(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(priority != null ? Priority.valueOf(priority) : Priority.MEDIUM);
        task.setStatus(TaskStatus.PENDING);
        task.setDueDate(dueDate);
        taskService.createTask(userId, task);

        redirectAttributes.addFlashAttribute("success", "Task added successfully!");
        return "redirect:/tasks";
    }

    @PostMapping("/tasks/form/{id}/status")
    public String updateTaskStatus(
            @PathVariable Long id,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            taskService.updateTaskStatus(id, TaskStatus.valueOf(status));
            redirectAttributes.addFlashAttribute("success", "Task status updated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update task: " + e.getMessage());
        }
        return "redirect:/tasks";
    }

    @PostMapping("/tasks/form/{id}/delete")
    public String deleteTask(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            taskService.deleteTask(id);
            redirectAttributes.addFlashAttribute("success", "Task deleted!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete task: " + e.getMessage());
        }
        return "redirect:/tasks";
    }

    // ===================== HEALTH FORM ENDPOINTS =====================

    @PostMapping("/health/form")
    public String addHealthActivity(
            @RequestParam String activityType,
            @RequestParam Integer duration,
            @RequestParam(required = false) Integer caloriesBurned,
            @RequestParam(required = false) String notes,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recordDate,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        HealthActivity activity = new HealthActivity();
        activity.setActivityType(activityType);
        activity.setDuration(duration);
        activity.setCaloriesBurned(caloriesBurned);
        activity.setNotes(notes);
        activity.setRecordDate(recordDate);
        healthService.addHealthActivity(userId, activity);

        redirectAttributes.addFlashAttribute("success", "Activity logged successfully!");
        return "redirect:/health";
    }

    @PostMapping("/health/form/{id}/delete")
    public String deleteHealthActivity(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            healthService.deleteActivity(id);
            redirectAttributes.addFlashAttribute("success", "Activity deleted!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete activity: " + e.getMessage());
        }
        return "redirect:/health";
    }

    // ===================== SKILL FORM ENDPOINTS =====================

    @PostMapping("/skills/form")
    public String addSkill(
            @RequestParam String skillName,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Integer targetHours,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        Skill skill = new Skill();
        skill.setSkillName(skillName);
        skill.setLevel(level != null ? SkillLevel.valueOf(level) : SkillLevel.BEGINNER);
        skill.setTargetHours(targetHours != null ? targetHours : 0);
        skillService.createSkill(userId, skill);

        redirectAttributes.addFlashAttribute("success", "Skill added successfully!");
        return "redirect:/skills";
    }

    @PostMapping("/skills/form/{id}/progress")
    public String addSkillProgress(
            @PathVariable Long id,
            @RequestParam BigDecimal hoursSpent,
            @RequestParam(required = false) String notes,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate progressDate,
            RedirectAttributes redirectAttributes) {
        try {
            SkillProgress progress = new SkillProgress();
            progress.setHoursSpent(hoursSpent);
            progress.setNotes(notes);
            progress.setProgressDate(progressDate);
            skillService.addProgress(id, progress);
            redirectAttributes.addFlashAttribute("success", "Practice session logged!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to log progress: " + e.getMessage());
        }
        return "redirect:/skills";
    }

    @PostMapping("/skills/form/{id}/delete")
    public String deleteSkill(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            skillService.deleteSkill(id);
            redirectAttributes.addFlashAttribute("success", "Skill deleted!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete skill: " + e.getMessage());
        }
        return "redirect:/skills";
    }
}
