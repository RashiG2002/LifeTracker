package com.lifetracker.service;

import com.lifetracker.entity.Budget;
import com.lifetracker.entity.Expense;
import com.lifetracker.entity.Income;
import com.lifetracker.entity.User;
import com.lifetracker.repository.BudgetRepository;
import com.lifetracker.repository.ExpenseRepository;
import com.lifetracker.repository.IncomeRepository;
import com.lifetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BudgetService {
    
    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final UserRepository userRepository;
    
    public Budget createBudget(Long userId, Budget budget) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        budget.setUser(user);
        budget.setActualSpent(BigDecimal.ZERO);
        return budgetRepository.save(budget);
    }
    
    public List<Budget> getBudgetsByUserId(Long userId) {
        return budgetRepository.findByUserUserId(userId);
    }
    
    public List<Budget> getActiveBudgets(Long userId) {
        return budgetRepository.findActiveBudgetsByUserId(userId, LocalDate.now());
    }
    
    public List<Budget> getBudgetsByCategory(Long userId, String category) {
        return budgetRepository.findByUserUserIdAndCategory(userId, category);
    }
    
    public Optional<Budget> getBudgetById(Long budgetId) {
        return budgetRepository.findById(budgetId);
    }
    
    public Budget updateBudget(Budget budget) {
        return budgetRepository.save(budget);
    }
    
    public void deleteBudget(Long budgetId) {
        budgetRepository.deleteById(budgetId);
    }
    
    public Budget updateActualSpent(Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        BigDecimal spent = expenseRepository.getTotalExpensesByUserIdAndDateRange(
                budget.getUser().getUserId(),
                budget.getStartDate(),
                budget.getEndDate()
        );
        
        budget.setActualSpent(spent != null ? spent : BigDecimal.ZERO);
        return budgetRepository.save(budget);
    }
    
    public boolean isOverBudget(Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        return budget.getActualSpent().compareTo(budget.getLimitAmount()) > 0;
    }
    
    public Map<String, Object> getMonthlyFinancialSummary(Long userId, YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        BigDecimal monthlyIncome = incomeRepository.getTotalIncomeByUserIdAndDateRange(userId, startDate, endDate);
        BigDecimal monthlyExpenses = expenseRepository.getTotalExpensesByUserIdAndDateRange(userId, startDate, endDate);
        BigDecimal monthlyBalance = (monthlyIncome != null ? monthlyIncome : BigDecimal.ZERO)
                .subtract(monthlyExpenses != null ? monthlyExpenses : BigDecimal.ZERO);
        
        List<Income> monthlyIncomes = incomeRepository.findByUserUserIdAndIncomeDateBetween(userId, startDate, endDate);
        List<Expense> monthlyExpensesList = expenseRepository.findByUserUserIdAndExpenseDateBetween(userId, startDate, endDate);
        
        return Map.ofEntries(
            Map.entry("yearMonth", yearMonth),
            Map.entry("totalIncome", monthlyIncome != null ? monthlyIncome : BigDecimal.ZERO),
            Map.entry("totalExpenses", monthlyExpenses != null ? monthlyExpenses : BigDecimal.ZERO),
            Map.entry("balance", monthlyBalance),
            Map.entry("incomes", monthlyIncomes),
            Map.entry("expenses", monthlyExpensesList)
        );
    }
}
