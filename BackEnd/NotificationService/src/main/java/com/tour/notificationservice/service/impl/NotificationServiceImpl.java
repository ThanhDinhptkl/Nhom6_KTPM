package com.tour.notificationservice.service.impl;

import com.tour.notificationservice.entity.Notification;
import com.tour.notificationservice.repository.NotificationRepository;
import com.tour.notificationservice.service.NotificationService;
import com.tour.notificationservice.service.EmailService;
import com.tour.notificationservice.client.CustomerClient;
import com.tour.notificationservice.client.TourClient;
import com.tour.notificationservice.dto.CustomerResponseDTO;
import com.tour.notificationservice.dto.TourResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final CustomerClient customerClient;
    private final TourClient tourClient;

    @Override
    @Transactional
    public Notification createNotification(Notification notification) {
        Notification saved = notificationRepository.save(notification);
        try {
            CustomerResponseDTO customer = customerClient.getCustomerById(notification.getUserId());
            if (customer != null && customer.getEmail() != null && !customer.getEmail().isEmpty()) {
                // Nếu là thông báo đặt tour và có tourId, lấy thêm thông tin tour
                if ("BOOKING".equals(notification.getType()) && notification.getTourId() != null) {
                    try {
                        TourResponseDTO tour = tourClient.getTourById(notification.getTourId());
                        if (tour != null) {
                            // Tạo nội dung HTML với ảnh
                            String detailedMessage = String.format(
                                "<div style='margin-bottom: 20px;'>" +
                                "<h3 style='color: #2c3e50;'>Bạn đã đặt tour '%s' thành công!</h3>" +
                                "<div style='margin: 20px 0;'>" +
                                "<img src='%s' alt='Tour Image' style='max-width: 100%%; height: auto; border-radius: 8px;'/>" +
                                "</div>" +
                                "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px;'>" +
                                "<h4 style='color: #2c3e50; margin-top: 0;'>Thông tin tour:</h4>" +
                                "<ul style='list-style: none; padding: 0;'>" +
                                "<li style='margin: 10px 0;'><strong>Địa điểm:</strong> %s</li>" +
                                "<li style='margin: 10px 0;'><strong>Ngày bắt đầu:</strong> %s</li>" +
                                "<li style='margin: 10px 0;'><strong>Ngày kết thúc:</strong> %s</li>" +
                                "<li style='margin: 10px 0;'><strong>Thời gian:</strong> %d ngày</li>" +
                                "<li style='margin: 10px 0;'><strong>Giá:</strong> %.2f VND</li>" +
                                "<li style='margin: 10px 0;'><strong>Số người tối đa:</strong> %d</li>" +
                                "</ul>" +
                                "</div>" +
                                "</div>",
                                tour.getTitle(),
                                tour.getImage() != null ? tour.getImage() : "https://via.placeholder.com/400x300?text=No+Image",
                                tour.getLocation(),
                                tour.getStart_date(),
                                tour.getEnd_date(),
                                tour.getDuration(),
                                tour.getPrice(),
                                tour.getMax_participants()
                            );
                            notification.setMessage(detailedMessage);
                            saved = notificationRepository.save(notification);
                        }
                    } catch (Exception e) {
                        System.err.println("Không thể lấy thông tin tour: " + e.getMessage());
                    }
                }
                emailService.sendSimpleMessage(customer.getEmail(), notification.getTitle(), notification.getMessage());
                System.out.println("Đã gửi email tới: " + customer.getEmail());
            } else {
                System.err.println("Không tìm thấy email cho userId: " + notification.getUserId());
            }
        } catch (Exception e) {
            System.err.println("Không gửi được email cho userId " + notification.getUserId() + ": " + e.getMessage());
        }
        return saved;
    }

    @Override
    public List<Notification> getUserNotifications(Integer userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Notification> getUnreadNotifications(Integer userId) {
        return notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);
    }

    @Override
    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Integer userId) {
        List<Notification> unreadNotifications = getUnreadNotifications(userId);
        unreadNotifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    @Override
    public long getUnreadCount(Integer userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
} 