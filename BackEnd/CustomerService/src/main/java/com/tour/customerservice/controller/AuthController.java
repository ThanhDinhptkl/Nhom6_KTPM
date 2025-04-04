package com.tour.customerservice.controller;

import com.tour.customerservice.dto.CustomerLoginDTO;
import com.tour.customerservice.model.Customer;
import com.tour.customerservice.repository.CustomerRepository;
import com.tour.customerservice.service.CustomerService;
import com.tour.customerservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/customer/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomerService customUserDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomerService customerService;

    private final CustomerRepository customerRepository;

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody CustomerLoginDTO customer) throws Exception {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(customer.getEmail(), customer.getPassword()));

        final UserDetails userDetails = customUserDetailsService.loadUserByUsername(customer.getEmail());
        Map<String, String> tokens = customerService.generateTokens(userDetails);

        return ResponseEntity.ok(tokens);
    }

    public AuthController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping("/login/google")
    public ResponseEntity<?> oauth2Success(Authentication authentication) {
        if (authentication == null || !(authentication instanceof OAuth2AuthenticationToken)) {
            return ResponseEntity.badRequest().body("Google authentication failed: No authentication object");
        }

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = token.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String phone = (String) attributes.get("phone");

        Optional<Customer> existingCustomer = customerRepository.findByEmail(email);
        Customer customer = existingCustomer.orElseGet(() -> {
            Customer newCustomer = new Customer();
            newCustomer.setEmail(email);
            newCustomer.setName(name != null ? name : "Unknown");
            newCustomer.setPhone(phone);
            newCustomer.setRole(Customer.Role.CUSTOMER);
            newCustomer.setPassword(""); // OAuth2 không có password
            return customerRepository.save(newCustomer);
        });

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(customer.getEmail());
        String jwt = jwtUtil.generateToken(userDetails);

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

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().body("Missing refresh token");
        }

        try {
            // Giải mã token và lấy username (email)
            String email = jwtUtil.extractUsername(refreshToken);

            // Tìm người dùng
            Customer customer = customerService.findByEmail(email);
            if (customer == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            // So sánh refreshToken gửi lên với token lưu trong DB
            if (!refreshToken.equals(customer.getRefreshToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
            }

            // Kiểm tra hạn token
            if (jwtUtil.isTokenExpired(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token expired");
            }

            // Tạo token mới
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    customer.getEmail(), customer.getPassword(), new ArrayList<>());
            Map<String, String> tokens = customerService.generateTokens(userDetails);

            return ResponseEntity.ok(tokens);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token format");
        }
    }
}