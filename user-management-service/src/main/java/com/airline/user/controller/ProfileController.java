package com.airline.user.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.airline.user.entity.User;
import com.airline.user.entity.Passenger;
import com.airline.user.entity.FrequentTraveler;
import com.airline.user.service.PassengerService;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller for passenger profile management.
 * Handles profile viewing, editing, and frequent traveler management.
 */
@Controller
public class ProfileController {

    @Autowired
    private PassengerService passengerService;

    // ===========================================
    // PROFILE PAGES
    // ===========================================

    /**
     * GET /profile
     * View the current user's profile.
     */
    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Passenger passenger = passengerService.getOrCreatePassenger(user.getId());
        model.addAttribute("passenger", passenger);
        model.addAttribute("username", user.getUsername());

        return "profile";
    }

    /**
     * GET /edit-profile
     * Show profile edit form.
     */
    @GetMapping("/edit-profile")
    public String editProfile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Passenger passenger = passengerService.getOrCreatePassenger(user.getId());
        model.addAttribute("passenger", passenger);

        return "edit-profile";
    }

    /**
     * POST /update-profile
     * Save profile changes.
     */
    @PostMapping("/update-profile")
    public String updateProfile(
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String dateOfBirth,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String address,
            HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Passenger passenger = passengerService.getOrCreatePassenger(user.getId());

        // Update fields
        if (fullName != null && !fullName.trim().isEmpty()) {
            passenger.setFullName(fullName.trim());
        }
        if (phone != null && !phone.trim().isEmpty()) {
            if (!phone.matches("\\d{10}")) {
                return "redirect:/edit-profile?error=Phone number must be exactly 10 digits";
            }
            passenger.setPhone(phone.trim());
        }
        if (email != null && !email.trim().isEmpty()) {
            passenger.setEmail(email.trim());
        }
        if (dateOfBirth != null && !dateOfBirth.trim().isEmpty()) {
            try {
                LocalDate dob = LocalDate.parse(dateOfBirth);
                if (dob.isAfter(LocalDate.now())) {
                    return "redirect:/edit-profile?error=Date of Birth cannot be in the future";
                }
                passenger.setDateOfBirth(dob);
            } catch (Exception e) {
                // Invalid date format, skip
            }
        }
        if (gender != null && !gender.trim().isEmpty()) {
            passenger.setGender(gender.trim());
        }
        if (address != null && !address.trim().isEmpty()) {
            passenger.setAddress(address.trim());
        }

        passengerService.updateProfile(passenger);

        return "redirect:/profile?updated=true";
    }

    // ===========================================
    // FREQUENT TRAVELERS
    // ===========================================

    /**
     * GET /frequent-travelers
     * View list of saved travelers.
     */
    @GetMapping("/frequent-travelers")
    public String viewFrequentTravelers(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<FrequentTraveler> travelers = passengerService.getFrequentTravelers(user.getId());
        model.addAttribute("travelers", travelers);

        return "travelers";
    }

    /**
     * POST /add-traveler
     * Add a new frequent traveler.
     */
    @PostMapping("/add-traveler")
    public String addTraveler(
            @RequestParam String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam String relationship,
            HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // Validate required fields
        if (fullName == null || fullName.trim().isEmpty()) {
            return "redirect:/frequent-travelers?error=Name is required";
        }

        FrequentTraveler traveler = new FrequentTraveler();
        traveler.setUserId(user.getId());
        traveler.setFullName(fullName.trim());
        traveler.setPhone(phone != null ? phone.trim() : null);
        traveler.setEmail(email != null ? email.trim() : null);
        traveler.setRelationship(relationship);

        passengerService.addFrequentTraveler(traveler);

        return "redirect:/frequent-travelers?added=true";
    }

    /**
     * POST /delete-traveler
     * Remove a frequent traveler.
     */
    @PostMapping("/delete-traveler")
    public String deleteTraveler(
            @RequestParam Long travelerId,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        passengerService.deleteFrequentTraveler(travelerId, user.getId());

        return "redirect:/frequent-travelers?deleted=true";
    }
}
