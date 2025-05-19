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
            notification.setTitle("Thanh toán thành công");
            notification.setType("PAYMENT");
            notification.setMessage(String.format(
                "Kính gửi %s,\n\n" +
                "Chúng tôi xin xác nhận rằng khoản thanh toán của bạn đã được xử lý thành công.\n\n" +
                "Chi tiết đặt tour:\n" +
                "- Mã đặt tour: %d\n" +
                "- Tour: %s\n" +
                "- Địa điểm: %s\n" +
                "- Ngày khởi hành: %s\n" +
                "- Ngày kết thúc: %s\n" +
                "- Số người: %d\n" +
                "- Tổng tiền: %.2f VND\n" +
                "- Ngày đặt: %s\n\n" +
                "Chúng tôi sẽ liên hệ với bạn trong thời gian sớm nhất để cung cấp thêm thông tin chi tiết về tour.\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ Tour Booking",
                customer.getName(),
                booking.getId(),
                tour.getTitle(),
                tour.getLocation(),
                dateFormat.format(tour.getStart_date()),
                dateFormat.format(tour.getEnd_date()),
                booking.getNumber_of_people(),
                booking.getTotal_price(),
                dateFormat.format(booking.getBooking_date())
            ));

            // Lưu thông báo và gửi email
            Notification saved = notificationRepository.save(notification);
            try {
                if (customer.getEmail() != null && !customer.getEmail().isEmpty()) {
                    emailService.sendSimpleMessage(
                        customer.getEmail(),
                        "Xác nhận thanh toán thành công - Tour " + tour.getTitle(),
                        notification.getMessage()
                    );
                    System.out.println("Đã gửi email xác nhận thanh toán tới: " + customer.getEmail());
                } else {
                    System.err.println("Không tìm thấy email cho userId: " + userId);
                }
            } catch (Exception e) {
                System.err.println("Không gửi được email xác nhận thanh toán cho userId " + userId + ": " + e.getMessage());
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
                "Vui lòng tiến hành thanh toán để xác nhận đặt chỗ. Bạn có thể quản lý đặt tour của mình tại phần 'Tour đã đặt' trên website.\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ Tour Booking",
                customer.getName(),
                booking.getId(),
                tour.getTitle(),
                tour.getLocation(),
                 dateFormat.format(booking.getBooking_date()), // Use booking date as start date is not confirmed yet
                booking.getNumber_of_people(),
                booking.getTotal_price(),
                dateFormat.format(booking.getBooking_date())
            ));

            // Lưu thông báo và gửi email
            Notification saved = notificationRepository.save(notification);
            try {
                if (customer.getEmail() != null && !customer.getEmail().isEmpty()) {
                    emailService.sendSimpleMessage(
                        customer.getEmail(),
                        "Xác nhận đặt tour thành công - Tour " + tour.getTitle(),
                        notification.getMessage()
                    );
                    System.out.println("Đã gửi email xác nhận đặt tour tới: " + customer.getEmail());
                } else {
                    System.err.println("Không tìm thấy email cho userId: " + userId);
                }
            } catch (Exception e) {
                System.err.println("Không gửi được email xác nhận đặt tour cho userId " + userId + ": " + e.getMessage());
            }
            return saved;
        } catch (Exception e) {
            System.err.println("Lỗi khi tạo thông báo đặt tour: " + e.getMessage());
            throw new RuntimeException("Không thể tạo thông báo đặt tour: " + e.getMessage());
        }
    }
} 