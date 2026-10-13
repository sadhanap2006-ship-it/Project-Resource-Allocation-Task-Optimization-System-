package edu.sist.prs.repository;

import edu.sist.prs.entity.BudgetTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface BudgetTransactionRepository extends JpaRepository<BudgetTransactionEntity, Long> {
    List<BudgetTransactionEntity> findByProjectIdOrderByTxnDateAsc(Long projectId);
    List<BudgetTransactionEntity> findByProjectIdOrderByTxnDateDesc(Long projectId);
    List<BudgetTransactionEntity> findByProjectIdAndTxnDateLessThanEqualOrderByTxnDateAsc(Long projectId, LocalDate date);
}
