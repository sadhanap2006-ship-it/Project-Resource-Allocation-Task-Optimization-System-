package com.sist.rms.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sist.rms.dto.SimulationResponse;
import com.sist.rms.exception.ValidationException;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * EXPORTS THE WHAT-IF REPORT AS A PDF.
 *
 * Why an extra library?
 *   Java has no PDF writer in its standard library, and the specification
 *   asks for a PDF export. OpenPDF is a small, well-established, pure-Java
 *   library with a very simple API: you create a Document, add Paragraphs
 *   and Tables, and it writes the PDF bytes.
 *
 * Can the project work without it?
 *   Yes. The simulator page also has a "Print / Save as PDF" button that
 *   uses the browser's own print dialog. OpenPDF only adds a proper
 *   server-side download at GET /api/simulations/{id}/report.
 */
@Service
public class ReportService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private static final Font TITLE_FONT   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
    private static final Font HEADING_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
    private static final Font LABEL_FONT   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font BODY_FONT    = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font SMALL_FONT   = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

    private static final Color HEADER_BG = new Color(31, 78, 121);
    private static final Color STRIPE_BG = new Color(240, 244, 248);

    /** Builds the PDF in memory and returns its bytes. */
    public byte[] buildSimulationReport(SimulationResponse simulation) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // ---------- title ----------
            Paragraph title = new Paragraph("What-If Resource Swap Report", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4f);
            document.add(title);

            Paragraph subtitle = new Paragraph("Project Resource Allocation System", BODY_FONT);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(16f);
            document.add(subtitle);

            // ---------- header facts ----------
            document.add(labelled("Scenario", nullSafe(simulation.getScenarioName(), "(not saved)")));
            document.add(labelled("Project", nullSafe(simulation.getProjectName(), "-")));
            document.add(labelled("Milestone", nullSafe(simulation.getMilestoneName(), "-")));
            document.add(labelled("Generated on",
                    java.time.LocalDate.now().format(DATE)));

            document.add(Chunk.NEWLINE);

            // ---------- comparison table ----------
            PdfPTable table = new PdfPTable(new float[]{2.4f, 2.3f, 2.3f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(8f);

            table.addCell(headerCell("Item"));
            table.addCell(headerCell("Current"));
            table.addCell(headerCell("Simulated"));

            int row = 0;
            row = addRow(table, row, "Resource",
                    nullSafe(simulation.getCurrentDeveloperName(), "-"),
                    nullSafe(simulation.getNewDeveloperName(), "-"));
            row = addRow(table, row, "Hourly cost",
                    money(simulation.getCurrentHourlyCost()),
                    money(simulation.getNewHourlyCost()));
            row = addRow(table, row, "Allocation",
                    percent(simulation.getCurrentAllocationPercentage()),
                    percent(simulation.getSimulatedAllocationPercentage()));
            row = addRow(table, row, "Cost for this milestone",
                    money(simulation.getCurrentCost()),
                    money(simulation.getSimulatedCost()));
            row = addRow(table, row, "Skill match",
                    percent(simulation.getCurrentSkillMatch()),
                    percent(simulation.getSimulatedSkillMatch()));
            row = addRow(table, row, "Missing skills",
                    simulation.getCurrentMissingSkills().isEmpty() ? "None"
                            : String.join(", ", simulation.getCurrentMissingSkills()),
                    simulation.getSimulatedMissingSkills().isEmpty() ? "None"
                            : String.join(", ", simulation.getSimulatedMissingSkills()));
            row = addRow(table, row, "Conflicts",
                    String.valueOf(nullSafeInt(simulation.getCurrentConflicts())),
                    String.valueOf(nullSafeInt(simulation.getSimulatedConflicts())));
            row = addRow(table, row, "Estimated delivery date",
                    simulation.getCurrentDeliveryDate() == null ? "Not estimated"
                            : simulation.getCurrentDeliveryDate().format(DATE),
                    simulation.getSimulatedDeliveryDate() == null ? "Not estimated"
                            : simulation.getSimulatedDeliveryDate().format(DATE));
            addRow(table, row, "Budget impact", "-", money(simulation.getBudgetImpact()));

            document.add(table);

            // ---------- narrative ----------
            document.add(Chunk.NEWLINE);
            document.add(labelled("Delivery impact",
                    nullSafe(simulation.getDeliveryImpactMessage(), "-")));
            document.add(labelled("Recommendation",
                    nullSafe(simulation.getRecommendation(), "-")));

            document.add(Chunk.NEWLINE);
            Paragraph method = new Paragraph(
                    "How these numbers are produced: cost = working days x 8 hours x allocation percentage x hourly "
                  + "rate. Delivery date = milestone start + ceil(estimated effort in developer-days / total "
                  + "capacity) - 1 day. Conflicts are found by adding up the allocations that overlap in time and "
                  + "comparing the total with the developer's availability. All of this is ordinary arithmetic; "
                  + "no AI or machine-learning model is used.", SMALL_FONT);
            document.add(method);

            Paragraph footer = new Paragraph(
                    "Running a simulation does not change any project data. This report describes a hypothetical "
                  + "scenario only.", SMALL_FONT);
            footer.setSpacingBefore(6f);
            document.add(footer);

            document.close();
            return out.toByteArray();

        } catch (DocumentException ex) {
            throw new ValidationException("Could not generate the PDF report: " + ex.getMessage());
        }
    }

    // -----------------------------------------------------------------
    //  small helpers
    // -----------------------------------------------------------------

    private Paragraph labelled(String label, String value) {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Chunk(label + ": ", LABEL_FONT));
        paragraph.add(new Chunk(value, BODY_FONT));
        paragraph.setSpacingAfter(3f);
        return paragraph;
    }

    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADING_FONT));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(6f);
        return cell;
    }

    private int addRow(PdfPTable table, int rowIndex, String label, String current, String simulated) {
        Color background = rowIndex % 2 == 0 ? Color.WHITE : STRIPE_BG;
        table.addCell(bodyCell(label, LABEL_FONT, background));
        table.addCell(bodyCell(current, BODY_FONT, background));
        table.addCell(bodyCell(simulated, BODY_FONT, background));
        return rowIndex + 1;
    }

    private PdfPCell bodyCell(String text, Font font, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(background);
        cell.setPadding(5f);
        return cell;
    }

    private String nullSafe(String value, String fallback) {
        return (value == null || value.trim().isEmpty()) ? fallback : value;
    }

    private int nullSafeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String money(java.math.BigDecimal value) {
        return value == null ? "-" : "Rs. " + value.toPlainString();
    }

    private String percent(Integer value) {
        return value == null ? "-" : value + "%";
    }
}
