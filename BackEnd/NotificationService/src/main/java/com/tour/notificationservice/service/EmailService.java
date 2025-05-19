package com.tour.notificationservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
    
    public void sendSimpleMessageBooking(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            
            // Simplified HTML template with minimal styling
            String htmlContent = "<div style='font-family:Arial;max-width:600px;margin:0 auto;padding:20px'>" +
                               "<div style='background:#fff;border-radius:8px;box-shadow:0 2px 4px rgba(0,0,0,0.1)'>" +
                               "<div style='padding:20px'>" +
                               text +
                               "</div>" +
                               "<div style='background:#f8f9fa;padding:15px;border-top:1px solid #e9ecef;border-radius:0 0 8px 8px'>" +
                               "<p style='margin:0;color:#6c757d;font-size:14px'>" +
                               "Trân trọng,<br>Đội ngũ Tour Booking" +
                               "</p>" +
                               "</div>" +
                               "</div>" +
                               "</div>";
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Lỗi khi gửi email: " + e.getMessage());
        }
    }
} 