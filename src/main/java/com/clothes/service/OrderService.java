package com.clothes.service;

import com.clothes.dao.OrderDAO;
import com.clothes.dao.OrderItemDAO;
import com.clothes.model.Cart;
import com.clothes.model.CartItem;
import com.clothes.model.Order;
import com.clothes.model.OrderItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for Order management
 */
@Service
public class OrderService {

    private final OrderDAO orderDAO;
    private final OrderItemDAO orderItemDAO;

    public OrderService(OrderDAO orderDAO, OrderItemDAO orderItemDAO) {
        this.orderDAO = orderDAO;
        this.orderItemDAO = orderItemDAO;
    }

    /**
     * Create order from cart
     */
    public Long createOrder(Long userId, Cart cart, String shippingAddress,
            String paymentMethod, String notes) {
        if (cart == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(cart.getTotalAmount());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setShippingAddress(shippingAddress);
        order.setPaymentMethod(paymentMethod);
        order.setNotes(notes);

        // Convert cart items to order items
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getPrice());
            orderItems.add(orderItem);
        }
        order.setOrderItems(orderItems);

        return orderDAO.save(order);
    }

    /**
     * Get order by ID
     */
    public Optional<Order> getOrderById(Long orderId) {
        return orderDAO.findById(orderId);
    }

    /**
     * Get orders by user ID
     */
    public List<Order> getOrdersByUserId(Long userId) {
        return orderDAO.findByUserId(userId);
    }

    /**
     * Get orders by status
     */
    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderDAO.findByStatus(status);
    }

    /**
     * Get all orders
     */
    public List<Order> getAllOrders() {
        return orderDAO.findAll();
    }

    /**
     * Get recent orders
     */
    public List<Order> getRecentOrders(int limit) {
        return orderDAO.findRecentOrders(limit);
    }

    /**
     * Update order status
     */
    public boolean updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Optional<Order> orderOpt = orderDAO.findById(orderId);
        if (orderOpt.isEmpty()) {
            return false;
        }

        return orderDAO.updateStatus(orderId, status) > 0;
    }

    /**
     * Cancel order
     */
    public boolean cancelOrder(Long orderId, Long userId) {
        Optional<Order> orderOpt = orderDAO.findById(orderId);
        if (orderOpt.isEmpty()) {
            return false;
        }

        Order order = orderOpt.get();

        // Check if user owns this order
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền hủy đơn hàng này");
        }

        // Only allow cancellation if order is PENDING
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalArgumentException("Không thể hủy đơn hàng đã được xử lý");
        }

        return orderDAO.updateStatus(orderId, Order.OrderStatus.CANCELLED) > 0;
    }

    /**
     * Calculate total revenue
     */
    public BigDecimal calculateTotalRevenue() {
        List<Order> completedOrders = orderDAO.findByStatus(Order.OrderStatus.COMPLETED);
        return completedOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get order count by user
     */
    public int getOrderCountByUser(Long userId) {
        return orderDAO.countByUserId(userId);
    }

    /**
     * Get order count by status
     */
    public int getOrderCountByStatus(Order.OrderStatus status) {
        return orderDAO.countByStatus(status);
    }
}
