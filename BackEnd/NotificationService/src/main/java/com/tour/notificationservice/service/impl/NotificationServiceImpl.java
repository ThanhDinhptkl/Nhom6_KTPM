package com.tour.notificationservice.service.impl;

import com.tour.notificationservice.entity.Notification;
import com.tour.notificationservice.repository.NotificationRepository;
import com.tour.notificationservice.service.NotificationService;
import com.tour.notificationservice.service.EmailService;
import com.tour.notificationservice.client.CustomerClient;
import com.tour.notificationservice.dto.CustomerResponseDTO;
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

    @Override
    @Transactional
    public Notification createNotification(Notification notification) {
        Notification saved = notificationRepository.save(notification);
        try {
            CustomerResponseDTO customer = customerClient.getCustomerById(notification.getUserId());
            if (customer != null && customer.getEmail() != null && !customer.getEmail().isEmpty()) {
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