package com.clothes.controller.admincontroller;

import com.clothes.service.SettingsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

/**
 * Admin Settings Management Controller
 */
@Controller
@RequestMapping("/admin/settings")
public class AdminSettingsController {

    private final SettingsService settingsService;

    public AdminSettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public String showSettings(HttpSession session, Model model) {
        if (session.getAttribute("adminId") == null) {
            return "redirect:/admin/login";
        }

        Map<String, String> settings = settingsService.getAllSettingsMap();
        model.addAttribute("settings", settings);

        return "admin/settings";
    }

    @PostMapping("/save")
    public String saveSettings(@RequestParam Map<String, String> allParams,
            HttpSession session) {
        if (session.getAttribute("adminId") == null) {
            return "redirect:/admin/login";
        }

        Long adminId = (Long) session.getAttribute("adminId");

        // Remove non-setting parameters
        allParams.remove("_csrf");

        settingsService.saveAllSettings(allParams, adminId);

        return "redirect:/admin/settings?success=true";
    }
}
