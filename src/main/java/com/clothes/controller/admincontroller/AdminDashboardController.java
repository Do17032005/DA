package com.clothes.controller.admincontroller;

import com.clothes.model.Order;
import com.clothes.model.Product;
import com.clothes.service.AdminDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
    public String showDashboard(HttpSession session, Model model) {
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
        List<Product> topProducts = dashboardService.getTopProducts(5);
        model.addAttribute("topProducts", topProducts);

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
}
