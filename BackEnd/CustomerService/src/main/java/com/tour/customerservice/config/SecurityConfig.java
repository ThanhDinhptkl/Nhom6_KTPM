package com.tour.customerservice.config;

import com.tour.customerservice.filter.JwtAuthenticationFilter;
import com.tour.customerservice.filter.RateLimitFilter;
import com.tour.customerservice.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    @Lazy
    private CustomerService customerService;

    @Autowired
    @Lazy
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private RateLimitFilter rateLimitFilter;

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
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/customer/auth/**", "/oauth2/**", "/api/rate-limit/status",
                                "/api/circuit-breaker/**", "/actuator/**")
                        .permitAll() // Cho phep truy cap cong khai
                        .requestMatchers("/customer/email/**").hasAnyRole("ADMIN") // Phân quyền cho ADMIN
                        .requestMatchers("/customer/phone/**").hasAnyRole("ADMIN") // Phân quyền cho ADMIN
                        .requestMatchers("/customer/update").hasAnyRole("CUSTOMER", "ADMIN") // Phân quyền cho CUSTOMER
                                                                                             // và ADMIN
                        .requestMatchers("/customer/changepassword").hasAnyRole("CUSTOMER", "ADMIN") // Phân quyền cho
                                                                                                     // CUSTOMER và
                                                                                                     // ADMIN
                        .requestMatchers("/customer/delete/**").hasRole("ADMIN") // Chỉ ADMIN mới được xóa người dùng
                        .requestMatchers("/customer/resetpassword/**").hasRole("ADMIN") // Chỉ ADMIN mới được reset
                                                                                        // password
                        .requestMatchers("/customer/customerlist").hasRole("ADMIN") // Chỉ ADMIN mới được xem danh sách
                                                                                    // khách hàng
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService()))
                        .defaultSuccessUrl("/customer/auth/login/google", true));

        // Thêm Rate Limit Filter trước các filter khác
        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        // Thêm JWT Filter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        http.authenticationProvider(authenticationProvider());

        return http.build();
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService() {
        return new DefaultOAuth2UserService();
    }
}