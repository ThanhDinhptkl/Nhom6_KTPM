package com.tour.customerservice.config;

import com.tour.customerservice.filter.JwtAuthenticationFilter;
import com.tour.customerservice.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    @Lazy
    private CustomerService customerService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customerService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Vô hiệu hóa CSRF
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // Cho phép truy cập công khai
                        .requestMatchers("/api/customer/email/**").hasAnyRole("CUSTOMER", "ADMIN") // Phân quyền cho CUSTOMER và ADMIN
                        .requestMatchers("/api/customer/phone/**").hasAnyRole("CUSTOMER", "ADMIN") // Phân quyền cho CUSTOMER và ADMIN
                        .requestMatchers("/api/customer/update").hasAnyRole("CUSTOMER", "ADMIN") // Phân quyền cho CUSTOMER và ADMIN
                        .requestMatchers("/api/customer/change-password").hasRole("CUSTOMER") // Chỉ CUSTOMER mới được đổi mật khẩu
                        .requestMatchers("/api/admin/delete/**").hasRole("ADMIN") // Chỉ ADMIN mới được xóa người dùng
                        .requestMatchers("/api/resetpassword/**").hasRole("ADMIN") // Chỉ ADMIN mới được reset password
                        .requestMatchers("/api/admin/customerlist").hasRole("ADMIN") // Chỉ ADMIN mới được xem danh sách khách hàng
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Sử dụng session STATELESS
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}