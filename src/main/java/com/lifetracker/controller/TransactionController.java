package com.lifetracker.controller;

import com.lifetracker.entity.Expense;
import com.lifetracker.entity.Income;
import com.lifetracker.entity.Budget;
import com.lifetracker.service.TransactionService;
import com.lifetracker.service.BudgetService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api")
@RequiredArgsConstructor
public class TransactionController {
    
    private final TransactionService transactionService;
    private final BudgetService budgetService;
    
    // ===== FORM SUBMISSION ENDPOINTS =====
    
    @PostMapping("/transactions/income")
    public String addIncomeForm(
            @RequestParam BigDecimal amount,
            @RequestParam String source,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String description,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = getValidSessionUserId(session);
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Session expired. Please login again.");
            return "redirect:/login";
        }
        
        Income income = new Income();
        income.setAmount(amount);
        income.setSource(source);
        income.setIncomeDate(date);
        income.setDescription(description);
        transactionService.addIncome(userId, income);
        
        return "redirect:/transactions";
    }
    
    @PostMapping("/transactions/expense")
    public String addExpenseForm(
            @RequestParam BigDecimal amount,
            @RequestParam String category,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String description,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = getValidSessionUserId(session);
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Session expired. Please login again.");
            return "redirect:/login";
        }
        
        Expense expense = new Expense();
        expense.setAmount(amount);
        expense.setCategory(category);
        expense.setExpenseDate(date);
        expense.setDescription(description);
        transactionService.addExpense(userId, expense);
        
        return "redirect:/transactions";
    }
    
    @PostMapping("/transactions/expense/{id}")
    public String deleteExpenseForm(@PathVariable Long id) {
        transactionService.deleteExpense(id);
        return "redirect:/transactions";
    }
    
    @PostMapping("/transactions/income/{id}")
    public String deleteIncomeForm(@PathVariable Long id) {
        transactionService.deleteIncome(id);
        return "redirect:/transactions";
    }
    
    // ===== REST API ENDPOINTS =====
    
    @GetMapping("/expenses")
    @ResponseBody
    public ResponseEntity<List<Expense>> getExpenses(HttpSession session) {
        Long userId = getValidSessionUserId(session);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(transactionService.getExpensesByUserId(userId));
    }
    
    @PostMapping("/expenses")
    @ResponseBody
    public ResponseEntity<Expense> addExpense(@RequestBody Expense expense, HttpSession session) {
        Long userId = getValidSessionUserId(session);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(transactionService.addExpense(userId, expense));
    }
    
    @DeleteMapping("/expenses/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        transactionService.deleteExpense(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/incomes")
    @ResponseBody
    public ResponseEntity<List<Income>> getIncomes(HttpSession session) {
        Long userId = getValidSessionUserId(session);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(transactionService.getIncomesByUserId(userId));
    }
    
    @PostMapping("/incomes")
    @ResponseBody
    public ResponseEntity<Income> addIncome(@RequestBody Income income, HttpSession session) {
        Long userId = getValidSessionUserId(session);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(transactionService.addIncome(userId, income));
    }
    
    @DeleteMapping("/incomes/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteIncome(@PathVariable Long id) {
        transactionService.deleteIncome(id);
        return ResponseEntity.ok().build();
    }
    
    // ===== BUDGET ENDPOINTS =====
    
    @PostMapping("/budgets")
    public String addBudgetForm(
            @RequestParam String category,
            @RequestParam BigDecimal monthlyLimit,
            @RequestParam String month,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = getValidSessionUserId(session);
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Session expired. Please login again.");
            return "redirect:/login";
        }
        
        LocalDate startDate = LocalDate.parse(month + "-01");
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        
        Budget budget = new Budget();
        budget.setCategory(category);
        budget.setLimitAmount(monthlyLimit);
        budget.setStartDate(startDate);
        budget.setEndDate(endDate);
        budgetService.createBudget(userId, budget);
        
        return "redirect:/budgets";
    }
    
    @PostMapping("/budgets/{id}")
    public String deleteBudgetForm(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        return "redirect:/budgets";
    }
    
    @GetMapping("/budgets")
    @ResponseBody
    public ResponseEntity<List<Budget>> getBudgets(HttpSession session) {
        Long userId = getValidSessionUserId(session);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(budgetService.getBudgetsByUserId(userId));
    }
    
    @GetMapping("/financial-summary")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFinancialSummary(HttpSession session) {
        Long userId = getValidSessionUserId(session);
        if (userId == null) return ResponseEntity.status(401).build();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalIncome", transactionService.getTotalIncome(userId));
        summary.put("totalExpenses", transactionService.getTotalExpenses(userId));
        summary.put("balance", transactionService.getCurrentBalance(userId));
        
        return ResponseEntity.ok(summary);
    }

    private Long getValidSessionUserId(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (!transactionService.isValidUser(userId)) {
            session.invalidate();
            return null;
        }
        return userId;
    }
}
