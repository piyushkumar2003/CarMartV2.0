package com.airline.user.controller;

import java.util.List;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.airline.user.entity.User;
import com.airline.user.dto.FlightDTO;
import com.airline.user.service.UserService;

/**
 * ============================================
 * USER CONTROLLER
 * ============================================
 * 
 * Handles user authentication and navigation.
 * 
 * RESPONSIBILITIES:
 * - Login / Logout (integrated with Spring Security)
 * - User Registration
 * - Dashboard navigation
 * - Flight search (listing flights for booking)
 * 
 * NOTE: Booking-related endpoints are now in BookingController!
 */
@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ===========================================
    // PUBLIC PAGES (No login required)
    // ===========================================

    /**
     * Home page - landing page for the application.
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * Show registration form.
     */
    @GetMapping("/register")
    public String showRegister(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    /**
     * Process user registration.
     */
    @PostMapping("/register")
    public String register(@ModelAttribute User user, Model model) {
        try {
            userService.registerUser(user);
            return "redirect:/login";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    /**
     * Show login form.
     */
    @GetMapping("/login")
    public String showLogin(@RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid Credentials");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out.");
        }
        return "login";
    }

    /**
     * Process login.
     * Uses PasswordEncoder to verify BCrypt-hashed passwords.
     * Redirects to appropriate dashboard based on role.
     */
    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
            HttpSession session, Model model) {
        User user = userService.findByUsername(username);
        // Use passwordEncoder.matches() to compare plain password with hashed password
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            session.setAttribute("user", user);
            if ("ADMIN".equals(user.getRole())) {
                return "redirect:/admin-dashboard";
            } else {
                return "redirect:/passenger-dashboard";
            }
        }
        model.addAttribute("error", "Invalid Credentials");
        return "login";
    }

    /**
     * Logout - invalidate session.
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        SecurityContextHolder.clearContext();
        return "redirect:/";
    }

    // ===========================================
    // ADMIN PAGES
    // ===========================================

    /**
     * Admin dashboard.
     */
    @GetMapping("/admin-dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        if (user == null || !"ADMIN".equals(user.getRole()))
            return "redirect:/login";
        model.addAttribute("user", user);
        return "admin-dashboard";
    }

    /**
     * Show form to create new admin.
     */
    @GetMapping("/create-admin")
    public String showCreateAdmin(Model model, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null || !"ADMIN".equals(user.getRole()))
            return "redirect:/login";
        model.addAttribute("user", new User());
        return "register";
    }

    // ===========================================
    // PASSENGER PAGES
    // ===========================================

    /**
     * Passenger dashboard.
     */
    @GetMapping("/passenger-dashboard")
    public String passengerDashboard(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        if (user == null)
            return "redirect:/login";
        model.addAttribute("user", user);
        return "passenger-dashboard";
    }

    /**
     * Search flights form.
     */
    @GetMapping("/search-flights")
    public String searchFlightsForm(HttpSession session) {
        if (getCurrentUser(session) == null)
            return "redirect:/login";
        return "search-flights-user";
    }

    /**
     * Book flight - shows list of available flights.
     * User can search or view all flights.
     * 
     * FLOW:
     * 1. User enters search criteria (optional)
     * 2. Flights are fetched from Flight Service
     * 3. User clicks "Book" on a flight
     * 4. Redirects to /initiate-booking (in BookingController)
     */
    @GetMapping("/book-flight")
    public String searchAndBook(@RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String date,
            Model model, HttpSession session) {
        if (getCurrentUser(session) == null)
            return "redirect:/login";

        List<FlightDTO> flights = userService.searchFlights(source, destination, date);
        model.addAttribute("flights", flights);
        return "book-flight";
    }

    // ===========================================
    // HELPER METHODS
    // ===========================================

    /**
     * Get current user from session or Spring Security context.
     * Supports both session-based and Spring Security authentication.
     */
    private User getCurrentUser(HttpSession session) {
        // First try session
        User user = (User) session.getAttribute("user");
        if (user != null) {
            return user;
        }

        // Fallback to Spring Security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            user = userService.findByUsername(username);
            if (user != null) {
                // Cache in session for faster access
                session.setAttribute("user", user);
            }
            return user;
        }
        return null;
    }

    // ===========================================
    // ERROR PAGE
    // ===========================================

    /**
     * Generic error page.
     */
    @GetMapping("/error")
    public String errorPage() {
        return "error";
    }
}
