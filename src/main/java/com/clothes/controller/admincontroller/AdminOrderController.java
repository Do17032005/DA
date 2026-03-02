package com.clothes.controller.admincontroller;

import com.clothes.model.Order;
import com.clothes.model.OrderItem;
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        if (session.getAttribute("adminId") == null) {
            return "redirect:/admin/login";
        }

        List<Order> orders = orderService.getOrdersPaginated(status, page, size);
        Map<String, Long> statusCounts = orderService.getOrderStatusCounts();
        int totalOrders = orderService.getTotalOrdersCount(status);
        int totalPages = (int) Math.ceil((double) totalOrders / size);

        model.addAttribute("orders", orders);
        model.addAttribute("pendingCount", statusCounts.get("pending"));
        model.addAttribute("processingCount", statusCounts.get("processing"));
        model.addAttribute("shippingCount", statusCounts.get("shipping"));
        model.addAttribute("completedCount", statusCounts.get("completed"));
        model.addAttribute("status", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        return "admin/orders";
    }

    @GetMapping("/{id}")
    public String viewOrderDetails(@PathVariable Long id,
            HttpSession session,
            Model model) {
        if (session.getAttribute("adminId") == null) {
            return "redirect:/admin/login";
        }

        Optional<Order> orderOpt = orderService.getOrderById(id);
        if (orderOpt.isEmpty()) {
            return "redirect:/admin/orders";
        }

        Order order = orderOpt.get();

        // Load order items with product details
        List<OrderItem> items = orderService.getOrderItems(id);
        order.setOrderItems(items);

        model.addAttribute("order", order);
        return "admin/order-detail";
    }

    @PostMapping("/{id}/update-status")
    @ResponseBody
    public ResponseEntity<String> updateStatus(@PathVariable Long id,
            @RequestParam String status,
            HttpSession session) {
        System.out.println("=== POST /admin/orders/" + id + "/update-status ===");
        System.out.println("Status parameter: " + status);
        System.out.println("Session adminId: " + session.getAttribute("adminId"));

        if (session.getAttribute("adminId") == null) {
            return ResponseEntity.status(401).body("Unauthorized - Admin login required");
        }

        try {
            orderService.updateOrderStatus(id, status);
            System.out.println("Status update successful");
            return ResponseEntity.ok("Order status updated successfully");
        } catch (IllegalArgumentException e) {
            System.err.println("Validation error: " + e.getMessage());
            return ResponseEntity.badRequest().body("Invalid status: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error updating order status: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
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
