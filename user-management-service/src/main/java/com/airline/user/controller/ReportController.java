package com.airline.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.airline.user.service.ReportService;

@Controller
@RequestMapping("/admin")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/reports")
    public String showReports(Model model) {
        model.addAttribute("dailyStats", reportService.getDailyReport());
        model.addAttribute("destStats", reportService.getDestinationReport());
        model.addAttribute("occupancyStats", reportService.getOccupancyReport());

        // Mock user for layout if needed
        model.addAttribute("username", "Admin");

        return "admin-reports";
    }
}
