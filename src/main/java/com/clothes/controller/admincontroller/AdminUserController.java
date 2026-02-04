package com.clothes.controller.admincontroller;

import com.clothes.model.User;
import com.clothes.service.AdminCustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Admin Customer Management Controller
 */
@Controller
@RequestMapping("/admin/customers")
public class AdminUserController {

    private final AdminCustomerService customerService;

    public AdminUserController(AdminCustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public String listCustomers(HttpSession session, Model model) {
        if (session.getAttribute("adminId") == null) {
            return "redirect:/admin/login";
        }

        List<User> customers = customerService.getAllCustomers();
        Map<String, Integer> stats = customerService.getCustomerStats();

        model.addAttribute("customers", customers);
        model.addAttribute("totalCustomers", stats.get("total"));
        model.addAttribute("activeCustomers", stats.get("active"));
        model.addAttribute("newCustomers", stats.get("newThisMonth"));
        model.addAttribute("blockedCustomers", stats.get("blocked"));

        return "admin/customers";
    }

    @GetMapping("/{id}")
    public String viewCustomerDetails(@PathVariable Long id,
            HttpSession session,
            Model model) {
        if (session.getAttribute("adminId") == null) {
            return "redirect:/admin/login";
        }

        Optional<User> customer = customerService.getCustomerById(id);
        if (customer.isEmpty()) {
            return "redirect:/admin/customers";
        }

        model.addAttribute("customer", customer.get());
        return "admin/customer-detail";
    }

    @PostMapping("/{id}/toggle-status")
    @ResponseBody
    public ResponseEntity<Void> toggleStatus(@PathVariable Long id) {
        try {
            customerService.toggleCustomerStatus(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export")
    public String exportCustomers(HttpSession session) {
        if (session.getAttribute("adminId") == null) {
            return "redirect:/admin/login";
        }
        // TODO: Implement Excel export
        return "redirect:/admin/customers";
    }
}
