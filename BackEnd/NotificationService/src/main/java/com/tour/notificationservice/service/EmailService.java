package com.tour.notificationservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            
            // Chuyển đổi text thành HTML
            String htmlContent = text.replace("\n", "<br>");
            // Thêm style cho email
            htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
                         "<h2 style='color: #2c3e50;'>" + subject + "</h2>" +
                         "<div style='background-color: #f8f9fa; padding: 20px; border-radius: 5px;'>" +
                         htmlContent +
                         "</div>" +
                         "</div>";
            
            helper.setText(htmlContent, true); // true để cho phép HTML
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Lỗi khi gửi email: " + e.getMessage());
        }
    }
} 