package edu.sist.prs.controller;

import edu.sist.prs.dto.request.BudgetTransactionRequest;
import edu.sist.prs.dto.response.ApiResponse;
import edu.sist.prs.engine.model.BudgetForecastResult;
import edu.sist.prs.entity.BudgetTransactionEntity;
import edu.sist.prs.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<List<BudgetTransactionEntity>>> getTransactions(@PathVariable Long projectId) {
        List<BudgetTransactionEntity> list = budgetService.getTransactions(projectId);
        return ResponseEntity.ok(ApiResponse.success(list, "Transactions retrieved"));
    }

    @PostMapping("/transactions")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<BudgetTransactionEntity>> addTransaction(
            @PathVariable Long projectId,
            @Valid @RequestBody BudgetTransactionRequest request
    ) {
        BudgetTransactionEntity entity = budgetService.addTransaction(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(entity, "Transaction recorded"));
    }

    @PostMapping("/transactions/import")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<List<BudgetTransactionEntity>>> importTransactions(
            @PathVariable Long projectId,
            @RequestBody String csvContent
    ) {
        List<BudgetTransactionEntity> imported = budgetService.importCsv(projectId, csvContent);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(imported, "Transactions imported"));
    }

    @GetMapping("/budget")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<BudgetForecastResult>> getBudgetSummary(@PathVariable Long projectId) {
        BudgetForecastResult forecast = budgetService.getProjectForecast(projectId);
        return ResponseEntity.ok(ApiResponse.success(forecast, "Budget summary retrieved"));
    }

    @GetMapping("/budget/forecast")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<ApiResponse<BudgetForecastResult>> getBudgetForecast(@PathVariable Long projectId) {
        BudgetForecastResult forecast = budgetService.getProjectForecast(projectId);
        return ResponseEntity.ok(ApiResponse.success(forecast, "Budget forecast retrieved"));
    }
}
