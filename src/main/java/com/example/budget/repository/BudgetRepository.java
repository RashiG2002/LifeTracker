package com.lifetracker.repository;

import com.lifetracker.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    
    List<Budget> findByUserUserId(Long userId);
    
    List<Budget> findByUserUserIdAndCategory(Long userId, String category);
    
    List<Budget> findByUserUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long userId, LocalDate date1, LocalDate date2);
    
    default List<Budget> findActiveBudgetsByUserId(Long userId, LocalDate currentDate) {
        return findByUserUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                userId, currentDate, currentDate);
    }
}
