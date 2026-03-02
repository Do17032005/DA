package com.clothes.controller.admincontroller;

import com.clothes.model.Order;
import com.clothes.model.Product;
import com.clothes.service.AdminDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for admin dashboard
 */
@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({ "/", "/dashboard" })
    public String showDashboard(HttpSession session, Model model, @RequestParam(required = false) String period) {
        // Check admin authentication
        if (session.getAttribute("adminId") == null) {
            return "redirect:/admin/login";
        }

        // Get dashboard statistics
        Map<String, Object> stats = dashboardService.getDashboardStats();
        model.addAttribute("totalRevenue", stats.get("totalRevenue"));
        model.addAttribute("totalOrders", stats.get("totalOrders"));
        model.addAttribute("totalUsers", stats.get("totalUsers"));
        model.addAttribute("totalProducts", stats.get("totalProducts"));

        // Get recent orders
        List<Order> recentOrders = dashboardService.getRecentOrders(10);
        model.addAttribute("recentOrders", recentOrders);

        // Get top products
        List<Product> topProducts = dashboardService.getTopProducts(20);
        model.addAttribute("topProducts", topProducts);

        // Get revenue by category for category chart
        List<Map<String, Object>> categoryRevenue = dashboardService.getRevenueByCategory();
        model.addAttribute("categoryRevenue", categoryRevenue);

        // Get low stock products
        List<Product> lowStockProducts = dashboardService.getLowStockProducts(10);
        model.addAttribute("lowStockProducts", lowStockProducts);

        // Get order status counts
        Map<String, Long> orderCounts = dashboardService.getOrderStatusCounts();
        model.addAttribute("pendingCount", orderCounts.get("pending"));
        model.addAttribute("processingCount", orderCounts.get("processing"));
        model.addAttribute("shippingCount", orderCounts.get("shipping"));
        model.addAttribute("completedCount", orderCounts.get("completed"));

        // Get monthly revenue data for chart
        List<BigDecimal> monthlyRevenue = dashboardService.getMonthlyRevenueData();
        model.addAttribute("monthlyRevenue", monthlyRevenue);

        return "admin/dashboard";
    }

    /**
     * API endpoint for fetching period-specific revenue data
     */
    @GetMapping("/api/dashboard/revenue")
    @ResponseBody
    public Map<String, Object> getRevenueByPeriod(@RequestParam(required = false) String period) {
        Map<String, Object> response = new HashMap<>();
        List<BigDecimal> revenueData;

        if (period == null || period.isEmpty() || period.equals("month")) {
            revenueData = dashboardService.getMonthlyRevenueData();
        } else if (period.equals("today")) {
            revenueData = dashboardService.getTodayRevenueData();
        } else if (period.equals("week")) {
            revenueData = dashboardService.getWeeklyRevenueData();
        } else if (period.equals("year")) {
            revenueData = dashboardService.getYearlyRevenueData();
        } else {
            revenueData = dashboardService.getMonthlyRevenueData();
        }

        response.put("monthlyRevenue", revenueData);
        response.put("period", period);
        return response;
    }

    /**
     * API endpoint for fetching revenue by category (for testing/debugging)
     */
    @GetMapping("/api/dashboard/category-revenue")
    @ResponseBody
    public List<Map<String, Object>> getCategoryRevenue() {
        return dashboardService.getRevenueByCategory();
    }
}
