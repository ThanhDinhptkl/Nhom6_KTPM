package com.tour.customerservice.service;

import com.tour.customerservice.model.Customer;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface CustomerService extends UserDetailsService {
    Customer registerCustomer(Customer customer); // Đăng ký người dùng mới
    Customer findByEmail(String email); // Tìm kiếm người dùng theo email
    Customer findByPhone(String phone); // Tìm kiếm người dùng theo số điện thoại
}