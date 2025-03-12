package com.tour.customerservice.controller;

import com.tour.customerservice.dto.CustomerLoginDTO;
import com.tour.customerservice.model.Customer;
import com.tour.customerservice.service.CustomerService;
import com.tour.customerservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomerService customUserDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody CustomerLoginDTO customer) throws Exception {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(customer.getEmail(), customer.getPassword()));

        final UserDetails userDetails = customUserDetailsService.loadUserByUsername(customer.getEmail());
        final String jwt = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(jwt);
    }
    @PostMapping("/register")
    public ResponseEntity<?> registerCustomer(@RequestBody Customer customer) {
        try {
            // Đăng ký người dùng mới
            Customer registeredCustomer = customUserDetailsService.registerCustomer(customer);
            return ResponseEntity.ok(registeredCustomer);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}