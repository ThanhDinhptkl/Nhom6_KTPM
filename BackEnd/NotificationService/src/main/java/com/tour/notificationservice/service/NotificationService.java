package com.tour.notificationservice.service;

import com.tour.notificationservice.entity.Notification;
import java.util.List;

public interface NotificationService {
    Notification createNotification(Notification notification);
    List<Notification> getUserNotifications(Integer userId);
    List<Notification> getUnreadNotifications(Integer userId);
    Notification markAsRead(Long notificationId);
    void markAllAsRead(Integer userId);
    long getUnreadCount(Integer userId);
    void deleteNotification(Long notificationId);
} 