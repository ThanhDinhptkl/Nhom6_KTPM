package com.tour.customerservice.controller;

import com.tour.customerservice.dto.ChangePasswordDTO;
import com.tour.customerservice.model.Customer;
import com.tour.customerservice.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Customer> getCustomerByEmail(@PathVariable String email) {
        Customer customer = customerService.findByEmail(email);
        return ResponseEntity.ok(customer);
    }

    @GetMapping("/phone/{phone}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Customer> getCustomerByPhone(@PathVariable String phone) {
        Customer customer = customerService.findByPhone(phone);
        return ResponseEntity.ok(customer);
    }

    @PutMapping("/update")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<Customer> updateCustomer(@RequestBody Customer customer) {
        Customer updatedCustomer = customerService.updateCustomer(customer);
        return ResponseEntity.ok(updatedCustomer);
    }

    @PostMapping("/changepassword")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordDTO request) {
        customerService.changePassword(request.getId(), request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Integer id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/resetpassword/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetPassword(@PathVariable Integer id) {
        customerService.resetPassword(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/customerlist")
    @PreAuthorize("hasRole('ADMIN')")
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