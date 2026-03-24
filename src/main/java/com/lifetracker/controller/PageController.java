package com.lifetracker.controller;

import com.lifetracker.entity.User;
import com.lifetracker.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class PageController {
    
    private final UserService userService;
    private final TransactionService transactionService;
    private final BudgetService budgetService;
    private final HealthService healthService;
    private final TaskService taskService;
    private final SkillService skillService;
    private final RecommendationService recommendationService;
    
    // ===== AUTH PAGES =====
    
    @GetMapping("/")
    public String home(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/dashboard";
        }
        return "index";
    }
    
    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }
    
    @PostMapping("/login")
    public String login(@RequestParam String email, 
                       @RequestParam String password,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        return userService.loginUser(email, password)
                .map(user -> {
                    session.setAttribute("userId", user.getUserId());
                    session.setAttribute("userName", user.getName());
                    return "redirect:/dashboard";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Invalid email or password");
                    return "redirect:/login";
                });
    }
    
    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/dashboard";
        }
        return "register";
    }
    
    @PostMapping("/register")
    public String register(@ModelAttribute User user,
                          RedirectAttributes redirectAttributes) {
        try {
            userService.registerUser(user);
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }
    
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
    
    // ===== DASHBOARD =====
    
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        
        // Financial summary
        model.addAttribute("totalIncome", transactionService.getTotalIncome(userId));
        model.addAttribute("totalExpenses", transactionService.getTotalExpenses(userId));
        model.addAttribute("balance", transactionService.getCurrentBalance(userId));
        
        // Task summary
        model.addAttribute("pendingTasks", taskService.getPendingTasks(userId).size());
        model.addAttribute("taskStats", taskService.getTaskStatistics(userId));
        
        // Health summary
        model.addAttribute("totalActivityMinutes", healthService.getTotalDuration(userId));
        model.addAttribute("totalCaloriesBurned", healthService.getTotalCaloriesBurned(userId));
        
        // Skills count
        model.addAttribute("totalSkills", skillService.getSkillsByUserId(userId).size());
        
        return "dashboard";
    }
    
    // ===== TRANSACTION PAGES =====
    
    @GetMapping("/transactions")
    public String transactionsPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";
        
        model.addAttribute("expenses", transactionService.getExpensesByUserId(userId));
        model.addAttribute("incomes", transactionService.getIncomesByUserId(userId));
        model.addAttribute("totalIncome", transactionService.getTotalIncome(userId));
        model.addAttribute("totalExpenses", transactionService.getTotalExpenses(userId));
        model.addAttribute("balance", transactionService.getCurrentBalance(userId));
        
        return "transactions";
    }
    
    // ===== BUDGET PAGES =====
    
    @GetMapping("/budgets")
    public String budgetsPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";
        
        model.addAttribute("budgets", budgetService.getBudgetsByUserId(userId));
        model.addAttribute("activeBudgets", budgetService.getActiveBudgets(userId));
        
        return "budgets";
    }
    
    // ===== HEALTH PAGES =====
    
    @GetMapping("/health")
    public String healthPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";
        
        model.addAttribute("activities", healthService.getActivitiesByUserId(userId));
        model.addAttribute("totalDuration", healthService.getTotalDuration(userId));
        model.addAttribute("totalCalories", healthService.getTotalCaloriesBurned(userId));
        model.addAttribute("activitySummary", healthService.getActivitySummary(userId));
        
        return "health";
    }
    
    // ===== TASK PAGES =====
    
    @GetMapping("/tasks")
    public String tasksPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";
        
        model.addAttribute("tasks", taskService.getTasksByUserId(userId));
        model.addAttribute("pendingTasks", taskService.getPendingTasks(userId));
        model.addAttribute("taskStats", taskService.getTaskStatistics(userId));
        model.addAttribute("todayTasks", taskService.getTasksByDate(userId, LocalDate.now()));
        
        return "tasks";
    }
    
    // ===== SKILL PAGES =====
    
    @GetMapping("/skills")
    public String skillsPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";
        
        model.addAttribute("skills", skillService.getSkillsByUserId(userId));
        model.addAttribute("goals", skillService.getGoalsByUserId(userId));
        model.addAttribute("goalStats", skillService.getGoalStatistics(userId));
        model.addAttribute("activeGoals", skillService.getGoalsByUserId(userId).size());
        
        return "skills";
    }
    
    // ===== REPORTS PAGE =====
    
    @GetMapping("/reports")
    public String reportsPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";
        
        // Financial data
        model.addAttribute("totalIncome", transactionService.getTotalIncome(userId));
        model.addAttribute("totalExpenses", transactionService.getTotalExpenses(userId));
        model.addAttribute("expensesByCategory", transactionService.getExpensesByCategory(userId, true));
        model.addAttribute("incomeBySource", transactionService.getIncomeBySource(userId));
        
        // Task data
        model.addAttribute("taskStats", taskService.getTaskStatistics(userId));
        
        // Health data
        model.addAttribute("activitySummary", healthService.getActivitySummary(userId));
        
        // Skill data
        model.addAttribute("goalStats", skillService.getGoalStatistics(userId));
        
        // Recommendations
        model.addAttribute("recommendations", recommendationService.generateRecommendations(userId));
        
        return "reports";
    }
}
