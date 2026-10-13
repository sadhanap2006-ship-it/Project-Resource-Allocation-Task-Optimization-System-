package edu.sist.prs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import edu.sist.prs.engine.model.SimulationResult;
import edu.sist.prs.entity.ProjectEntity;
import edu.sist.prs.entity.SimulationScenarioEntity;
import edu.sist.prs.entity.UserEntity;
import edu.sist.prs.exception.ApiException;
import edu.sist.prs.repository.ProjectRepository;
import edu.sist.prs.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ReportService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
    private static final Font FONT_H2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(30, 58, 138));
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
    private static final Font FONT_REGULAR = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
    private static final Font FONT_GREEN = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(22, 101, 52));
    private static final Font FONT_RED = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(153, 27, 27));

    public ReportService(ProjectRepository projectRepository, UserRepository userRepository, ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public byte[] generateScenarioPdf(SimulationScenarioEntity scenario) {
        ProjectEntity project = projectRepository.findById(scenario.getProjectId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Project not found"));
        UserEntity author = userRepository.findById(scenario.getCreatedBy())
                .orElse(null);

        SimulationResult result;
        try {
            result = objectMapper.readValue(scenario.getResultJson(), SimulationResult.class);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Failed to deserialize simulation result: " + e.getMessage());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Header / Title
            Paragraph title = new Paragraph("Project Resource Allocation Report", FONT_TITLE);
            title.setSpacingAfter(6);
            document.add(title);

            Paragraph meta = new Paragraph();
            meta.add(new Chunk("Project: ", FONT_BOLD));
            meta.add(new Chunk(project.getName() + " (" + project.getCode() + ")\n", FONT_REGULAR));
            meta.add(new Chunk("Scenario: ", FONT_BOLD));
            meta.add(new Chunk(scenario.getName() + "\n", FONT_REGULAR));
            meta.add(new Chunk("Generated: ", FONT_BOLD));
            meta.add(new Chunk(scenario.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) + "\n", FONT_REGULAR));
            meta.add(new Chunk("Author: ", FONT_BOLD));
            meta.add(new Chunk((author != null ? author.getName() + " (" + author.getEmail() + ")" : "System") + "\n", FONT_REGULAR));
            meta.setSpacingAfter(14);
            document.add(meta);

            // 2. Allocation Comparison
            addSectionHeading(document, "2. Allocation Comparison");
            Paragraph allocDesc = new Paragraph("Active allocations before and after scenario simulation:", FONT_REGULAR);
            allocDesc.setSpacingAfter(6);
            document.add(allocDesc);

            PdfPTable allocTable = new PdfPTable(4);
            allocTable.setWidthPercentage(100);
            allocTable.setWidths(new float[]{30, 30, 20, 20});
            addCell(allocTable, "Milestone", FONT_BOLD, true);
            addCell(allocTable, "Resource", FONT_BOLD, true);
            addCell(allocTable, "Baseline %", FONT_BOLD, true);
            addCell(allocTable, "Simulated %", FONT_BOLD, true);

            if (result.allocationComparisons() != null) {
                for (Map<String, Object> a : result.allocationComparisons()) {
                    addCell(allocTable, String.valueOf(a.getOrDefault("milestoneName", "-")), FONT_REGULAR, false);
                    addCell(allocTable, String.valueOf(a.getOrDefault("resourceName", "-")), FONT_REGULAR, false);
                    addCell(allocTable, a.get("baselinePct") != null ? a.get("baselinePct") + "%" : "0%", FONT_REGULAR, false);
                    addCell(allocTable, a.get("simulatedPct") != null ? a.get("simulatedPct") + "%" : "0%", FONT_REGULAR, false);
                }
            }
            allocTable.setSpacingAfter(12);
            document.add(allocTable);

            // 3. Resource Peak Loads
            addSectionHeading(document, "3. Resource Peak Loads");
            if (result.resourcePeakLoads() != null && !result.resourcePeakLoads().isEmpty()) {
                PdfPTable peakTable = new PdfPTable(3);
                peakTable.setWidthPercentage(100);
                peakTable.setWidths(new float[]{40, 30, 30});
                addCell(peakTable, "Resource ID", FONT_BOLD, true);
                addCell(peakTable, "Baseline Peak %", FONT_BOLD, true);
                addCell(peakTable, "Simulated Peak %", FONT_BOLD, true);

                for (Map.Entry<Long, Map<String, Integer>> entry : result.resourcePeakLoads().entrySet()) {
                    addCell(peakTable, "Resource #" + entry.getKey(), FONT_REGULAR, false);
                    int base = entry.getValue().getOrDefault("baseline", 0);
                    int sim = entry.getValue().getOrDefault("simulated", 0);
                    addCell(peakTable, base + "%", FONT_REGULAR, false);
                    addCell(peakTable, sim + "%", sim <= 100 ? FONT_GREEN : FONT_RED, false);
                }
                peakTable.setSpacingAfter(12);
                document.add(peakTable);
            }

            // 4. Cost and Delivery Date Comparison
            addSectionHeading(document, "4. Cost and Delivery Date Comparison");
            PdfPTable kpiTable = new PdfPTable(4);
            kpiTable.setWidthPercentage(100);
            kpiTable.setWidths(new float[]{25, 25, 25, 25});
            addCell(kpiTable, "Metric", FONT_BOLD, true);
            addCell(kpiTable, "Current Baseline", FONT_BOLD, true);
            addCell(kpiTable, "Simulated", FONT_BOLD, true);
            addCell(kpiTable, "Delta / Impact", FONT_BOLD, true);

            String baselineCostStr = formatInr(result.baselinePlanTotal());
            String simCostStr = formatInr(result.simulatedPlanTotal());
            BigDecimal costDelta = result.simulatedPlanTotal().subtract(result.baselinePlanTotal());
            String costDeltaStr = (costDelta.compareTo(BigDecimal.ZERO) >= 0 ? "+ " : "- ") + formatInr(costDelta.abs());

            addCell(kpiTable, "Planned Cost", FONT_REGULAR, false);
            addCell(kpiTable, baselineCostStr, FONT_REGULAR, false);
            addCell(kpiTable, simCostStr, FONT_REGULAR, false);
            addCell(kpiTable, costDeltaStr, costDelta.compareTo(BigDecimal.ZERO) <= 0 ? FONT_GREEN : FONT_RED, false);

            String baseDeliv = result.baselineForecastDelivery() != null ? result.baselineForecastDelivery().toString() : "On Schedule";
            String simDeliv = result.simulatedForecastDelivery() != null ? result.simulatedForecastDelivery().toString() : "On Schedule";
            long daysDelta = 0;
            if (result.baselineForecastDelivery() != null && result.simulatedForecastDelivery() != null) {
                daysDelta = ChronoUnit.DAYS.between(result.baselineForecastDelivery(), result.simulatedForecastDelivery());
            }

            addCell(kpiTable, "Forecast Delivery", FONT_REGULAR, false);
            addCell(kpiTable, baseDeliv, FONT_REGULAR, false);
            addCell(kpiTable, simDeliv, FONT_REGULAR, false);
            addCell(kpiTable, daysDelta <= 0 ? daysDelta + " days (Faster)" : "+" + daysDelta + " days (Delay)",
                    daysDelta <= 0 ? FONT_GREEN : FONT_RED, false);

            kpiTable.setSpacingAfter(12);
            document.add(kpiTable);

            // 5. Conflicts Removed and Added
            addSectionHeading(document, "5. Conflict Analysis");
            Paragraph conflictP = new Paragraph();
            conflictP.add(new Chunk("Conflicts Removed: ", FONT_BOLD));
            conflictP.add(new Chunk((result.conflictsRemoved() != null ? result.conflictsRemoved().size() : 0) + "\n", FONT_GREEN));
            if (result.conflictsRemoved() != null) {
                for (var c : result.conflictsRemoved()) {
                    conflictP.add(new Chunk("  ✓ [" + c.severity() + "] " + c.description() + "\n", FONT_SMALL));
                }
            }
            conflictP.add(new Chunk("Conflicts Added: ", FONT_BOLD));
            conflictP.add(new Chunk((result.conflictsAdded() != null ? result.conflictsAdded().size() : 0) + "\n", FONT_RED));
            if (result.conflictsAdded() != null) {
                for (var c : result.conflictsAdded()) {
                    conflictP.add(new Chunk("  ✗ [" + c.severity() + "] " + c.description() + "\n", FONT_SMALL));
                }
            }
            conflictP.setSpacingAfter(12);
            document.add(conflictP);

            // 6. Budget Impact
            addSectionHeading(document, "6. Budget Impact & Run-Out Forecast");
            Paragraph budgetP = new Paragraph();
            budgetP.add(new Chunk("Total Budget: ", FONT_BOLD));
            budgetP.add(new Chunk(formatInr(project.getTotalBudget()) + "\n", FONT_REGULAR));
            budgetP.add(new Chunk("Current Run-Out Date: ", FONT_BOLD));
            budgetP.add(new Chunk((result.baselineRunOutDate() != null ? result.baselineRunOutDate().toString() : "Within budget") + "\n", FONT_REGULAR));
            budgetP.add(new Chunk("Simulated Run-Out Date: ", FONT_BOLD));
            budgetP.add(new Chunk((result.simulatedRunOutDate() != null ? result.simulatedRunOutDate().toString() : "Within budget") + "\n", FONT_REGULAR));
            budgetP.setSpacingAfter(12);
            document.add(budgetP);

            // 7. Decision Recommendation
            addSectionHeading(document, "7. Decision Recommendation");
            Paragraph recP = new Paragraph();
            Font recFont = "APPLY".equalsIgnoreCase(result.recommendation()) ? FONT_GREEN : FONT_RED;
            recP.add(new Chunk("RECOMMENDATION: " + result.recommendation() + "\n\n", recFont));
            recP.add(new Chunk("Assessment: ", FONT_BOLD));
            recP.add(new Chunk(result.recommendationReason() + "\n", FONT_REGULAR));
            document.add(recP);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "PDF rendering error: " + e.getMessage());
        }
    }

    private void addSectionHeading(Document doc, String title) throws DocumentException {
        Paragraph heading = new Paragraph(title, FONT_H2);
        heading.setSpacingBefore(8);
        heading.setSpacingAfter(4);
        doc.add(heading);
    }

    private void addCell(PdfPTable table, String text, Font font, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", font));
        cell.setPadding(4);
        if (isHeader) {
            cell.setBackgroundColor(new Color(241, 245, 249));
        }
        table.addCell(cell);
    }

    private String formatInr(BigDecimal amount) {
        if (amount == null) return "₹0.00";
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return nf.format(amount);
    }
}
