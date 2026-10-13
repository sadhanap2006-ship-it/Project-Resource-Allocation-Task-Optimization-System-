package edu.sist.prs.service;

import edu.sist.prs.dto.request.BudgetTransactionRequest;
import edu.sist.prs.engine.BudgetCalculator;
import edu.sist.prs.engine.model.AllocationSnapshot;
import edu.sist.prs.engine.model.BudgetForecastResult;
import edu.sist.prs.engine.model.BudgetTransactionSnapshot;
import edu.sist.prs.engine.model.MilestoneSnapshot;
import edu.sist.prs.engine.model.ResourceSnapshot;
import edu.sist.prs.engine.model.UnavailabilitySnapshot;
import edu.sist.prs.entity.BudgetTransactionEntity;
import edu.sist.prs.entity.ProjectEntity;
import edu.sist.prs.entity.TransactionCategory;
import edu.sist.prs.exception.ApiException;
import edu.sist.prs.repository.BudgetTransactionRepository;
import edu.sist.prs.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class BudgetService {

    private final BudgetTransactionRepository budgetTransactionRepository;
    private final ProjectRepository projectRepository;
    private final ConflictDetectionService conflictDetectionService;
    private final SettingsService settingsService;

    public BudgetService(
            BudgetTransactionRepository budgetTransactionRepository,
            ProjectRepository projectRepository,
            ConflictDetectionService conflictDetectionService,
            SettingsService settingsService
    ) {
        this.budgetTransactionRepository = budgetTransactionRepository;
        this.projectRepository = projectRepository;
        this.conflictDetectionService = conflictDetectionService;
        this.settingsService = settingsService;
    }

    @Transactional(readOnly = true)
    public List<BudgetTransactionEntity> getTransactions(Long projectId) {
        return budgetTransactionRepository.findByProjectIdOrderByTxnDateDesc(projectId);
    }

    @Transactional
    public BudgetTransactionEntity addTransaction(Long projectId, BudgetTransactionRequest req) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Project not found"));

        BudgetTransactionEntity entity = new BudgetTransactionEntity(
                project.getId(),
                req.txnDate(),
                req.amount(),
                req.category(),
                req.resourceId(),
                req.note()
        );

        BudgetTransactionEntity saved = budgetTransactionRepository.save(entity);
        conflictDetectionService.reevaluateScope(null, Set.of(projectId));
        return saved;
    }

    @Transactional
    public List<BudgetTransactionEntity> importCsv(Long projectId, String csvContent) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Project not found"));

        List<BudgetTransactionEntity> created = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (firstLine && (line.toLowerCase().contains("date") || line.toLowerCase().contains("amount"))) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                String[] parts = line.split(",", -1);
                if (parts.length >= 3) {
                    LocalDate date = LocalDate.parse(parts[0].trim());
                    BigDecimal amount = new BigDecimal(parts[1].trim());
                    TransactionCategory cat = TransactionCategory.valueOf(parts[2].trim().toUpperCase());
                    String note = parts.length > 3 ? parts[3].trim() : null;

                    created.add(new BudgetTransactionEntity(project.getId(), date, amount, cat, null, note));
                }
            }
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Failed to parse CSV: " + e.getMessage());
        }

        List<BudgetTransactionEntity> saved = budgetTransactionRepository.saveAll(created);
        conflictDetectionService.reevaluateScope(null, Set.of(projectId));
        return saved;
    }

    @Transactional(readOnly = true)
    public BudgetForecastResult getProjectForecast(Long projectId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Project not found"));

        List<MilestoneSnapshot> milestones = conflictDetectionService.loadMilestoneSnapshots();
        List<AllocationSnapshot> allocations = conflictDetectionService.loadAllocationSnapshots();
        List<ResourceSnapshot> resources = conflictDetectionService.loadResourceSnapshots();
        List<UnavailabilitySnapshot> unavailabilities = conflictDetectionService.loadUnavailabilitySnapshots();

        List<BudgetTransactionEntity> txns = budgetTransactionRepository.findByProjectIdOrderByTxnDateDesc(projectId);
        List<BudgetTransactionSnapshot> txnSnapshots = txns.stream()
                .map(t -> new BudgetTransactionSnapshot(t.getId(), t.getProjectId(), t.getTxnDate(), t.getAmount(), t.getCategory(), t.getResourceId(), t.getNote()))
                .toList();

        int burnWindow = settingsService.getInt("burn.window.days", 14);
        int hoursPerDay = settingsService.getInt("hours.per.day", 8);

        return BudgetCalculator.calculateProjectForecast(
                project.getId(),
                project.getName(),
                project.getTotalBudget(),
                project.getStartDate(),
                project.getEndDate(),
                milestones,
                allocations,
                resources,
                unavailabilities,
                txnSnapshots,
                LocalDate.now(),
                burnWindow,
                hoursPerDay
        );
    }
}
