package com.tour.notificationservice.service.impl;

import com.tour.notificationservice.entity.Notification;
import com.tour.notificationservice.repository.NotificationRepository;
import com.tour.notificationservice.service.NotificationService;
import com.tour.notificationservice.service.EmailService;
import com.tour.notificationservice.client.CustomerClient;
import com.tour.notificationservice.dto.CustomerResponseDTO;
import com.tour.notificationservice.client.TourClient;
import com.tour.notificationservice.dto.TourResponseDTO;
import com.tour.notificationservice.client.BookingClient;
import com.tour.notificationservice.dto.BookingResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.SimpleDateFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final CustomerClient customerClient;
    private final TourClient tourClient;
    private final BookingClient bookingClient;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

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
                                            "<img src='%s' alt='Tour Image' style='max-width: 100%%; height: auto; border-radius: 8px;'/>"
                                            +
                                            "</div>" +
                                            "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px;'>"
                                            +
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
                                    tour.getImage() != null ? tour.getImage()
                                            : "https://via.placeholder.com/400x300?text=No+Image",
                                    tour.getLocation(),
                                    tour.getStart_date(),
                                    tour.getEnd_date(),
                                    tour.getDuration(),
                                    tour.getPrice(),
                                    tour.getMax_participants());
                            notification.setMessage(detailedMessage);
                            saved = notificationRepository.save(notification);
                        }
                    } catch (Exception e) {
                        System.err.println("Không thể lấy thông tin tour: " + e.getMessage());
                    }
                }
                emailService.sendSimpleMessageBooking(customer.getEmail(), notification.getTitle(),
                        notification.getMessage());
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

    @Override
    @Transactional
    public Notification createPaymentSuccessNotification(Integer userId, Integer tourId, Integer bookingId) {
        try {
            // Lấy thông tin khách hàng, tour và booking
            CustomerResponseDTO customer = customerClient.getCustomerById(userId);
            if (customer == null) {
                throw new RuntimeException("Không tìm thấy thông tin khách hàng với ID: " + userId);
            }

            TourResponseDTO tour = tourClient.getTourById(tourId);
            if (tour == null) {
                throw new RuntimeException("Không tìm thấy thông tin tour với ID: " + tourId);
            }

            BookingResponseDTO booking = bookingClient.getBookingById(bookingId);
            if (booking == null) {
                throw new RuntimeException("Không tìm thấy thông tin booking với ID: " + bookingId);
            }

            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setTourId(tourId);
            notification.setTitle("Thanh toán thành công");
            notification.setType("PAYMENT");

            // Tạo nội dung HTML với ảnh - sử dụng format ngắn gọn hơn
            String detailedMessage = String.format(
                    "<h3 style='color:#2c3e50'>Thanh toán tour '%s' thành công!</h3>" +
                            "<img src='%s' alt='Tour' style='max-width:100%%;height:auto;border-radius:8px;margin:10px 0'/>"
                            +
                            "<div style='background:#f8f9fa;padding:15px;border-radius:5px'>" +
                            "<h4 style='color:#2c3e50;margin:0 0 10px'>Thông tin thanh toán:</h4>" +
                            "<ul style='list-style:none;padding:0;margin:0'>" +
                            "<li style='margin:5px 0'><b>Mã tour:</b> %d</li>" +
                            "<li style='margin:5px 0'><b>Tour:</b> %s</li>" +
                            "<li style='margin:5px 0'><b>Địa điểm:</b> %s</li>" +
                            "<li style='margin:5px 0'><b>Ngày đi:</b> %s</li>" +
                            "<li style='margin:5px 0'><b>Số người:</b> %d</li>" +
                            "<li style='margin:5px 0'><b>Tổng tiền:</b> %.2f VND</li>" +
                            "</ul>" +
                            "</div>" +
                            "<div style='margin-top:15px;padding:10px;background:#e8f5e9;border-radius:5px'>" +
                            "<p style='margin:0;color:#2e7d32'>Chúng tôi sẽ liên hệ với bạn sớm nhất để cung cấp thêm thông tin.</p>"
                            +
                            "</div>",
                    tour.getTitle(),
                    tour.getImage() != null ? tour.getImage() : "https://via.placeholder.com/400x300?text=No+Image",
                    booking.getId(),
                    tour.getTitle(),
                    tour.getLocation(),
                    dateFormat.format(tour.getStart_date()),
                    booking.getNumber_of_people(),
                    booking.getTotal_price());

            notification.setMessage(detailedMessage);

            // Lưu thông báo và gửi email
            Notification saved = notificationRepository.save(notification);
            try {
                if (customer.getEmail() != null && !customer.getEmail().isEmpty()) {
                    emailService.sendSimpleMessageBooking(
                            customer.getEmail(),
                            "Xác nhận thanh toán thành công - Tour " + tour.getTitle(),
                            notification.getMessage());
                    System.out.println("Đã gửi email xác nhận thanh toán tới: " + customer.getEmail());
                } else {
                    System.err.println("Không tìm thấy email cho userId: " + userId);
                }
            } catch (Exception e) {
                System.err.println(
                        "Không gửi được email xác nhận thanh toán cho userId " + userId + ": " + e.getMessage());
            }
            return saved;
        } catch (Exception e) {
            System.err.println("Lỗi khi tạo thông báo thanh toán: " + e.getMessage());
            throw new RuntimeException("Không thể tạo thông báo thanh toán: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Notification createBookingSuccessNotification(Integer userId, Integer tourId, Integer bookingId) {
        try {
            // Lấy thông tin khách hàng, tour và booking
            CustomerResponseDTO customer = customerClient.getCustomerById(userId);
            if (customer == null) {
                throw new RuntimeException("Không tìm thấy thông tin khách hàng với ID: " + userId);
            }

            TourResponseDTO tour = tourClient.getTourById(tourId);
            if (tour == null) {
                throw new RuntimeException("Không tìm thấy thông tin tour với ID: " + tourId);
            }

            BookingResponseDTO booking = bookingClient.getBookingById(bookingId);
            if (booking == null) {
                throw new RuntimeException("Không tìm thấy thông tin booking với ID: " + bookingId);
            }

            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setTourId(tourId);
            notification.setTitle("Đặt tour thành công");
            notification.setType("BOOKING");
            notification.setMessage(String.format(
                    "Kính gửi %s,\n\n" +
                            "Chúc mừng bạn đã đặt tour thành công!\n\n" +
                            "Chi tiết đặt tour:\n" +
                            "- Mã đặt tour: %d\n" +
                            "- Tour: %s\n" +
                            "- Địa điểm: %s\n" +
                            "- Ngày khởi hành dự kiến: %s\n" +
                            "- Số người: %d\n" +
                            "- Tổng tiền: %.2f VND\n" +
                            "- Ngày đặt: %s\n\n" +
                            "Vui lòng tiến hành thanh toán để xác nhận đặt chỗ. Bạn có thể quản lý đặt tour của mình tại phần 'Tour đã đặt' trên website.\n\n"
                            +
                            "Trân trọng,\n" +
                            "Đội ngũ Tour Booking",
                    customer.getName(),
                    booking.getId(),
                    tour.getTitle(),
                    tour.getLocation(),
                    dateFormat.format(booking.getBooking_date()), // Use booking date as start date is not confirmed yet
                    booking.getNumber_of_people(),
                    booking.getTotal_price(),
                    dateFormat.format(booking.getBooking_date())));

            // Lưu thông báo và gửi email
            Notification saved = notificationRepository.save(notification);
            try {
                if (customer.getEmail() != null && !customer.getEmail().isEmpty()) {
                    emailService.sendSimpleMessageBooking(
                            customer.getEmail(),
                            "Xác nhận đặt tour thành công - Tour " + tour.getTitle(),
                            notification.getMessage());
                    System.out.println("Đã gửi email xác nhận đặt tour tới: " + customer.getEmail());
                } else {
                    System.err.println("Không tìm thấy email cho userId: " + userId);
                }
            } catch (Exception e) {
                System.err
                        .println("Không gửi được email xác nhận đặt tour cho userId " + userId + ": " + e.getMessage());
            }
            return saved;
        } catch (Exception e) {
            System.err.println("Lỗi khi tạo thông báo đặt tour: " + e.getMessage());
            throw new RuntimeException("Không thể tạo thông báo đặt tour: " + e.getMessage());
        }
    }
}