package service;

import dao.NotificationDAO;
import dto.NotificationResponseDTO;
import model.Notification;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {

    private final NotificationDAO notificationDAO = new NotificationDAO();

    public List<NotificationResponseDTO> getNotifications(Integer userId) throws SQLException {
        List<Notification> list = (userId != null) ? notificationDAO.findByUserId(userId) : notificationDAO.findAll();
        List<NotificationResponseDTO> dtos = new ArrayList<>();
        for (Notification n : list) {
            NotificationResponseDTO dto = new NotificationResponseDTO();
            dto.setId(n.getId());
            dto.setUserId(n.getUserId());
            dto.setTitle(n.getTitle());
            dto.setMessage(n.getMessage());
            dto.setType(n.getType());
            dto.setSeverity(n.getSeverity());
            dto.setRead(n.isRead());
            dto.setCreatedAt(n.getCreatedAt());
            dtos.add(dto);
        }
        return dtos;
    }

    public boolean markAsRead(int notificationId) throws SQLException {
        return notificationDAO.markAsRead(notificationId);
    }

    public boolean markAllAsRead() throws SQLException {
        return notificationDAO.markAllAsRead();
    }
}
