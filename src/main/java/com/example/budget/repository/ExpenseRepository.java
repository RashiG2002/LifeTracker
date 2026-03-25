package com.lifetracker.repository;

import com.lifetracker.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    
    List<Expense> findByUserUserIdOrderByExpenseDateDesc(Long userId);
    
    List<Expense> findByUserUserIdAndCategory(Long userId, String category);
    
    List<Expense> findByUserUserIdAndExpenseDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.userId = :userId")
    BigDecimal getTotalExpensesByUserId(@Param("userId") Long userId);
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.userId = :userId AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalExpensesByUserIdAndDateRange(@Param("userId") Long userId, 
                                                     @Param("startDate") LocalDate startDate, 
                                                     @Param("endDate") LocalDate endDate);
    
    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.user.userId = :userId GROUP BY e.category")
    List<Object[]> getExpensesByCategory(@Param("userId") Long userId);
}
