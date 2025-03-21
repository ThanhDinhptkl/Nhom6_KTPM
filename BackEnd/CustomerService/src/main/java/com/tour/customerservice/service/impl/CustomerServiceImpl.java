package com.tour.customerservice.service.impl;

import com.tour.customerservice.model.Customer;
import com.tour.customerservice.repository.CustomerRepository;
import com.tour.customerservice.service.CustomerService;
import com.tour.customerservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Customer registerCustomer(Customer customer) {
        // Mã hóa mật khẩu trước khi lưu vào database
        customer.setPassword(passwordEncoder.encode(customer.getPassword()));
        return customerRepository.save(customer);
    }

    @Override
    public Customer findByEmail(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Override
    public Customer findByPhone(String phone) {
        return customerRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found with phone: " + phone));
    }

    @Override
    public List<Customer> findAllCustomers() {
        return customerRepository.findAll();
    }

    @Override
    public Customer updateCustomer(String token, Customer updatedCustomer) {
        try {
            String currentUserEmail = jwtUtil.extractUsername(token);
            System.out.println("currentUserEmail: " + currentUserEmail);
            if (currentUserEmail == null || currentUserEmail.isEmpty()) {
                throw new RuntimeException("Invalid token");
            }

            Customer existingCustomer = customerRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Chỉ cho phép cập nhật nếu email trong token khớp với email của khách hàng
            if (!existingCustomer.getEmail().equals(updatedCustomer.getEmail())) {
                throw new RuntimeException("Unauthorized to update this account");
            }

            // Chỉ cập nhật những thông tin cần thiết (tránh ghi đè password)
            existingCustomer.setName(updatedCustomer.getName());
            existingCustomer.setPhone(updatedCustomer.getPhone());

            return customerRepository.save(existingCustomer);
        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired token");
        }
    }

    @Override
    public void deleteCustomer(Integer id) {
        customerRepository.deleteById(id);
    }

    @Override
    public void resetPassword(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        customer.setPassword(passwordEncoder.encode("12345")); // Reset password về 12345
        customerRepository.save(customer);
    }

    @Override
    public void changePassword(String token, String oldPassword, String newPassword) {
        try {
            // Kiểm tra token hợp lệ
            if (token == null || token.isEmpty()) {
                throw new RuntimeException("Invalid token format");
            }

            // Lấy email từ JWT
            String email;
            try {
                email = jwtUtil.extractUsername(token.replace("Bearer ", ""));
            } catch (Exception e) {
                throw new RuntimeException("Invalid or expired token");
            }

            // Tìm người dùng
            Customer customer = findByEmail(email);

            if (!passwordEncoder.matches(oldPassword, customer.getPassword())) {
                throw new RuntimeException("Old password is incorrect");
            }
            customer.setPassword(passwordEncoder.encode(newPassword));
            customerRepository.save(customer);
        }catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new RuntimeException("Token has expired, please login again.");
        } catch (io.jsonwebtoken.SignatureException e) {
            throw new RuntimeException("Invalid JWT signature.");
        } catch (Exception e) {
            throw new RuntimeException("Invalid token format.");
        }
    }

    @Override
    public Customer saveCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Customer customer = findByEmail(email);
        return new org.springframework.security.core.userdetails.User(
                customer.getEmail(), customer.getPassword(), customer.getAuthorities());
    }
}