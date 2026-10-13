package edu.sist.prs.engine.model;

import edu.sist.prs.entity.TransactionCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetTransactionSnapshot(
        Long id,
        Long projectId,
        LocalDate txnDate,
        BigDecimal amount,
        TransactionCategory category,
        Long resourceId,
        String note
) {}
