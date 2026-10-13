package service;

import dao.AllocationDAO;
import dao.DeveloperDAO;
import dao.LeaveRequestDAO;
import dao.MilestoneDAO;
import dao.ProjectDAO;
import model.Allocation;
import model.Developer;
import model.LeaveRequest;
import model.Milestone;
import model.Project;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * CalendarScheduleService - Computes day-by-day work schedule for employees.
 * Pulls from real Allocation records, Milestone deadlines, and approved Leave records.
 */
public class CalendarScheduleService {

    private final AllocationDAO allocationDAO = new AllocationDAO();
    private final MilestoneDAO milestoneDAO = new MilestoneDAO();
    private final ProjectDAO projectDAO = new ProjectDAO();
    private final LeaveRequestDAO leaveRequestDAO = new LeaveRequestDAO();
    private final DeveloperDAO developerDAO = new DeveloperDAO();

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Feature 2: Computes monthly calendar schedule with daily allocation load,
     * color indicators (green <70%, amber 70-90%, red >90%), milestone deadlines, and leaves.
     */
    public Map<String, Object> getMonthSchedule(int developerId, String monthStr) throws SQLException {
        YearMonth ym;
        try {
            ym = (monthStr != null && monthStr.matches("\\d{4}-\\d{2}")) ?
                    YearMonth.parse(monthStr) : YearMonth.now();
        } catch (Exception e) {
            ym = YearMonth.now();
        }

        LocalDate firstDay = ym.atDay(1);
        LocalDate lastDay = ym.atEndOfMonth();

        return buildScheduleResponse(developerId, firstDay, lastDay, "MONTH", ym.toString());
    }

    /**
     * Feature 2 (Secondary option): Computes 7-day week schedule.
     */
    public Map<String, Object> getWeekSchedule(int developerId, String weekStartDateStr) throws SQLException {
        LocalDate start;
        try {
            start = (weekStartDateStr != null && !weekStartDateStr.isEmpty()) ?
                    LocalDate.parse(weekStartDateStr, ISO_FORMATTER) : LocalDate.now();
        } catch (Exception e) {
            start = LocalDate.now();
        }
        LocalDate end = start.plusDays(6);

        return buildScheduleResponse(developerId, start, end, "WEEK", start.toString());
    }

    /**
     * Details for a specific clicked date.
     */
    public Map<String, Object> getDayDetail(int developerId, String dateStr) throws SQLException {
        LocalDate target = LocalDate.parse(dateStr, ISO_FORMATTER);
        Map<String, Object> schedule = buildScheduleResponse(developerId, target, target, "DAY", dateStr);
        List<Map<String, Object>> days = (List<Map<String, Object>>) schedule.get("days");
        if (days != null && !days.isEmpty()) {
            return days.get(0);
        }
        return Collections.emptyMap();
    }

    private Map<String, Object> buildScheduleResponse(int developerId, LocalDate fromDate, LocalDate toDate, String viewType, String periodKey) throws SQLException {
        Developer dev = developerDAO.findById(developerId);
        List<Allocation> allDevAllocations = allocationDAO.findByDeveloperId(developerId);
        List<LeaveRequest> devLeaves = leaveRequestDAO.findByDeveloperId(developerId);

        // Fetch milestone and project lookup maps
        Map<Integer, Milestone> milestoneMap = new HashMap<>();
        Map<Integer, Project> projectMap = new HashMap<>();

        for (Allocation a : allDevAllocations) {
            if (!milestoneMap.containsKey(a.getMilestoneId())) {
                Milestone m = milestoneDAO.findById(a.getMilestoneId());
                if (m != null) {
                    milestoneMap.put(m.getId(), m);
                    if (!projectMap.containsKey(m.getProjectId())) {
                        Project p = projectDAO.findById(m.getProjectId());
                        if (p != null) projectMap.put(p.getId(), p);
                    }
                }
            }
        }

        List<Map<String, Object>> dayList = new ArrayList<>();
        LocalDate curr = fromDate;

        while (!curr.isAfter(toDate)) {
            String currStr = curr.format(ISO_FORMATTER);
            int dailyLoad = 0;
            List<Map<String, Object>> activeAllocations = new ArrayList<>();
            List<Map<String, Object>> deadlines = new ArrayList<>();
            Map<String, Object> dayLeave = null;

            // Check active allocations for this day
            for (Allocation a : allDevAllocations) {
                LocalDate aStart = LocalDate.parse(a.getStartDate(), ISO_FORMATTER);
                LocalDate aEnd = LocalDate.parse(a.getEndDate(), ISO_FORMATTER);

                if (!curr.isBefore(aStart) && !curr.isAfter(aEnd)) {
                    dailyLoad += a.getAllocationPercentage();

                    Map<String, Object> allocDto = new HashMap<>();
                    allocDto.put("allocationId", a.getId());
                    allocDto.put("allocationPercentage", a.getAllocationPercentage());
                    allocDto.put("startDate", a.getStartDate());
                    allocDto.put("endDate", a.getEndDate());

                    Milestone m = milestoneMap.get(a.getMilestoneId());
                    if (m != null) {
                        allocDto.put("milestoneName", m.getName());
                        allocDto.put("milestoneStatus", m.getStatus());
                        Project p = projectMap.get(m.getProjectId());
                        allocDto.put("projectName", p != null ? p.getName() : "Unknown Project");
                    } else {
                        allocDto.put("milestoneName", "Milestone #" + a.getMilestoneId());
                        allocDto.put("projectName", "Project #" + a.getProjectId());
                    }
                    activeAllocations.add(allocDto);
                }

                // Check milestone deadline falling on this exact date
                Milestone m = milestoneMap.get(a.getMilestoneId());
                if (m != null && m.getEndDate() != null && m.getEndDate().equals(currStr)) {
                    Map<String, Object> dl = new HashMap<>();
                    dl.put("milestoneId", m.getId());
                    dl.put("milestoneName", m.getName());
                    dl.put("projectName", projectMap.get(m.getProjectId()) != null ? projectMap.get(m.getProjectId()).getName() : "Project");
                    deadlines.add(dl);
                }
            }

            // Check approved leave covering this day
            for (LeaveRequest lr : devLeaves) {
                if ("APPROVED".equalsIgnoreCase(lr.getStatus())) {
                    LocalDate lStart = LocalDate.parse(lr.getStartDate(), ISO_FORMATTER);
                    LocalDate lEnd = LocalDate.parse(lr.getEndDate(), ISO_FORMATTER);
                    if (!curr.isBefore(lStart) && !curr.isAfter(lEnd)) {
                        dayLeave = new HashMap<>();
                        dayLeave.put("leaveId", lr.getId());
                        dayLeave.put("reason", lr.getReason());
                        dayLeave.put("status", lr.getStatus());
                        break;
                    }
                }
            }

            // Color coding: green <70%, amber 70-90%, red >90%
            String loadColor = "none";
            if (dailyLoad > 0) {
                if (dailyLoad < 70) {
                    loadColor = "green";
                } else if (dailyLoad <= 90) {
                    loadColor = "amber";
                } else {
                    loadColor = "red";
                }
            }

            Map<String, Object> dayMap = new HashMap<>();
            dayMap.put("date", currStr);
            dayMap.put("dayOfWeek", curr.getDayOfWeek().toString());
            dayMap.put("dayOfMonth", curr.getDayOfMonth());
            dayMap.put("totalAllocationPercentage", dailyLoad);
            dayMap.put("loadColor", loadColor);
            dayMap.put("hasWork", dailyLoad > 0);
            dayMap.put("allocations", activeAllocations);
            dayMap.put("milestoneDeadlines", deadlines);
            dayMap.put("leave", dayLeave);
            dayMap.put("isWeekend", curr.getDayOfWeek().getValue() >= 6);

            dayList.add(dayMap);
            curr = curr.plusDays(1);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("developerId", developerId);
        response.put("developerName", dev != null ? dev.getName() : "Developer");
        response.put("viewType", viewType);
        response.put("period", periodKey);
        response.put("startDate", fromDate.format(ISO_FORMATTER));
        response.put("endDate", toDate.format(ISO_FORMATTER));
        response.put("days", dayList);

        return response;
    }
}
