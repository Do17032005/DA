package com.clothes.controller.admincontroller;

import com.clothes.model.Order;
import com.clothes.service.AdminOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Admin Order Management Controller
 */
@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final AdminOrderService orderService;

    public AdminOrderController(AdminOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public String listOrders(@RequestParam(required = false) String status,
            HttpSession session,
            Model model) {
        if (session.getAttribute("adminId") == null) {
            return "redirect:/admin/login";
        }

        List<Order> orders = orderService.getOrdersByStatus(status);
        Map<String, Long> statusCounts = orderService.getOrderStatusCounts();

        model.addAttribute("orders", orders);
        model.addAttribute("pendingCount", statusCounts.get("pending"));
        model.addAttribute("processingCount", statusCounts.get("processing"));
        model.addAttribute("shippingCount", statusCounts.get("shipping"));
        model.addAttribute("completedCount", statusCounts.get("completed"));
        model.addAttribute("status", status);

        return "admin/orders";
    }

    @GetMapping("/{id}")
    public String viewOrderDetails(@PathVariable Long id,
            HttpSession session,
            Model model) {
        if (session.getAttribute("adminId") == null) {
            return "redirect:/admin/login";
        }

        Optional<Order> order = orderService.getOrderById(id);
        if (order.isEmpty()) {
            return "redirect:/admin/orders";
        }

        model.addAttribute("order", order.get());
        return "admin/order-detail";
    }

    @PostMapping("/{id}/update-status")
    @ResponseBody
    public ResponseEntity<Void> updateStatus(@PathVariable Long id,
            @RequestParam String status) {
        try {
            orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/invoice")
    public String printInvoice(@PathVariable Long id,
            HttpSession session,
            Model model) {
        if (session.getAttribute("adminId") == null) {
            return "redirect:/admin/login";
        }

        Optional<Order> order = orderService.getOrderById(id);
        if (order.isEmpty()) {
            return "redirect:/admin/orders";
        }

        model.addAttribute("order", order.get());
        return "admin/invoice";
    }

    @GetMapping("/export")
    public String exportOrders(HttpSession session) {
        if (session.getAttribute("adminId") == null) {
            return "redirect:/admin/login";
        }
        // TODO: Implement Excel export
        return "redirect:/admin/orders";
    }
}
