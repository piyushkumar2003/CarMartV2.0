package com.airline.user.controller;

import com.airline.user.dto.AuthRequest;
import com.airline.user.dto.AuthResponse;
import com.airline.user.dto.RegisterRequest;
import com.airline.user.entity.User;
import com.airline.user.repository.UserRepository;
import com.airline.user.security.CustomUserDetailsService;
import com.airline.user.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller for JWT-based REST API.
 * 
 * Provides endpoints for:
 * - POST /api/auth/login - Authenticate and get JWT token
 * - POST /api/auth/register - Register new user and get JWT token
 * - GET /api/auth/validate - Validate current token
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticate user and return JWT token.
     * 
     * @param request AuthRequest containing username and password
     * @return AuthResponse with JWT token on success
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        try {
            // Authenticate credentials
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()));

            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            User user = userDetailsService.loadUserEntity(request.getUsername());

            // Generate JWT token
            String token = jwtService.generateToken(userDetails);

            return ResponseEntity.ok(AuthResponse.success(
                    token,
                    user.getUsername(),
                    user.getRole(),
                    jwtService.getExpirationTime()));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Invalid username or password"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.error("Authentication failed: " + e.getMessage()));
        }
    }

    /**
     * Register new user and return JWT token.
     * 
     * @param request RegisterRequest containing user details
     * @return AuthResponse with JWT token on success
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            // Check if username already exists
            if (userRepository.findByUsername(request.getUsername()) != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(AuthResponse.error("Username already exists"));
            }

            // Create new user with encoded password
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(request.getRole() != null ? request.getRole() : "PASSENGER");

            // Save user
            userRepository.save(user);

            // Load user details and generate token
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            String token = jwtService.generateToken(userDetails);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(AuthResponse.success(
                            token,
                            user.getUsername(),
                            user.getRole(),
                            jwtService.getExpirationTime()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.error("Registration failed: " + e.getMessage()));
        }
    }

    /**
     * Validate current token and return user info.
     * This endpoint is protected - only accessible with valid JWT.
     * 
     * @return User information if token is valid
     */
    @GetMapping("/validate")
    public ResponseEntity<AuthResponse> validateToken() {
        // If we reach this point, the token is valid (JWT filter validated it)
        try {
            // Get current authenticated user from security context
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated()) {
                String username = auth.getName();
                User user = userDetailsService.loadUserEntity(username);

                AuthResponse response = new AuthResponse();
                response.setUsername(user.getUsername());
                response.setRole(user.getRole());
                response.setMessage("Token is valid");

                return ResponseEntity.ok(response);
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Not authenticated"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Token validation failed"));
        }
    }
}
