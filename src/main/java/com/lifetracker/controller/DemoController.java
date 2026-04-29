package com.lifetracker.controller;

import com.lifetracker.entity.*;
import com.lifetracker.entity.enums.Priority;
import com.lifetracker.entity.enums.SkillLevel;
import com.lifetracker.entity.enums.TaskStatus;
import com.lifetracker.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class DemoController {

    private final UserService userService;
    private final TransactionService transactionService;
    private final TaskService taskService;
    private final HealthService healthService;
    private final SkillService skillService;

    @GetMapping("/generate-demo")
    public String generateDemoData(HttpSession session, RedirectAttributes redirectAttributes) {
        Random random = new Random();
        String randomSuffix = UUID.randomUUID().toString().substring(0, 5);

        // 1. Fetch or Create the specific user
        User demoUser;
        try {
            demoUser = userService.findByEmail("it24101015@my.sliit.lk").orElseGet(() -> {
                User newUser = User.builder()
                        .name("amantha")
                        .email("it24101015@my.sliit.lk")
                        .password("1234qwe")
                        .build();
                return userService.registerUser(newUser);
            });
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error setting up test user: " + e.getMessage());
            return "redirect:/login";
        }

        Long userId = demoUser.getUserId();

        // 2. Add 10 Transactions (Mixed Incomes and Expenses)
        String[] expenseCategories = {"Food", "Transport", "Entertainment", "Utilities", "Shopping"};
        String[] incomeSources = {"Salary", "Freelance", "Investment", "Gift"};

        for (int i = 0; i < 10; i++) {
            if (i < 3) {
                // 3 Incomes
                Income inc = Income.builder()
                        .amount(BigDecimal.valueOf(1000 + random.nextInt(4000))) // 1000 to 5000
                        .source(incomeSources[random.nextInt(incomeSources.length)])
                        .description("Demo Income " + i)
                        .incomeDate(LocalDate.now().minusDays(random.nextInt(30)))
                        .build();
                transactionService.addIncome(userId, inc);
            } else {
                // 7 Expenses
                Expense exp = Expense.builder()
                        .amount(BigDecimal.valueOf(50 + random.nextInt(450))) // 50 to 500
                        .category(expenseCategories[random.nextInt(expenseCategories.length)])
                        .description("Demo Expense " + i)
                        .expenseDate(LocalDate.now().minusDays(random.nextInt(30)))
                        .build();
                transactionService.addExpense(userId, exp);
            }
        }

        // 3. Add 10 Tasks
        String[] taskTitles = {"Buy groceries", "Finish report", "Call mom", "Pay bills", "Workout", "Read book", "Clean house", "Fix sink", "Book flights", "Prepare presentation"};
        for (int i = 0; i < 10; i++) {
            TaskStatus status = random.nextBoolean() ? TaskStatus.COMPLETED : (random.nextBoolean() ? TaskStatus.IN_PROGRESS : TaskStatus.PENDING);
            Priority priority = Priority.values()[random.nextInt(Priority.values().length)];
            
            Task t = Task.builder()
                    .title(taskTitles[i] + " " + randomSuffix)
                    .description("Auto-generated demo task.")
                    .priority(priority)
                    .status(status)
                    .dueDate(LocalDate.now().plusDays(random.nextInt(14) - 7)) // due between -7 and +7 days from now
                    .build();
            taskService.createTask(userId, t);
        }

        // 4. Log 10 Health Activities
        String[] activityTypes = {"Running", "Cycling", "Swimming", "Weightlifting", "Yoga", "Walking"};
        for (int i = 0; i < 10; i++) {
            int duration = 15 + random.nextInt(46); // 15 to 60 mins
            int calories = duration * (5 + random.nextInt(6)); // Rough estimate
            
            HealthActivity ha = HealthActivity.builder()
                    .activityType(activityTypes[random.nextInt(activityTypes.length)])
                    .duration(duration)
                    .caloriesBurned(calories)
                    .notes("Demo activity session")
                    .recordDate(LocalDate.now().minusDays(random.nextInt(30)))
                    .build();
            healthService.addHealthActivity(userId, ha);
        }

        // 5. Track 10 Skills & Progress (Create 3 Skills, add multiple progress logs to make up 10 logs total)
        String[] skillNames = {"Java Programming", "Guitar", "Spanish Language", "Cooking"};
        
        for(int s = 0; s < Math.min(3, skillNames.length); s++) {
            Skill skill = Skill.builder()
                    .skillName(skillNames[s])
                    .level(SkillLevel.values()[random.nextInt(SkillLevel.values().length)])
                    .build();
            skill = skillService.createSkill(userId, skill);
            
            // Add progress logs for each skill (roughly 10 total across all skills)
            int logsToAdd = (s == 0) ? 4 : 3; 
            for (int p = 0; p < logsToAdd; p++) {
                SkillProgress sp = SkillProgress.builder()
                        .hoursSpent(BigDecimal.valueOf(1 + random.nextDouble() * 2)) // 1 to 3 hours
                        .notes("Practiced " + skillNames[s])
                        .progressDate(LocalDate.now().minusDays(random.nextInt(30)))
                        .build();
                skillService.addProgress(skill.getSkillId(), sp);
            }
        }

        // Automatically log the demo user in
        session.setAttribute("userId", demoUser.getUserId());
        session.setAttribute("userName", demoUser.getName());
        
        redirectAttributes.addFlashAttribute("success", "Demo User created and populated with data! Logged in as " + demoUser.getName());
        return "redirect:/dashboard";
    }
}
