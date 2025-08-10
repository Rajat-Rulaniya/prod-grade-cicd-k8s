package com.inventory.management.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            final String authorizationHeader = request.getHeader("Authorization");
            
            String username = null;
            String jwt = null;
            
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                jwt = authorizationHeader.substring(7);
                try {
                    username = jwtUtil.extractUsername(jwt);
                    if (username != null) {
                        System.out.println("JWT Filter - Username extracted: " + username);
                    } else {
                        System.out.println("JWT Filter - Failed to extract username from token");
                    }
                } catch (Exception e) {
                    System.out.println("JWT Filter - Error extracting username: " + e.getMessage());
                }
            } else {
                System.out.println("JWT Filter - No Authorization header or invalid format for: " + request.getRequestURI());
            }
            
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                    
                    if (jwtUtil.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        System.out.println("JWT Filter - Authentication successful for user: " + username);
                    } else {
                        System.out.println("JWT Filter - Token validation failed for user: " + username);
                    }
                } catch (Exception e) {
                    System.out.println("JWT Filter - Error loading user details: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("JWT Filter - General error: " + e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
}
