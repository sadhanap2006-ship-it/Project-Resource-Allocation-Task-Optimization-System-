package service;

import dao.AvailabilityDAO;
import dao.DeveloperDAO;
import dto.AvailabilityRequestDTO;
import dto.AvailabilityResponseDTO;
import model.Availability;
import model.Developer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * AvailabilityService - Decoupled Service Layer for Resource Availability vs. Planned Leave
 * 
 * Architectural Decision Note (B3 Compliance):
 * -----------------------------------------------------------------------------------------
 * Time-bounded approved leaves (which deduct developer working capacity to 0% over a date
 * range) are persisted in the `developer_availability` table with explicit start/end dates
 * and approval statuses. Baseline resource capacity (e.g., full-time 100% vs. part-time 50%)
 * is maintained on the `developers.availability_percentage` column.
 * 
 * Keeping this structure avoids denormalizing two duplicate temporal tables while giving
 * the service layer two distinct, high-level business capabilities:
 *   1) recordPlannedLeave() -> Deducts effective capacity to 0% during the calendar interval.
 *   2) adjustBaseAvailability() -> Modifies the resource's baseline standard capacity.
 */
public class AvailabilityService {

    private final AvailabilityDAO availabilityDAO = new AvailabilityDAO();
    private final DeveloperDAO developerDAO = new DeveloperDAO();

    /**
     * Records a formal planned leave interval (deducts capacity to 0% across dates).
     */
    public AvailabilityResponseDTO recordPlannedLeave(AvailabilityRequestDTO req) throws SQLException {
        Availability a = new Availability();
        a.setDeveloperId(req.getDeveloperId());
        a.setStartDate(req.getStartDate());
        a.setEndDate(req.getEndDate());
        a.setReason(req.getReason() != null ? req.getReason() : "Planned Leave");
        a.setStatus(req.getStatus() != null ? req.getStatus() : "APPROVED");

        int id = availabilityDAO.insert(a);
        a.setId(id);

        Developer dev = developerDAO.findById(req.getDeveloperId());
        String devName = dev != null ? dev.getName() : "Developer #" + req.getDeveloperId();
        a.setDeveloperName(devName);

        return mapToDTO(a, "PLANNED_LEAVE");
    }

    /**
     * Adjusts the developer's baseline standard capacity percentage (e.g. 50% part-time vs 100% full-time).
     */
    public boolean adjustBaseAvailability(int developerId, int availabilityPercentage) throws SQLException {
        Developer dev = developerDAO.findById(developerId);
        if (dev == null) return false;
        dev.setAvailabilityPercentage(Math.max(0, Math.min(100, availabilityPercentage)));
        return developerDAO.update(dev);
    }

    public List<AvailabilityResponseDTO> getAllAvailabilityRecords() throws SQLException {
        List<Availability> list = availabilityDAO.findAll();
        List<AvailabilityResponseDTO> dtos = new ArrayList<>();
        for (Availability a : list) {
            dtos.add(mapToDTO(a, "PLANNED_LEAVE"));
        }
        return dtos;
    }

    public List<AvailabilityResponseDTO> getAvailabilityForDeveloper(int devId) throws SQLException {
        List<Availability> list = availabilityDAO.findByDeveloperId(devId);
        List<AvailabilityResponseDTO> dtos = new ArrayList<>();
        for (Availability a : list) {
            dtos.add(mapToDTO(a, "PLANNED_LEAVE"));
        }
        return dtos;
    }

    public boolean deleteAvailabilityRecord(int id) throws SQLException {
        return availabilityDAO.delete(id);
    }

    private AvailabilityResponseDTO mapToDTO(Availability a, String type) {
        AvailabilityResponseDTO dto = new AvailabilityResponseDTO();
        dto.setId(a.getId());
        dto.setDeveloperId(a.getDeveloperId());
        dto.setDeveloperName(a.getDeveloperName());
        dto.setStartDate(a.getStartDate());
        dto.setEndDate(a.getEndDate());
        dto.setReason(a.getReason());
        dto.setStatus(a.getStatus());
        dto.setType(type);
        return dto;
    }
}
