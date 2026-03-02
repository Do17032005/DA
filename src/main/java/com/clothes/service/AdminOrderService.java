package com.clothes.service;

import com.clothes.dao.OrderDAO;
import com.clothes.dao.UserDAO;
import com.clothes.model.Order;
import com.clothes.model.OrderItem;
import com.clothes.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for admin order management
 */
@Service
@Transactional
public class AdminOrderService {

    private final OrderDAO orderDAO;
    private final UserDAO userDAO;
    private final OrderService orderService;

    public AdminOrderService(OrderDAO orderDAO, UserDAO userDAO, OrderService orderService) {
        this.orderDAO = orderDAO;
        this.userDAO = userDAO;
        this.orderService = orderService;
    }

    public List<Order> getAllOrders() {
        List<Order> orders = orderDAO.findAll();
        enrichOrdersWithUserData(orders);
        return orders;
    }

    public List<Order> getOrdersByStatus(String status) {
        if (status == null || status.isEmpty() || status.equals("all")) {
            return getAllOrders();
        }

        Order.OrderStatus orderStatus = Order.OrderStatus.fromValue(status.toUpperCase());
        List<Order> orders = orderDAO.findAll().stream()
                .filter(o -> o.getStatus() == orderStatus)
                .collect(Collectors.toList());
        enrichOrdersWithUserData(orders);
        return orders;
    }

    public List<Order> getOrdersPaginated(String status, int page, int size) {
        List<Order> orders;
        if (status == null || status.isEmpty() || status.equals("all")) {
            orders = orderDAO.findAllPaginated(page, size);
        } else {
            Order.OrderStatus orderStatus = Order.OrderStatus.fromValue(status.toUpperCase());
            orders = orderDAO.findByStatusPaginated(orderStatus, page, size);
        }
        enrichOrdersWithUserData(orders);
        return orders;
    }

    public int getTotalOrdersCount(String status) {
        if (status == null || status.isEmpty() || status.equals("all")) {
            return orderDAO.count();
        } else {
            Order.OrderStatus orderStatus = Order.OrderStatus.fromValue(status.toUpperCase());
            return orderDAO.countByStatus(orderStatus);
        }
    }

    public Optional<Order> getOrderById(Long id) {
        Optional<Order> order = orderDAO.findById(id);
        order.ifPresent(o -> {
            if (o.getUserId() != null) {
                userDAO.findById(o.getUserId()).ifPresent(user -> {
                    o.setUser(user);
                    o.setCustomerName(user.getFullName());
                    o.setPhone(user.getPhone());
                });
            }
        });
        return order;
    }

    public void updateOrderStatus(Long orderId, String status) {
        System.out.println("=== AdminOrderService.updateOrderStatus ===");
        System.out.println("Order ID: " + orderId);
        System.out.println("New Status (raw): " + status);

        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }

        Order.OrderStatus newStatus = Order.OrderStatus.fromValue(status.toUpperCase());
        System.out.println("Parsed OrderStatus enum: " + newStatus);

        int rowsUpdated = orderDAO.updateStatus(orderId, newStatus);
        System.out.println("Rows updated: " + rowsUpdated);

        if (rowsUpdated == 0) {
            throw new RuntimeException(
                    "Failed to update order status - no rows affected. Order ID may not exist: " + orderId);
        }

        System.out.println("Order status updated successfully");
    }

    public Map<String, Long> getOrderStatusCounts() {
        List<Order> orders = orderDAO.findAll();
        return Map.of(
                "pending", orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.PENDING).count(),
                "processing", orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.PROCESSING).count(),
                "shipping", orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.SHIPPING).count(),
                "completed", orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.COMPLETED).count());
    }

    private void enrichOrdersWithUserData(List<Order> orders) {
        orders.forEach(order -> {
            if (order.getUserId() != null) {
                userDAO.findById(order.getUserId()).ifPresent(user -> {
                    order.setUser(user);
                    order.setCustomerName(user.getFullName());
                    order.setPhone(user.getPhone());
                });
            }
            // Set order code if not exists
            if (order.getOrderCode() == null) {
                order.setOrderCode("ORD" + String.format("%06d", order.getOrderId()));
            }
            // Set createdAt from orderDate if not exists
            if (order.getCreatedAt() == null && order.getOrderDate() != null) {
                order.setCreatedAt(order.getOrderDate());
            }
            // Set total from totalAmount if not exists
            if (order.getTotal() == null && order.getTotalAmount() != null) {
                order.setTotal(order.getTotalAmount());
            }

            // Populate Item Count
            try {
                // Assuming countItemsByOrderId exists in OrderDAO or we implement it
                int count = orderDAO.countItemsByOrderId(order.getOrderId());
                order.setItemCount(count);
            } catch (Exception e) {
                order.setItemCount(0);
            }
        });
    }

    /**
     * Get order items with product details
     */
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderService.getOrderItems(orderId);
    }
}
