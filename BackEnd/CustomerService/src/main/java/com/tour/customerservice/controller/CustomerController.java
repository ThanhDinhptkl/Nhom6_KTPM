package com.tour.customerservice.controller;

import com.tour.customerservice.dto.ChangePasswordDTO;
import com.tour.customerservice.model.Customer;
import com.tour.customerservice.service.CustomerService;
import com.tour.customerservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private JwtUtil jwtUtil;

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

    @PutMapping("/update")
    public ResponseEntity<?> updateCustomer(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestBody Customer customer) {
        try {
            Customer updatedCustomer = customerService.updateCustomer(token.replace("Bearer ", ""), customer);
            return ResponseEntity.ok(updatedCustomer);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/changepassword")
    public ResponseEntity<?> changePassword(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestBody ChangePasswordDTO request) {
        try {
            customerService.changePassword(token.replace("Bearer ", ""), request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Integer id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/resetpassword/{id}")
    public ResponseEntity<Void> resetPassword(@PathVariable Integer id) {
        customerService.resetPassword(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/customerlist")
    public ResponseEntity<List<Customer>> getAllCustomers() {
        List<Customer> customers = customerService.findAllCustomers();
        return ResponseEntity.ok(customers);
    }

//    @GetMapping("/customer/dashboard")
//    @PreAuthorize("hasRole('CUSTOMER')")
//    public String customerDashboard() {
//        return "Welcome to Customer Dashboard!";
//    }
//
//    @GetMapping("/admin/dashboard")
//    @PreAuthorize("hasRole('ADMIN')")
//    public String adminDashboard() {
//        return "Welcome to Admin Dashboard!";
//    }
//
//    @GetMapping("/guide/dashboard")
//    @PreAuthorize("hasRole('GUIDE')")
//    public String guideDashboard() {
//        return "Welcome to Guide Dashboard!";
//    }
}