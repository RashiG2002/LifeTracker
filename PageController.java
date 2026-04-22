package com.lifetracker.controller;

import com.lifetracker.dto.BudgetSnapshotDTO;
import com.lifetracker.dto.GuidedActionDTO;
import com.lifetracker.dto.ReportInsightDTO;
import com.lifetracker.entity.Expense;
import com.lifetracker.entity.Income;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                    var weather = weatherService.getWeatherByLocation(task.getLocation());
                    var alert = weatherService.getWeatherAlert(weather);
                    // Store weather data in session/model context if needed
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
        // Real alert section
        List<ReportInsightDTO> realAlerts = buildRealAlerts(
            userId,
            window,
            allTime,
            totalIncome,
            totalExpenses,
            budgetSnapshots,
            overdueCount,
            highPriorityOpenCount
        );

        ReportWindow analysisWindow = allTime
            ? new ReportWindow("analysis", "Last 30 Days", LocalDate.now().minusDays(29), LocalDate.now())
            : window;
        List<Expense> analysisExpenses = transactionService.getExpensesByDateRange(userId, analysisWindow.startDate(), analysisWindow.endDate());
        List<Income> analysisIncomes = transactionService.getIncomesByDateRange(userId, analysisWindow.startDate(), analysisWindow.endDate());

        ReportInsightDTO baselineInsight = buildBaselineInsight(userId, analysisWindow, currentMetrics);
        List<ReportInsightDTO> rootCauseInsights = buildRootCauseInsights(
            trendCurrentMetrics,
            trendPreviousMetrics,
            expensesByCategory,
            overdueCount,
            highPriorityOpenCount
        );
        String dataConfidenceLabel = buildDataConfidenceLabel(analysisExpenses.size(), analysisIncomes.size(), allTasks.size());
        List<String> behaviorPatterns = buildBehaviorPatterns(analysisExpenses, allTasks);
        List<RecurringCommitmentSnapshot> recurringCommitments = buildRecurringCommitments(userId);
        PeriodReviewSummary periodReviewSummary = buildPeriodReviewSummary(
            baselineInsight,
            rootCauseInsights,
            realAlerts,
            guidedActions
        );

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
        model.addAttribute("realAlerts", realAlerts);
        model.addAttribute("baselineInsight", baselineInsight);
        model.addAttribute("rootCauseInsights", rootCauseInsights);
        model.addAttribute("dataConfidenceLabel", dataConfidenceLabel);
        model.addAttribute("behaviorPatterns", behaviorPatterns);
        model.addAttribute("recurringCommitments", recurringCommitments);
        model.addAttribute("periodReviewSummary", periodReviewSummary);
        model.addAttribute("expenseSimulatorBase", totalExpenses);
        model.addAttribute("taskSimulatorOverdueBase", overdueCount);
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

    private List<ReportInsightDTO> buildRealAlerts(Long userId,
                                                   ReportWindow window,
                                                   boolean allTime,
                                                   BigDecimal totalIncome,
                                                   BigDecimal totalExpenses,
                                                   List<BudgetSnapshotDTO> budgetSnapshots,
                                                   long overdueCount,
                                                   long highPriorityOpenCount) {
        // Real alert rules
        List<ReportInsightDTO> alerts = new ArrayList<>();

        long breach100 = budgetSnapshots.stream()
            .filter(budget -> budget.getUtilizationPercent() != null && budget.getUtilizationPercent().compareTo(BigDecimal.valueOf(100)) >= 0)
            .count();
        long breach90 = budgetSnapshots.stream()
            .filter(budget -> budget.getUtilizationPercent() != null
                && budget.getUtilizationPercent().compareTo(BigDecimal.valueOf(90)) >= 0
                && budget.getUtilizationPercent().compareTo(BigDecimal.valueOf(100)) < 0)
            .count();
        long breach70 = budgetSnapshots.stream()
            .filter(budget -> budget.getUtilizationPercent() != null
                && budget.getUtilizationPercent().compareTo(BigDecimal.valueOf(70)) >= 0
                && budget.getUtilizationPercent().compareTo(BigDecimal.valueOf(90)) < 0)
            .count();

        if (breach100 > 0) {
            alerts.add(ReportInsightDTO.builder()
                .title("Budget breach alert")
                .message(breach100 + " budget" + (breach100 == 1 ? " has" : "s have") + " crossed 100% utilization. Freeze non-essential spending in those categories today.")
                .level("danger")
                .icon("fa-triangle-exclamation")
                .build());
        }
        if (breach90 > 0) {
            alerts.add(ReportInsightDTO.builder()
                .title("Budget critical warning")
                .message(breach90 + " budget" + (breach90 == 1 ? " is" : "s are") + " above 90% utilization. Review upcoming purchases before the cycle closes.")
                .level("warning")
                .icon("fa-gauge-high")
                .build());
        }
        if (breach70 > 0) {
            alerts.add(ReportInsightDTO.builder()
                .title("Budget early warning")
                .message(breach70 + " budget" + (breach70 == 1 ? " is" : "s are") + " above 70% utilization. Slow discretionary spending to avoid end-of-period breach.")
                .level("info")
                .icon("fa-bell")
                .build());
        }

        BigDecimal currentBalance = transactionService.getCurrentBalance(userId);
        if (currentBalance != null) {
            if (currentBalance.compareTo(BigDecimal.ZERO) <= 0) {
                alerts.add(ReportInsightDTO.builder()
                    .title("Cash runway warning")
                    .message("Your current balance is at or below zero. Prioritize essential expenses and add income coverage immediately.")
                    .level("danger")
                    .icon("fa-wallet")
                    .build());
            } else {
                LocalDate now = LocalDate.now();
                LocalDate spanStart = allTime ? now.minusDays(29) : window.startDate();
                LocalDate spanEnd = allTime ? now : window.endDate();
                long spanDays = Math.max(1, ChronoUnit.DAYS.between(spanStart, spanEnd) + 1);

                BigDecimal incomeForRunway = allTime
                    ? transactionService.getTotalIncomeByDateRange(userId, spanStart, spanEnd)
                    : totalIncome;
                BigDecimal expensesForRunway = allTime
                    ? transactionService.getTotalExpensesByDateRange(userId, spanStart, spanEnd)
                    : totalExpenses;
                BigDecimal dailyNetBurn = expensesForRunway.subtract(incomeForRunway)
                    .divide(BigDecimal.valueOf(spanDays), 2, RoundingMode.HALF_UP);

                if (dailyNetBurn.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal runwayDays = currentBalance.divide(dailyNetBurn, 0, RoundingMode.DOWN);
                    if (runwayDays.compareTo(BigDecimal.valueOf(7)) <= 0) {
                        alerts.add(ReportInsightDTO.builder()
                            .title("Cash runway critical")
                            .message("At the current burn rate, your balance covers about " + runwayDays + " days. Reduce variable expenses now.")
                            .level("danger")
                            .icon("fa-hourglass-end")
                            .build());
                    } else if (runwayDays.compareTo(BigDecimal.valueOf(30)) <= 0) {
                        alerts.add(ReportInsightDTO.builder()
                            .title("Cash runway warning")
                            .message("At the current burn rate, your balance covers about " + runwayDays + " days. Tighten spending this month.")
                            .level("warning")
                            .icon("fa-hourglass-half")
                            .build());
                    }
                }
            }
        }

        if (overdueCount > 0 || highPriorityOpenCount > 0) {
            String taskMessage = "You have " + overdueCount + " overdue task" + (overdueCount == 1 ? "" : "s")
                + " and " + highPriorityOpenCount + " high-priority open task" + (highPriorityOpenCount == 1 ? "" : "s")
                + ". Clear these before taking new work.";
            alerts.add(ReportInsightDTO.builder()
                .title("Task urgency alert")
                .message(taskMessage)
                .level(overdueCount > 0 && highPriorityOpenCount > 0 ? "danger" : "warning")
                .icon("fa-list-check")
                .build());
        }

        List<LocalDate> healthDates = healthService.getActivitiesByUserId(userId).stream()
            .map(activity -> activity.getRecordDate())
            .filter(date -> date != null)
            .toList();
        if (healthDates.isEmpty()) {
            alerts.add(ReportInsightDTO.builder()
                .title("Health inactivity alert")
                .message("No health activity has been logged yet. Add one short session today to start your streak.")
                .level("warning")
                .icon("fa-heart-pulse")
                .build());
        } else {
            LocalDate lastHealthDate = healthDates.stream().max(LocalDate::compareTo).orElse(LocalDate.now());
            long daysSinceHealth = ChronoUnit.DAYS.between(lastHealthDate, LocalDate.now());
            if (daysSinceHealth > 14) {
                alerts.add(ReportInsightDTO.builder()
                    .title("Health streak broken")
                    .message("No activity logged for " + daysSinceHealth + " days. Schedule a 20-minute session today.")
                    .level("danger")
                    .icon("fa-heart-crack")
                    .build());
            } else if (daysSinceHealth > 7) {
                alerts.add(ReportInsightDTO.builder()
                    .title("Health inactivity warning")
                    .message("No activity logged for " + daysSinceHealth + " days. Add one workout to protect momentum.")
                    .level("warning")
                    .icon("fa-heart-pulse")
                    .build());
            }
        }

        List<LocalDate> skillProgressDates = skillService.getSkillsByUserId(userId).stream()
            .flatMap(skill -> skillService.getProgressBySkillId(skill.getSkillId()).stream())
            .map(progress -> progress.getProgressDate())
            .filter(date -> date != null)
            .toList();
        if (!skillService.getSkillsByUserId(userId).isEmpty()) {
            if (skillProgressDates.isEmpty()) {
                alerts.add(ReportInsightDTO.builder()
                    .title("Skills inactivity alert")
                    .message("Skills exist but no progress logs were found. Add one focused practice entry today.")
                    .level("warning")
                    .icon("fa-graduation-cap")
                    .build());
            } else {
                LocalDate lastSkillDate = skillProgressDates.stream().max(LocalDate::compareTo).orElse(LocalDate.now());
                long daysSinceSkill = ChronoUnit.DAYS.between(lastSkillDate, LocalDate.now());
                if (daysSinceSkill > 30) {
                    alerts.add(ReportInsightDTO.builder()
                        .title("Skills streak broken")
                        .message("No skill practice logged for " + daysSinceSkill + " days. Restart with a 25-minute learning sprint.")
                        .level("danger")
                        .icon("fa-user-graduate")
                        .build());
                } else if (daysSinceSkill > 14) {
                    alerts.add(ReportInsightDTO.builder()
                        .title("Skills inactivity warning")
                        .message("No skill practice logged for " + daysSinceSkill + " days. Add a small practice block this week.")
                        .level("warning")
                        .icon("fa-graduation-cap")
                        .build());
                }
            }
        }

        return alerts;
    }

    private ReportInsightDTO buildBaselineInsight(Long userId,
                                               ReportWindow analysisWindow,
                                               ReportMetrics currentMetrics) {
        long spanDays = Math.max(1, ChronoUnit.DAYS.between(analysisWindow.startDate(), analysisWindow.endDate()) + 1);

        BigDecimal incomeBaseline = BigDecimal.ZERO;
        BigDecimal expenseBaseline = BigDecimal.ZERO;
        int samples = 0;

        for (int i = 1; i <= 3; i++) {
            LocalDate end = analysisWindow.startDate().minusDays(((long) i - 1) * spanDays + 1);
            LocalDate start = end.minusDays(spanDays - 1);
            incomeBaseline = incomeBaseline.add(transactionService.getTotalIncomeByDateRange(userId, start, end));
            expenseBaseline = expenseBaseline.add(transactionService.getTotalExpensesByDateRange(userId, start, end));
            samples++;
        }

        BigDecimal baselineSavings = samples > 0
            ? incomeBaseline.subtract(expenseBaseline).divide(BigDecimal.valueOf(samples), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        BigDecimal savingsDeltaVsBaseline = currentMetrics.totalSavings().subtract(baselineSavings);

        String level = savingsDeltaVsBaseline.compareTo(BigDecimal.ZERO) >= 0 ? "success" : "warning";
        String message = "Savings are "
            + (savingsDeltaVsBaseline.compareTo(BigDecimal.ZERO) >= 0 ? "above" : "below")
            + " the 3-period baseline by Rs. " + savingsDeltaVsBaseline.abs().setScale(2, RoundingMode.HALF_UP)
            + ". Baseline average savings: Rs. " + baselineSavings.setScale(2, RoundingMode.HALF_UP) + ".";

        return ReportInsightDTO.builder()
            .title("Trend vs Baseline")
            .message(message)
            .level(level)
            .icon("fa-wave-square")
            .build();
    }

    private List<ReportInsightDTO> buildRootCauseInsights(ReportMetrics trendCurrentMetrics,
                                                           ReportMetrics trendPreviousMetrics,
                                                           Map<String, BigDecimal> expensesByCategory,
                                                           long overdueCount,
                                                           long highPriorityOpenCount) {
        List<ReportInsightDTO> causes = new ArrayList<>();

        BigDecimal expenseDelta = trendCurrentMetrics.totalExpenses().subtract(trendPreviousMetrics.totalExpenses());
        if (expenseDelta.compareTo(BigDecimal.ZERO) > 0) {
            String topCategory = expensesByCategory.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("uncategorized spending");
            causes.add(ReportInsightDTO.builder()
                .title("Expense rise root cause")
                .message("Expenses increased by Rs. " + expenseDelta.setScale(2, RoundingMode.HALF_UP)
                    + ", with the strongest pressure coming from " + topCategory + ".")
                .level("warning")
                .icon("fa-chart-line")
                .build());
        }

        BigDecimal completionDelta = trendCurrentMetrics.taskCompletionRate().subtract(trendPreviousMetrics.taskCompletionRate());
        if (completionDelta.compareTo(BigDecimal.ZERO) < 0) {
            causes.add(ReportInsightDTO.builder()
                .title("Task throughput root cause")
                .message("Task completion dropped by " + completionDelta.abs().setScale(1, RoundingMode.HALF_UP)
                    + " points, while backlog pressure is " + overdueCount + " overdue and "
                    + highPriorityOpenCount + " high-priority open tasks.")
                .level("warning")
                .icon("fa-list-check")
                .build());
        }

        if (causes.isEmpty()) {
            causes.add(ReportInsightDTO.builder()
                .title("No critical root-cause drift")
                .message("Current period is stable against comparison baseline. Continue monitoring category concentration and overdue backlog.")
                .level("info")
                .icon("fa-circle-check")
                .build());
        }

        return causes;
    }

    private String buildDataConfidenceLabel(int expenseCount, int incomeCount, int taskCount) {
        int totalSignals = expenseCount + incomeCount + taskCount;
        if (totalSignals >= 40) {
            return "High confidence";
        }
        if (totalSignals >= 15) {
            return "Medium confidence";
        }
        return "Low confidence";
    }

    private List<String> buildBehaviorPatterns(List<Expense> expenses, List<Task> tasks) {
        List<String> patterns = new ArrayList<>();
        if (!expenses.isEmpty()) {
            Map<DayOfWeek, BigDecimal> spendByDay = expenses.stream()
                .filter(expense -> expense.getExpenseDate() != null && expense.getAmount() != null)
                .collect(Collectors.groupingBy(
                    expense -> expense.getExpenseDate().getDayOfWeek(),
                    Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

            spendByDay.entrySet().stream().max(Map.Entry.comparingByValue()).ifPresent(entry ->
                patterns.add("Highest spending day is " + entry.getKey() + " (Rs. " + entry.getValue().setScale(2, RoundingMode.HALF_UP) + ")."));

            BigDecimal weekendSpend = expenses.stream()
                .filter(expense -> expense.getExpenseDate() != null)
                .filter(expense -> expense.getExpenseDate().getDayOfWeek() == DayOfWeek.SATURDAY
                    || expense.getExpenseDate().getDayOfWeek() == DayOfWeek.SUNDAY)
                .map(Expense::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalSpend = expenses.stream()
                .map(Expense::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalSpend.signum() > 0) {
                BigDecimal weekendShare = weekendSpend.multiply(BigDecimal.valueOf(100)).divide(totalSpend, 1, RoundingMode.HALF_UP);
                patterns.add("Weekend spend share is " + weekendShare + "% of the selected period.");
            }
        }

        long dueSoon = tasks.stream()
            .filter(task -> task.getDueDate() != null)
            .filter(task -> !task.getDueDate().isBefore(LocalDate.now()) && !task.getDueDate().isAfter(LocalDate.now().plusDays(3)))
            .count();
        patterns.add("Tasks due in next 3 days: " + dueSoon + ".");

        return patterns;
    }

    private List<RecurringCommitmentSnapshot> buildRecurringCommitments(Long userId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(89);
        List<Expense> recentExpenses = transactionService.getExpensesByDateRange(userId, start, end);

        Map<String, List<Expense>> grouped = recentExpenses.stream()
            .filter(expense -> expense.getDescription() != null && !expense.getDescription().isBlank())
            .collect(Collectors.groupingBy(expense -> (expense.getCategory() + " - " + expense.getDescription()).trim()));

        List<RecurringCommitmentSnapshot> recurring = new ArrayList<>();
        for (Map.Entry<String, List<Expense>> entry : grouped.entrySet()) {
            List<Expense> items = entry.getValue();
            if (items.size() < 2) {
                continue;
            }
            BigDecimal total = items.stream()
                .map(Expense::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avgMonthly = total.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            LocalDate lastDate = items.stream()
                .map(Expense::getExpenseDate)
                .filter(date -> date != null)
                .max(LocalDate::compareTo)
                .orElse(end);
            recurring.add(new RecurringCommitmentSnapshot(entry.getKey(), avgMonthly, items.size(), lastDate));
        }

        return recurring.stream()
            .sorted(Comparator.comparing(RecurringCommitmentSnapshot::avgMonthlyAmount).reversed())
            .limit(6)
            .toList();
    }

    private PeriodReviewSummary buildPeriodReviewSummary(ReportInsightDTO baselineInsight,
                                                         List<ReportInsightDTO> rootCauseInsights,
                                                         List<ReportInsightDTO> realAlerts,
                                                         List<GuidedActionDTO> guidedActions) {
        String headline = realAlerts.stream().anyMatch(alert -> "danger".equalsIgnoreCase(alert.getLevel()))
            ? "Critical signals detected"
            : "Period performance is mostly stable";

        String win = baselineInsight != null ? baselineInsight.getMessage() : "Baseline not available.";
        String risk = rootCauseInsights.isEmpty() ? "No major root-cause drift." : rootCauseInsights.get(0).getMessage();
        String next = guidedActions.isEmpty() ? "Keep logging data and review next period." : guidedActions.get(0).getTitle() + ": " + guidedActions.get(0).getSummary();

        return new PeriodReviewSummary(headline, win, risk, next);
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

    private record RecurringCommitmentSnapshot(String name,
                                               BigDecimal avgMonthlyAmount,
                                               long occurrences,
                                               LocalDate lastDate) {
    }

    private record PeriodReviewSummary(String headline,
                                       String wins,
                                       String risks,
                                       String nextStep) {
    }
}
