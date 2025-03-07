package com.tour.customerservice.controller;

import com.tour.customerservice.model.Customer;
import com.tour.customerservice.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping("/email/{email}")
    public ResponseEntity<Customer> getCustomerByEmail(@PathVariable String email) {
        Customer customer = customerService.findByEmail(email);
        return ResponseEntity.ok(customer);
    }

    @GetMapping("/phone/{phone}")
    public ResponseEntity<Customer> getCustomerByPhone(@PathVariable String phone) {
        Customer customer = customerService.findByPhone(phone);
        return ResponseEntity.ok(customer);
    }

    @GetMapping("/customer/dashboard")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String customerDashboard() {
        return "Welcome to Customer Dashboard!";
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard() {
        return "Welcome to Admin Dashboard!";
    }

    @GetMapping("/guide/dashboard")
    @PreAuthorize("hasRole('GUIDE')")
    public String guideDashboard() {
        return "Welcome to Guide Dashboard!";
    }
}