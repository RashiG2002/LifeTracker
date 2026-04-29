package com.lifetracker.controller;

import com.lifetracker.dto.BudgetSnapshotDTO;
import com.lifetracker.dto.GuidedActionDTO;
import com.lifetracker.dto.ReportInsightDTO;
import com.lifetracker.entity.Expense;
import com.lifetracker.entity.Task;
import com.lifetracker.entity.User;
import com.lifetracker.entity.enums.Priority;
import com.lifetracker.entity.enums.TaskStatus;
import com.lifetracker.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
    private final WeatherService weatherService;
    
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
    public String budgetsPage(
            @RequestParam(required = false) String month,
            HttpSession session, 
            Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";
        
        // Default to current month if not specified
        java.time.YearMonth selectedMonth = java.time.YearMonth.now();
        if (month != null && !month.isEmpty()) {
            try {
                selectedMonth = java.time.YearMonth.parse(month);
            } catch (Exception e) {
                selectedMonth = java.time.YearMonth.now();
            }
        }
        
        // Get budgets and financial summary
        model.addAttribute("budgets", budgetService.getBudgetsByUserId(userId));
        model.addAttribute("activeBudgets", budgetService.getActiveBudgets(userId));
        
        // Get monthly financial data
        var financialSummary = budgetService.getMonthlyFinancialSummary(userId, selectedMonth);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("monthlyIncome", financialSummary.get("totalIncome"));
        model.addAttribute("monthlyExpenses", financialSummary.get("totalExpenses"));
        model.addAttribute("monthlyBalance", financialSummary.get("balance"));
        model.addAttribute("monthlyIncomes", financialSummary.get("incomes"));
        model.addAttribute("monthlyExpensesList", financialSummary.get("expenses"));
        
        // Previous month for navigation
        model.addAttribute("previousMonth", selectedMonth.minusMonths(1));
        model.addAttribute("nextMonth", selectedMonth.plusMonths(1));
        
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
        
        // Add weather data for each outdoor task
        var tasks = taskService.getTasksByUserId(userId);
        if (tasks != null) {
            for (var task : tasks) {
                if (task.getIsOutdoor() != null && task.getIsOutdoor() && task.getLocation() != null) {
                    // Weather data can be accessed if needed for outdoor tasks
                    weatherService.getWeatherByLocation(task.getLocation());
                }
            }
        }
        
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
    public String reportsPage(HttpSession session, Model model,
                              @RequestParam(defaultValue = "all") String period) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        ReportWindow window = resolveReportWindow(period);
        boolean allTime = window.isAllTime();

        TrendWindow trendWindow = resolveTrendWindow(window);

        ReportMetrics currentMetrics = buildMetrics(userId, window, allTime);
        ReportMetrics trendCurrentMetrics = buildMetrics(userId, trendWindow.currentWindow(), false);
        ReportMetrics trendPreviousMetrics = buildMetrics(userId, trendWindow.previousWindow(), false);

        BigDecimal totalIncome = currentMetrics.totalIncome();
        BigDecimal totalExpenses = currentMetrics.totalExpenses();
        BigDecimal totalSavings = currentMetrics.totalSavings();
        BigDecimal savingsRate = currentMetrics.savingsRate();

        Map<String, BigDecimal> expensesByCategory = currentMetrics.expensesByCategory();
        Map<String, BigDecimal> incomeBySource = currentMetrics.incomeBySource();
        Map.Entry<String, BigDecimal> topIncomeSource = incomeBySource.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);

        Map<TaskStatus, Long> taskStats = currentMetrics.taskStats();
        BigDecimal taskCompletionRate = currentMetrics.taskCompletionRate();
        long pendingCount = taskStats.getOrDefault(TaskStatus.PENDING, 0L);
        long inProgressCount = taskStats.getOrDefault(TaskStatus.IN_PROGRESS, 0L);
        long completedCount = taskStats.getOrDefault(TaskStatus.COMPLETED, 0L);

        List<Task> allTasks = taskService.getTasksByUserId(userId);
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);
        long overdueCount = allTasks.stream()
            .filter(task -> task.getDueDate() != null
                && task.getDueDate().isBefore(today)
                && task.getStatus() != TaskStatus.COMPLETED)
            .count();
        long dueTodayCount = allTasks.stream()
            .filter(task -> task.getDueDate() != null
                && task.getDueDate().isEqual(today)
                && task.getStatus() != TaskStatus.COMPLETED)
            .count();
        long dueThisWeekCount = allTasks.stream()
            .filter(task -> task.getDueDate() != null
                && !task.getDueDate().isBefore(today)
                && !task.getDueDate().isAfter(weekEnd)
                && task.getStatus() != TaskStatus.COMPLETED)
            .count();
        long highPriorityOpenCount = allTasks.stream()
            .filter(task -> task.getPriority() == Priority.HIGH
                && task.getStatus() != TaskStatus.COMPLETED)
            .count();
        List<Task> upcomingTasks = allTasks.stream()
            .filter(task -> task.getDueDate() != null
                && !task.getDueDate().isBefore(today)
                && task.getStatus() != TaskStatus.COMPLETED)
            .sorted(Comparator.comparing(Task::getDueDate))
            .limit(5)
            .toList();

        Integer totalActivityMinutes = currentMetrics.totalActivityMinutes();
        Integer totalCaloriesBurned = currentMetrics.totalCaloriesBurned();
        Map<String, Integer> activitySummary = currentMetrics.activitySummary();

        List<BudgetSnapshotDTO> budgetSnapshots = buildBudgetSnapshots(userId);
        List<GuidedActionDTO> guidedActions = buildGuidedActions(
            savingsRate,
            taskCompletionRate,
            overdueCount,
            highPriorityOpenCount,
            totalActivityMinutes,
            budgetSnapshots,
            skillService.getSkillsByUserId(userId).size(),
            skillService.getGoalStatistics(userId)
        );
        List<ReportInsightDTO> reportInsights = buildReportInsights(
            totalIncome,
            totalExpenses,
            totalSavings,
            savingsRate,
            expensesByCategory,
            taskCompletionRate,
            totalActivityMinutes,
            totalCaloriesBurned,
            budgetSnapshots
        );
        reportInsights.add(0, buildTrendInsight(trendCurrentMetrics, trendPreviousMetrics, trendWindow.label()));

        model.addAttribute("selectedPeriod", window.key());
        model.addAttribute("periodLabel", window.label());
        model.addAttribute("periodStart", window.startDate());
        model.addAttribute("periodEnd", window.endDate());
        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("totalSavings", totalSavings);
        model.addAttribute("savingsRate", savingsRate);
        model.addAttribute("expensesByCategory", expensesByCategory);
        model.addAttribute("incomeBySource", incomeBySource);
        model.addAttribute("topIncomeSourceName", topIncomeSource != null ? topIncomeSource.getKey() : null);
        model.addAttribute("taskStats", taskStats);
        model.addAttribute("taskCompletionRate", taskCompletionRate);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("inProgressCount", inProgressCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("dueTodayCount", dueTodayCount);
        model.addAttribute("dueThisWeekCount", dueThisWeekCount);
        model.addAttribute("highPriorityOpenCount", highPriorityOpenCount);
        model.addAttribute("upcomingTasks", upcomingTasks);
        model.addAttribute("activitySummary", activitySummary);
        model.addAttribute("totalActivityMinutes", totalActivityMinutes);
        model.addAttribute("totalCaloriesBurned", totalCaloriesBurned);
        model.addAttribute("budgetSnapshots", budgetSnapshots);
        model.addAttribute("guidedActions", guidedActions);
        model.addAttribute("reportInsights", reportInsights);
        model.addAttribute("comparisonLabel", trendWindow.label());
        model.addAttribute("comparisonCurrentLabel", trendWindow.currentWindow().label());
        model.addAttribute("comparisonPreviousLabel", trendWindow.previousWindow().label());
        model.addAttribute("incomeDelta", trendCurrentMetrics.totalIncome().subtract(trendPreviousMetrics.totalIncome()));
        model.addAttribute("expenseDelta", trendCurrentMetrics.totalExpenses().subtract(trendPreviousMetrics.totalExpenses()));
        model.addAttribute("savingsDelta", trendCurrentMetrics.totalSavings().subtract(trendPreviousMetrics.totalSavings()));
        model.addAttribute("taskCompletionDelta", trendCurrentMetrics.taskCompletionRate().subtract(trendPreviousMetrics.taskCompletionRate()));
        model.addAttribute("activityDelta", BigDecimal.valueOf(totalActivityMinutes != null ? totalActivityMinutes : 0)
            .subtract(BigDecimal.valueOf(trendPreviousMetrics.totalActivityMinutes() != null ? trendPreviousMetrics.totalActivityMinutes() : 0)));

        model.addAttribute("goalStats", skillService.getGoalStatistics(userId));
        model.addAttribute("recommendations", recommendationService.generateRecommendations(userId));
        
        return "reports";
    }
    
    // ===== SETTINGS PAGE =====
    
    @GetMapping("/settings")
    public String settingsPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";
        
        User user = userService.findById(userId).orElse(null);
        model.addAttribute("user", user);
        
        return "settings";
    }
    
    @PostMapping("/settings")
    public String updateSettings(@ModelAttribute User user, HttpSession session, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";
        
        User existingUser = userService.findById(userId).orElse(null);
        existingUser.setMaxDueDateDays(user.getMaxDueDateDays());
        existingUser.setNotificationHoursBefore(user.getNotificationHoursBefore());
        existingUser.setNotifyInProgress(user.getNotifyInProgress());
        existingUser.setNotifyComplete(user.getNotifyComplete());
        
        userService.updateUser(existingUser);
        redirectAttributes.addFlashAttribute("success", "Settings updated successfully!");
        
        return "redirect:/settings";
    }

    // ===== USER PROFILE PAGE =====
    
    @GetMapping("/profile")
    public String profilePage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";
        
        User user = userService.findById(userId).orElse(null);
        model.addAttribute("user", user);
        
        return "profile";
    }
    
    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute User user, HttpSession session, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";
        
        User existingUser = userService.findById(userId).orElse(null);
        existingUser.setName(user.getName());
        existingUser.setDateOfBirth(user.getDateOfBirth());
        
        userService.updateUser(existingUser);
        session.setAttribute("userName", existingUser.getName());
        redirectAttributes.addFlashAttribute("success", "Profile information updated successfully!");
        
        return "redirect:/profile";
    }
    
    @PostMapping("/profile/settings")
    public String updateProfileSettings(@RequestParam(required = false) Integer maxDueDateDays,
                                       @RequestParam(required = false) Integer notificationHoursBefore,
                                       @RequestParam(defaultValue = "false") Boolean notifyInProgress,
                                       @RequestParam(defaultValue = "false") Boolean notifyComplete,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";
        
        User existingUser = userService.findById(userId).orElse(null);
        
        if (maxDueDateDays != null) {
            existingUser.setMaxDueDateDays(maxDueDateDays);
        }
        if (notificationHoursBefore != null) {
            existingUser.setNotificationHoursBefore(notificationHoursBefore);
        }
        existingUser.setNotifyInProgress(notifyInProgress);
        existingUser.setNotifyComplete(notifyComplete);
        
        userService.updateUser(existingUser);
        redirectAttributes.addFlashAttribute("success", "Notification settings updated successfully!");
        
        return "redirect:/profile#settings-info";
    }

    private ReportMetrics buildMetrics(Long userId, ReportWindow window, boolean allTime) {
        BigDecimal totalIncome = allTime
            ? transactionService.getTotalIncome(userId)
            : transactionService.getTotalIncomeByDateRange(userId, window.startDate(), window.endDate());
        BigDecimal totalExpenses = allTime
            ? transactionService.getTotalExpenses(userId)
            : transactionService.getTotalExpensesByDateRange(userId, window.startDate(), window.endDate());
        BigDecimal totalSavings = totalIncome.subtract(totalExpenses);
        BigDecimal savingsRate = totalIncome.signum() > 0
            ? totalSavings.multiply(BigDecimal.valueOf(100)).divide(totalIncome, 1, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        Map<String, BigDecimal> expensesByCategory = allTime
            ? transactionService.getExpensesByCategory(userId, true)
            : transactionService.getExpensesByCategory(userId, window.startDate(), window.endDate());
        Map<String, BigDecimal> incomeBySource = allTime
            ? transactionService.getIncomeBySource(userId)
            : transactionService.getIncomeBySource(userId, window.startDate(), window.endDate());

        Map<TaskStatus, Long> taskStats = taskService.getTaskStatistics(userId);
        long totalTasks = taskStats.values().stream().mapToLong(Long::longValue).sum();
        long completedTasks = taskStats.getOrDefault(TaskStatus.COMPLETED, 0L);
        BigDecimal taskCompletionRate = totalTasks > 0
            ? BigDecimal.valueOf(completedTasks).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalTasks), 1, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        Integer totalActivityMinutes = allTime
            ? healthService.getTotalDuration(userId)
            : healthService.getTotalDuration(userId, window.startDate(), window.endDate());
        Integer totalCaloriesBurned = allTime
            ? healthService.getTotalCaloriesBurned(userId)
            : healthService.getTotalCaloriesBurned(userId, window.startDate(), window.endDate());
        Map<String, Integer> activitySummary = allTime
            ? healthService.getActivitySummary(userId)
            : healthService.getActivitySummary(userId, window.startDate(), window.endDate());

        return new ReportMetrics(
            totalIncome,
            totalExpenses,
            totalSavings,
            savingsRate,
            expensesByCategory,
            incomeBySource,
            taskStats,
            taskCompletionRate,
            totalActivityMinutes,
            totalCaloriesBurned,
            activitySummary
        );
    }

    private ReportInsightDTO buildTrendInsight(ReportMetrics currentMetrics,
                                               ReportMetrics previousMetrics,
                                               String label) {
        BigDecimal savingsDelta = currentMetrics.totalSavings().subtract(previousMetrics.totalSavings());
        BigDecimal expenseDelta = currentMetrics.totalExpenses().subtract(previousMetrics.totalExpenses());
        BigDecimal taskDelta = currentMetrics.taskCompletionRate().subtract(previousMetrics.taskCompletionRate());
        BigDecimal activityDelta = BigDecimal.valueOf(currentMetrics.totalActivityMinutes() != null ? currentMetrics.totalActivityMinutes() : 0)
            .subtract(BigDecimal.valueOf(previousMetrics.totalActivityMinutes() != null ? previousMetrics.totalActivityMinutes() : 0));

        boolean positiveTrend = savingsDelta.compareTo(BigDecimal.ZERO) >= 0;
        String message = (positiveTrend ? "Performance improved" : "Performance softened")
            + " versus " + label.toLowerCase() + ": savings "
            + (positiveTrend ? "increased" : "fell") + " by Rs. " + savingsDelta.abs().setScale(2, RoundingMode.HALF_UP)
            + ", expenses " + (expenseDelta.compareTo(BigDecimal.ZERO) <= 0 ? "decreased" : "increased")
            + " by Rs. " + expenseDelta.abs().setScale(2, RoundingMode.HALF_UP)
            + ", task completion moved by " + taskDelta.setScale(1, RoundingMode.HALF_UP) + " points"
            + " and activity changed by " + activityDelta.setScale(0, RoundingMode.HALF_UP) + " minutes.";

        return ReportInsightDTO.builder()
            .title("Period comparison")
            .message(message)
            .level(positiveTrend ? "success" : "warning")
            .icon("fa-chart-line")
            .build();
    }

    private List<GuidedActionDTO> buildGuidedActions(BigDecimal savingsRate,
                                                     BigDecimal taskCompletionRate,
                                                     long overdueCount,
                                                     long highPriorityOpenCount,
                                                     Integer totalActivityMinutes,
                                                     List<BudgetSnapshotDTO> budgetSnapshots,
                                                     int skillCount,
                                                     Map<?, Long> goalStats) {
        List<GuidedActionDTO> actions = new ArrayList<>();

        long overBudgetCount = budgetSnapshots.stream().filter(BudgetSnapshotDTO::isOverBudget).count();
        if (overBudgetCount > 0 || savingsRate.compareTo(BigDecimal.valueOf(20)) < 0) {
            actions.add(GuidedActionDTO.builder()
                .title("Budget Reset Drill")
                .category("Budget")
                .summary("Rebuild your spending guardrails with one category cap, one weekly review, and one cut rule.")
                .icon("fa-wallet")
                .level(overBudgetCount > 0 ? "danger" : "warning")
                .effort("15 min")
                .steps(List.of(
                    "Pick the category that is consuming the most money this period.",
                    "Set a hard weekly cap and write it down before the next purchase.",
                    "Move one optional expense to next week and review the result after 7 days."
                ))
                .build());
        } else {
            actions.add(GuidedActionDTO.builder()
                .title("Budget Maintenance Routine")
                .category("Budget")
                .summary("Keep the current spending pattern stable by reviewing your top category before the weekend.")
                .icon("fa-wallet")
                .level("success")
                .effort("10 min")
                .steps(List.of(
                    "Check the top three spending categories.",
                    "Confirm each one still matches a real priority.",
                    "Keep a small buffer for unplanned costs and avoid touching it."
                ))
                .build());
        }

        if (totalActivityMinutes == null || totalActivityMinutes < 150) {
            actions.add(GuidedActionDTO.builder()
                .title("Weekly Movement Plan")
                .category("Health")
                .summary("Create a simple activity loop that is easy to repeat and easy to measure.")
                .icon("fa-heart-pulse")
                .level("warning")
                .effort("20 min")
                .steps(List.of(
                    "Schedule three 20-minute walks on fixed days this week.",
                    "Add one strength or mobility session to avoid an all-cardio routine.",
                    "Log the activity immediately after finishing so progress is visible."
                ))
                .build());
        } else {
            actions.add(GuidedActionDTO.builder()
                .title("Health Momentum Loop")
                .category("Health")
                .summary("Protect your current activity level by using the same time slot each week.")
                .icon("fa-heart-pulse")
                .level("success")
                .effort("15 min")
                .steps(List.of(
                    "Keep one repeatable workout slot in your calendar.",
                    "Track calories or minutes after each session, not later in the day.",
                    "Pick one recovery habit, such as stretching or walking after meals."
                ))
                .build());
        }

        if (taskCompletionRate.compareTo(BigDecimal.valueOf(70)) < 0 || overdueCount > 0 || highPriorityOpenCount > 0) {
            actions.add(GuidedActionDTO.builder()
                .title("Task Focus Sprint")
                .category("Tasks")
                .summary("Use a short prioritization loop to clear overdue and high-priority tasks first.")
                .icon("fa-list-check")
                .level("warning")
                .effort("25 min")
                .steps(List.of(
                    "Select overdue tasks and high-priority items before anything else.",
                    "Work on one task for a 25-minute block with no switching.",
                    "Mark the task complete or split it into the next physical action."
                ))
                .build());
        } else {
            actions.add(GuidedActionDTO.builder()
                .title("Task Throughput Routine")
                .category("Tasks")
                .summary("Keep momentum by closing one small task early each day.")
                .icon("fa-list-check")
                .level("success")
                .effort("15 min")
                .steps(List.of(
                    "Start the day by finishing one low-friction task.",
                    "Keep all active work in one place so the queue stays visible.",
                    "Review unfinished items at the end of the day and re-order them."
                ))
                .build());
        }

        if (skillCount < 3 || goalStats == null || goalStats.isEmpty()) {
            actions.add(GuidedActionDTO.builder()
                .title("Skill Ladder Plan")
                .category("Skills")
                .summary("Choose one skill, attach one goal, and practice in short cycles so progress is measurable.")
                .icon("fa-graduation-cap")
                .level("warning")
                .effort("30 min")
                .steps(List.of(
                    "Pick one skill you actually use in work or daily life.",
                    "Set a simple goal with a weekly practice schedule and success criterion.",
                    "Record one proof of progress after each practice session."
                ))
                .build());
        } else {
            actions.add(GuidedActionDTO.builder()
                .title("Skill Growth Review")
                .category("Skills")
                .summary("Turn your current skills into a repeatable improvement cycle instead of collecting unfinished goals.")
                .icon("fa-graduation-cap")
                .level("success")
                .effort("20 min")
                .steps(List.of(
                    "Review active goals and remove anything not useful this month.",
                    "Increase the difficulty of one goal by one step only.",
                    "Log a short note on what improved and what still feels weak."
                ))
                .build());
        }

        return actions;
    }

    private List<BudgetSnapshotDTO> buildBudgetSnapshots(Long userId) {
        return budgetService.getActiveBudgets(userId).stream()
            .map(budget -> {
                BigDecimal spent = transactionService.getExpensesByDateRange(
                        userId,
                        budget.getStartDate(),
                        budget.getEndDate())
                    .stream()
                    .filter(expense -> expense.getCategory() != null && expense.getCategory().equalsIgnoreCase(budget.getCategory()))
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal limit = budget.getLimitAmount() != null ? budget.getLimitAmount() : BigDecimal.ZERO;
                BigDecimal remaining = limit.subtract(spent);
                BigDecimal utilization = limit.signum() > 0
                    ? spent.multiply(BigDecimal.valueOf(100)).divide(limit, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

                return BudgetSnapshotDTO.builder()
                    .category(budget.getCategory())
                    .limitAmount(limit)
                    .spentAmount(spent)
                    .remainingAmount(remaining)
                    .utilizationPercent(utilization)
                    .overBudget(spent.compareTo(limit) > 0)
                    .build();
            })
            .sorted(Comparator.comparing(BudgetSnapshotDTO::isOverBudget).reversed()
                .thenComparing(BudgetSnapshotDTO::getUtilizationPercent, Comparator.reverseOrder()))
            .toList();
        }

    private List<ReportInsightDTO> buildReportInsights(BigDecimal totalIncome,
                                                       BigDecimal totalExpenses,
                                                       BigDecimal totalSavings,
                                                       BigDecimal savingsRate,
                                                       Map<String, BigDecimal> expensesByCategory,
                                                       BigDecimal taskCompletionRate,
                                                       Integer totalActivityMinutes,
                                                       Integer totalCaloriesBurned,
                                                       List<BudgetSnapshotDTO> budgetSnapshots) {
        List<ReportInsightDTO> insights = new ArrayList<>();

        if (totalIncome.compareTo(BigDecimal.ZERO) > 0 && totalExpenses.compareTo(totalIncome) > 0) {
            insights.add(ReportInsightDTO.builder()
                .title("Spending is ahead of income")
                .message("Your expenses are currently higher than your income. Review discretionary categories before the next cycle ends.")
                .level("danger")
                .icon("fa-triangle-exclamation")
                .build());
        } else if (savingsRate.compareTo(BigDecimal.valueOf(20)) < 0) {
            insights.add(ReportInsightDTO.builder()
                .title("Savings rate is low")
                .message("You are saving " + savingsRate + "% of income. A practical target is 20% or more.")
                .level("warning")
                .icon("fa-piggy-bank")
                .build());
        } else {
            insights.add(ReportInsightDTO.builder()
                .title("Healthy savings behavior")
                .message("You kept " + savingsRate + "% of your income this period. That is a strong cash flow signal.")
                .level("success")
                .icon("fa-seedling")
                .build());
        }

        expensesByCategory.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .ifPresent(entry -> {
                BigDecimal share = totalExpenses.signum() > 0
                    ? entry.getValue().multiply(BigDecimal.valueOf(100)).divide(totalExpenses, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                insights.add(ReportInsightDTO.builder()
                    .title("Largest expense driver")
                    .message(entry.getKey() + " accounts for " + share + "% of spending. That is the first category to review for savings opportunities.")
                    .level(share.compareTo(BigDecimal.valueOf(35)) > 0 ? "warning" : "info")
                    .icon("fa-chart-pie")
                    .build());
            });

        if (totalActivityMinutes != null && totalActivityMinutes > 0) {
            insights.add(ReportInsightDTO.builder()
                .title("Activity progress")
                .message("You logged " + totalActivityMinutes + " active minutes and burned " + totalCaloriesBurned + " calories in this view.")
                .level(totalActivityMinutes >= 150 ? "success" : "warning")
                .icon("fa-heart-pulse")
                .build());
        }

        if (taskCompletionRate.compareTo(BigDecimal.valueOf(70)) < 0) {
            insights.add(ReportInsightDTO.builder()
                .title("Task completion needs attention")
                .message("Only " + taskCompletionRate + "% of tracked tasks are completed. Narrow the backlog into smaller actions.")
                .level("warning")
                .icon("fa-list-check")
                .build());
        }

        long overBudgetCount = budgetSnapshots.stream().filter(BudgetSnapshotDTO::isOverBudget).count();
        if (overBudgetCount > 0) {
            insights.add(ReportInsightDTO.builder()
                .title("Budget alerts")
                .message(overBudgetCount + " active budget" + (overBudgetCount == 1 ? " is" : "s are") + " above limit. Reallocate spending before the next cycle.")
                .level("danger")
                .icon("fa-wallet")
                .build());
        }

        if (insights.size() < 3) {
            insights.add(ReportInsightDTO.builder()
                .title("Next review step")
                .message("Compare this period with the previous one after a few more entries are logged. That will show whether your habits are improving.")
                .level("info")
                .icon("fa-magnifying-glass-chart")
                .build());
        }

        return insights;
    }

    private TrendWindow resolveTrendWindow(ReportWindow window) {
        if (window.isAllTime()) {
            LocalDate currentEnd = LocalDate.now();
            LocalDate currentStart = currentEnd.minusDays(29);
            LocalDate previousEnd = currentStart.minusDays(1);
            LocalDate previousStart = previousEnd.minusDays(29);
            return new TrendWindow(
                new ReportWindow("trend-current", "Last 30 Days", currentStart, currentEnd),
                new ReportWindow("trend-previous", "Previous 30 Days", previousStart, previousEnd),
                "Last 30 Days vs Previous 30 Days"
            );
        }

        long spanDays = window.endDate().toEpochDay() - window.startDate().toEpochDay() + 1;
        LocalDate previousEnd = window.startDate().minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(spanDays - 1);
        return new TrendWindow(
                window,
                new ReportWindow("previous-" + window.key(), "Previous " + window.label(), previousStart, previousEnd),
                window.label() + " vs Previous Period"
        );
    }

    private ReportWindow resolveReportWindow(String period) {
        return switch (period.toLowerCase()) {
            case "daily" -> new ReportWindow("daily", "Today", LocalDate.now(), LocalDate.now());
            case "weekly" -> new ReportWindow("weekly", "This Week", LocalDate.now().with(DayOfWeek.MONDAY), LocalDate.now());
            case "monthly" -> new ReportWindow("monthly", "This Month", LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()), LocalDate.now());
            default -> new ReportWindow("all", "All Time", null, null);
        };
    }

    private record ReportWindow(String key, String label, LocalDate startDate, LocalDate endDate) {
        boolean isAllTime() {
            return startDate == null || endDate == null;
        }
    }

    private record TrendWindow(ReportWindow currentWindow, ReportWindow previousWindow, String label) {
    }

    private record ReportMetrics(BigDecimal totalIncome,
                                 BigDecimal totalExpenses,
                                 BigDecimal totalSavings,
                                 BigDecimal savingsRate,
                                 Map<String, BigDecimal> expensesByCategory,
                                 Map<String, BigDecimal> incomeBySource,
                                 Map<TaskStatus, Long> taskStats,
                                 BigDecimal taskCompletionRate,
                                 Integer totalActivityMinutes,
                                 Integer totalCaloriesBurned,
                                 Map<String, Integer> activitySummary) {
    }
}
