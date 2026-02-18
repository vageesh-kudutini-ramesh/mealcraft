package com.mealcraft.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter
 * 
 * Intercepts HTTP requests and validates JWT tokens.
 * Extracts token from Authorization header and sets authentication in SecurityContext.
 * 
 * @author MealCraft Team
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private UserDetailsService userDetailsService;
    private JwtUtil jwtUtil;
    
    public JwtAuthenticationFilter() {
        System.out.println("[JWT Filter] ========== FILTER CONSTRUCTOR CALLED ==========");
    }
    
    public void setUserDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
        System.out.println("[JWT Filter] UserDetailsService SET: " + (userDetailsService != null ? "OK" : "NULL"));
    }
    
    public void setJwtUtil(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        System.out.println("[JWT Filter] JwtUtil SET: " + (jwtUtil != null ? "OK" : "NULL"));
    }
    
    public void initialize() {
        System.out.println("[JWT Filter] ========== FILTER INITIALIZED ==========");
        System.out.println("[JWT Filter] UserDetailsService: " + (userDetailsService != null ? "INJECTED" : "NULL"));
        System.out.println("[JWT Filter] JwtUtil: " + (jwtUtil != null ? "INJECTED" : "NULL"));
    }

    /** Sends 401 JSON response so the frontend can redirect to login. */
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String json = String.format("{\"error\":\"Unauthorized\",\"message\":\"%s\",\"status\":401}",
            message.replace("\"", "\\\""));
        response.getWriter().write(json);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            String path = request.getRequestURI();
            System.out.println("========================================");
            System.out.println("[JWT Filter] ========== FILTER EXECUTING ==========");
            System.out.println("[JWT Filter] Filter called for path: " + path);
            System.out.println("[JWT Filter] Request method: " + request.getMethod());
            System.out.println("[JWT Filter] Filter class: " + this.getClass().getName());
            System.out.println("[JWT Filter] UserDetailsService: " + (userDetailsService != null ? "OK" : "NULL"));
            System.out.println("[JWT Filter] JwtUtil: " + (jwtUtil != null ? "OK" : "NULL"));
            
            final String authorizationHeader = request.getHeader("Authorization");
            System.out.println("[JWT Filter] Authorization header: " + (authorizationHeader != null ? authorizationHeader.substring(0, Math.min(30, authorizationHeader.length())) + "..." : "null"));

        // Skip filter for public endpoints
        if (path.startsWith("/api/auth/") || path.equals("/api/health")) {
            System.out.println("[JWT Filter] Skipping public endpoint: " + path);
            filterChain.doFilter(request, response);
            return;
        }

        String username = null;
        String jwt = null;

        // Extract JWT token from Authorization header
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7).trim();
            if (!jwt.isEmpty()) {
                try {
                    username = jwtUtil.extractUsername(jwt);
                    System.out.println("[JWT Filter] ✓ Extracted username: " + username);
                } catch (Exception e) {
                    System.out.println("[JWT Filter] ✗ ERROR extracting username: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("[JWT Filter] ✗ No valid Authorization header");
        }

            // Validate token and set authentication in SecurityContext
            if (username != null && jwt != null && !jwt.isEmpty()) {
                try {
                    System.out.println("[JWT Filter] Loading user details for: " + username);
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                    System.out.println("[JWT Filter] User details loaded: " + userDetails.getUsername());
                    
                    System.out.println("[JWT Filter] Validating token...");
                    boolean isValid = jwtUtil.validateToken(jwt, userDetails);
                    System.out.println("[JWT Filter] Token validation result: " + isValid + " for user: " + username);
                    
                    if (isValid) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        System.out.println("[JWT Filter] ✓✓✓ Authentication SET for user: " + username + " on path: " + path);
                        System.out.println("[JWT Filter] SecurityContext authentication: " + 
                            (SecurityContextHolder.getContext().getAuthentication() != null ? "SET" : "NULL"));
                    } else {
                        System.out.println("[JWT Filter] ✗✗✗ Token validation FAILED for user: " + username);
                        sendUnauthorized(response, "Invalid or expired token");
                        return;
                    }
                } catch (Exception e) {
                    System.out.println("[JWT Filter] ✗✗✗ ERROR setting authentication: " + e.getMessage());
                    e.printStackTrace();
                    sendUnauthorized(response, "Authentication failed: " + e.getMessage());
                    return;
                }
            }

            System.out.println("========================================");
        } catch (Exception e) {
            System.err.println("[JWT Filter] CRITICAL ERROR in filter: " + e.getMessage());
            e.printStackTrace();
        }
        
        filterChain.doFilter(request, response);
    }
}
