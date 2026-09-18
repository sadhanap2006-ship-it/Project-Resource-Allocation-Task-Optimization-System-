package com.sist.rms.repository;

import com.sist.rms.entity.BudgetTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BudgetTransactionRepository extends JpaRepository<BudgetTransaction, Long> {

    List<BudgetTransaction> findByProjectBudgetIdOrderByTransactionDateAsc(Long projectBudgetId);
}
