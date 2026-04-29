package com.lifetracker.service;

import com.lifetracker.entity.Expense;
import com.lifetracker.entity.Income;
import com.lifetracker.entity.User;
import com.lifetracker.repository.ExpenseRepository;
import com.lifetracker.repository.IncomeRepository;
import com.lifetracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private Expense expense;
    private Income income;

    @BeforeEach
    void setUp() {
        user = User.builder().userId(1L).build();
        expense = Expense.builder()
                .amount(BigDecimal.valueOf(100.00))
                .category("Food")
                .expenseDate(LocalDate.now())
                .build();
        income = Income.builder()
                .amount(BigDecimal.valueOf(500.00))
                .source("Salary")
                .incomeDate(LocalDate.now())
                .build();
    }

    @Test
    void addExpense_shouldAddExpenseSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);

        Expense result = transactionService.addExpense(1L, expense);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        verify(expenseRepository).save(expense);
    }

    @Test
    void addExpense_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> transactionService.addExpense(1L, expense));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void addIncome_shouldAddIncomeSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(incomeRepository.save(any(Income.class))).thenReturn(income);

        Income result = transactionService.addIncome(1L, income);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        verify(incomeRepository).save(income);
    }

    @Test
    void getCurrentBalance_shouldReturnCorrectBalance() {
        when(incomeRepository.getTotalIncomeByUserId(1L)).thenReturn(BigDecimal.valueOf(500.00));
        when(expenseRepository.getTotalExpensesByUserId(1L)).thenReturn(BigDecimal.valueOf(100.00));

        BigDecimal balance = transactionService.getCurrentBalance(1L);

        assertEquals(BigDecimal.valueOf(400.00), balance);
    }
}