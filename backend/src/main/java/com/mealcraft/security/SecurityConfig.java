package com.mealcraft.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.annotation.Order;

/**
 * Spring Security Configuration
 * 
 * Configures security settings for the MealCraft application including:
 * - JWT authentication
 * - Password encoding (BCrypt)
 * - CORS configuration
 * - Public and protected endpoints
 * 
 * @author MealCraft Team
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Order(1)
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private JwtUtil jwtUtil;

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String corsAllowedOrigins;
    
    @jakarta.annotation.PostConstruct
    public void init() {
        System.out.println("[SecurityConfig] ========== SECURITY CONFIG INITIALIZED ==========");
        System.out.println("[SecurityConfig] UserDetailsService: " + (userDetailsService != null ? "INJECTED" : "NULL"));
        System.out.println("[SecurityConfig] JwtUtil: " + (jwtUtil != null ? "INJECTED" : "NULL"));
    }
    
    /**
     * Creates and configures JWT Authentication Filter
     * This bean is explicitly created here to ensure proper dependency injection
     * 
     * @return Configured JwtAuthenticationFilter
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        System.out.println("[SecurityConfig] ========== CREATING JWT FILTER BEAN ==========");
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        filter.setUserDetailsService(userDetailsService);
        filter.setJwtUtil(jwtUtil);
        filter.initialize();
        System.out.println("[SecurityConfig] ✓✓✓ JWT Filter bean created successfully");
        return filter;
    }

    /**
     * Configures security filter chain
     * 
     * @param http HttpSecurity object
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration error occurs
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        System.out.println("[SecurityConfig] ========== CONFIGURING SECURITY FILTER CHAIN ==========");
        System.out.println("[SecurityConfig] JWT Filter instance check: " + (jwtAuthenticationFilter != null ? "EXISTS" : "NULL"));
        
        // Add JWT filter FIRST - before any other configuration
        if (jwtAuthenticationFilter != null) {
            System.out.println("[SecurityConfig] Adding JWT filter to chain BEFORE authorization...");
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            System.out.println("[SecurityConfig] ✓✓✓ JWT Filter added to filter chain successfully");
        } else {
            System.out.println("[SecurityConfig] ✗✗✗ CRITICAL ERROR: JWT Filter is NULL!");
            throw new IllegalStateException("JWT Authentication Filter is null - cannot configure security");
        }
        
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/recipes/discover").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler(accessDeniedHandler())
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    String jsonResponse = "{\"error\":\"Unauthorized\",\"message\":\"Authentication required\",\"status\":401}";
                    response.getWriter().write(jsonResponse);
                })
            )
            .authenticationProvider(authenticationProvider());

        System.out.println("[SecurityConfig] ========== SECURITY FILTER CHAIN CONFIGURED ==========");
        return http.build();
    }

    /**
     * Configures CORS (Cross-Origin Resource Sharing)
     * Allows frontend to make requests to backend API
     * 
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.asList(corsAllowedOrigins.split(",\\s*"));
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Configures authentication provider
     * Uses UserDetailsService and BCrypt password encoder
     * 
     * @return AuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Configures password encoder (BCrypt)
     * 
     * @return PasswordEncoder (BCrypt)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures authentication manager
     * 
     * @param config AuthenticationConfiguration
     * @return AuthenticationManager
     * @throws Exception if configuration error occurs
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Custom access denied handler
     * Returns proper JSON error response instead of empty 403
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest request, HttpServletResponse response, 
                org.springframework.security.access.AccessDeniedException accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            String jsonResponse = "{\"error\":\"Access denied\",\"message\":\"Authentication required. Please log in.\",\"status\":403}";
            response.getWriter().write(jsonResponse);
        };
    }
}

