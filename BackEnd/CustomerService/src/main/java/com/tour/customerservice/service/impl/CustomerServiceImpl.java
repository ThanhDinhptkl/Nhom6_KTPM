package com.tour.customerservice.service.impl;

import com.tour.customerservice.model.Customer;
import com.tour.customerservice.repository.CustomerRepository;
import com.tour.customerservice.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public Customer updateCustomer(Customer customer) {
        return customerRepository.save(customer);
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
    public void changePassword(Integer id, String oldPassword, String newPassword) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(oldPassword, customer.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }

        // Cập nhật mật khẩu mới
        customer.setPassword(passwordEncoder.encode(newPassword));
        customerRepository.save(customer);
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