package com.lifetracker.service;

import com.lifetracker.entity.Expense;
import com.lifetracker.entity.Income;
import com.lifetracker.entity.User;
import com.lifetracker.repository.ExpenseRepository;
import com.lifetracker.repository.IncomeRepository;
import com.lifetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {
    
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final UserRepository userRepository;
    
    // ===== EXPENSE METHODS =====
    
    public Expense addExpense(Long userId, Expense expense) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        expense.setUser(user);
        return expenseRepository.save(expense);
    }
    
    public List<Expense> getExpensesByUserId(Long userId) {
        return expenseRepository.findByUserUserIdOrderByExpenseDateDesc(userId);
    }
    
    public List<Expense> getExpensesByCategory(Long userId, String category) {
        return expenseRepository.findByUserUserIdAndCategory(userId, category);
    }
    
    public List<Expense> getExpensesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findByUserUserIdAndExpenseDateBetween(userId, startDate, endDate);
    }
    
    public BigDecimal getTotalExpenses(Long userId) {
        BigDecimal total = expenseRepository.getTotalExpensesByUserId(userId);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    public BigDecimal getTotalExpensesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        BigDecimal total = expenseRepository.getTotalExpensesByUserIdAndDateRange(userId, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    public Map<String, BigDecimal> getExpensesByCategory(Long userId, boolean dummy) {
        List<Object[]> results = expenseRepository.getExpensesByCategory(userId);
        return results.stream()
                .collect(Collectors.toMap(
                        r -> (String) r[0],
                        r -> (BigDecimal) r[1]
                ));
    }
    
    public Optional<Expense> getExpenseById(Long expenseId) {
        return expenseRepository.findById(expenseId);
    }
    
    public Expense updateExpense(Expense expense) {
        return expenseRepository.save(expense);
    }
    
    public void deleteExpense(Long expenseId) {
        expenseRepository.deleteById(expenseId);
    }
    
    // ===== INCOME METHODS =====
    
    public Income addIncome(Long userId, Income income) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        income.setUser(user);
        return incomeRepository.save(income);
    }
    
    public List<Income> getIncomesByUserId(Long userId) {
        return incomeRepository.findByUserUserIdOrderByIncomeDateDesc(userId);
    }
    
    public List<Income> getIncomesBySource(Long userId, String source) {
        return incomeRepository.findByUserUserIdAndSource(userId, source);
    }
    
    public List<Income> getIncomesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return incomeRepository.findByUserUserIdAndIncomeDateBetween(userId, startDate, endDate);
    }
    
    public BigDecimal getTotalIncome(Long userId) {
        BigDecimal total = incomeRepository.getTotalIncomeByUserId(userId);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    public BigDecimal getTotalIncomeByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        BigDecimal total = incomeRepository.getTotalIncomeByUserIdAndDateRange(userId, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    public Map<String, BigDecimal> getIncomeBySource(Long userId) {
        List<Object[]> results = incomeRepository.getIncomeBySource(userId);
        return results.stream()
                .collect(Collectors.toMap(
                        r -> (String) r[0],
                        r -> (BigDecimal) r[1]
                ));
    }
    
    public Optional<Income> getIncomeById(Long incomeId) {
        return incomeRepository.findById(incomeId);
    }
    
    public Income updateIncome(Income income) {
        return incomeRepository.save(income);
    }
    
    public void deleteIncome(Long incomeId) {
        incomeRepository.deleteById(incomeId);
    }
    
    // ===== BALANCE METHODS =====
    
    public BigDecimal getCurrentBalance(Long userId) {
        BigDecimal totalIncome = getTotalIncome(userId);
        BigDecimal totalExpenses = getTotalExpenses(userId);
        return totalIncome.subtract(totalExpenses);
    }
}
