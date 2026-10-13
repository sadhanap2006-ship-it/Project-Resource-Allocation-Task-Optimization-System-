package edu.sist.prs.dto.request;

import edu.sist.prs.entity.TransactionCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetTransactionRequest(
        @NotNull(message = "Transaction date is required")
        LocalDate txnDate,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        @NotNull(message = "Category is required")
        TransactionCategory category,

        Long resourceId,

        @Size(max = 255, message = "Note cannot exceed 255 characters")
        String note
) {}
