package com.tour.customerservice.filter;

import com.tour.customerservice.service.CustomerService;
import com.tour.customerservice.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    @Lazy //để trì hoãn khởi tạo
    private CustomerService customerService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;
        String role = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            username = jwtUtil.extractUsername(jwt);
            role = jwtUtil.extractClaim(jwt, claims -> claims.get("role", String.class)); // Lấy role từ JWT
        }

        System.out.println("JWT Token: " + jwt);
        System.out.println("Extracted Username: " + username);
        System.out.println("Extracted Role from JWT: " + role);

        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
        if (username != null && (existingAuth == null || !existingAuth.getName().equals(username))) {
            UserDetails userDetails = this.customerService.loadUserByUsername(username);
            if (jwtUtil.validateToken(jwt, userDetails)) {
                System.out.println("Role from JWT: " + role);

                if (role.startsWith("ROLE_")) {
                    role = role.substring(5);
                }

                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, Collections.singletonList(authority));

                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }else {
            System.out.println("No Authentication needed or already authenticated!");
        }

        filterChain.doFilter(request, response);
    }
}