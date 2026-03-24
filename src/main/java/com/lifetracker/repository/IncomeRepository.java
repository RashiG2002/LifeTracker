package com.lifetracker.repository;

import com.lifetracker.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Long> {
    
    List<Income> findByUserUserIdOrderByIncomeDateDesc(Long userId);
    
    List<Income> findByUserUserIdAndSource(Long userId, String source);
    
    List<Income> findByUserUserIdAndIncomeDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.user.userId = :userId")
    BigDecimal getTotalIncomeByUserId(@Param("userId") Long userId);
    
    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.user.userId = :userId AND i.incomeDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalIncomeByUserIdAndDateRange(@Param("userId") Long userId, 
                                                   @Param("startDate") LocalDate startDate, 
                                                   @Param("endDate") LocalDate endDate);
    
    @Query("SELECT i.source, SUM(i.amount) FROM Income i WHERE i.user.userId = :userId GROUP BY i.source")
    List<Object[]> getIncomeBySource(@Param("userId") Long userId);
}
