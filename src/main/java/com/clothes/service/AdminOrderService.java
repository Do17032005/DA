package com.clothes.service;

import com.clothes.dao.OrderDAO;
import com.clothes.dao.UserDAO;
import com.clothes.model.Order;
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

    public AdminOrderService(OrderDAO orderDAO, UserDAO userDAO) {
        this.orderDAO = orderDAO;
        this.userDAO = userDAO;
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
        Optional<Order> order = orderDAO.findById(orderId);
        order.ifPresent(o -> {
            o.setStatus(Order.OrderStatus.fromValue(status.toUpperCase()));
            orderDAO.update(o);
        });
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
        });
    }
}
