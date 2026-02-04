package com.clothes.dao;

import com.clothes.model.Order;
import com.clothes.model.OrderItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO for Order entity
 */
@Repository
public class OrderDAO {

    private final JdbcTemplate jdbcTemplate;
    private final OrderItemDAO orderItemDAO;

    public OrderDAO(JdbcTemplate jdbcTemplate, OrderItemDAO orderItemDAO) {
        this.jdbcTemplate = jdbcTemplate;
        this.orderItemDAO = orderItemDAO;
    }

    private static class OrderRowMapper implements RowMapper<Order> {
        @Override
        public Order mapRow(ResultSet rs, int rowNum) throws SQLException {
            Order order = new Order();
            order.setOrderId(rs.getLong("order_id"));
            order.setUserId(rs.getLong("user_id"));

            Timestamp orderDate = rs.getTimestamp("order_date");
            if (orderDate != null) {
                order.setOrderDate(orderDate.toLocalDateTime());
            }

            order.setTotalAmount(rs.getBigDecimal("total_amount"));
            order.setTotal(rs.getBigDecimal("total_amount")); // For Thymeleaf template compatibility
            order.setStatus(Order.OrderStatus.fromValue(rs.getString("status")));
            order.setShippingAddress(rs.getString("shipping_address"));
            order.setPaymentMethod(rs.getString("payment_method"));
            order.setNotes(rs.getString("note"));

            // New field for admin - generated from ID since not in DB yet
            order.setOrderCode("ORD-" + rs.getLong("order_id"));

            return order;
        }
    }

    public Long save(Order order) {
        String sql = "INSERT INTO orders (user_id, order_date, total_amount, status, " +
                "shipping_address, payment_method, note) " +
                "VALUES (?, NOW(), ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                order.getUserId(),
                order.getTotalAmount(),
                order.getStatus().getValue(),
                order.getShippingAddress(),
                order.getPaymentMethod(),
                order.getNotes());

        Long orderId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // Save order items if present
        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            for (OrderItem item : order.getOrderItems()) {
                item.setOrderId(orderId);
                orderItemDAO.save(item);
            }
        }

        return orderId;
    }

    public int updateStatus(Long orderId, Order.OrderStatus status) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        return jdbcTemplate.update(sql, status.getValue(), orderId);
    }

    public int update(Order order) {
        String sql = "UPDATE orders SET total_amount = ?, status = ?, " +
                "shipping_address = ?, payment_method = ?, note = ? " +
                "WHERE order_id = ?";

        return jdbcTemplate.update(sql,
                order.getTotalAmount(),
                order.getStatus().getValue(),
                order.getShippingAddress(),
                order.getPaymentMethod(),
                order.getNotes(),
                order.getOrderId());
    }

    public Optional<Order> findById(Long orderId) {
        String sql = "SELECT * FROM orders WHERE order_id = ?";
        List<Order> orders = jdbcTemplate.query(sql, new OrderRowMapper(), orderId);

        if (orders.isEmpty()) {
            return Optional.empty();
        }

        Order order = orders.get(0);
        // Load order items
        order.setOrderItems(orderItemDAO.findByOrderId(orderId));

        return Optional.of(order);
    }

    public List<Order> findByUserId(Long userId) {
        String sql = "SELECT * FROM orders WHERE user_id = ? ORDER BY order_date DESC";
        List<Order> orders = jdbcTemplate.query(sql, new OrderRowMapper(), userId);

        // Load order items for each order
        for (Order order : orders) {
            order.setOrderItems(orderItemDAO.findByOrderId(order.getOrderId()));
        }

        return orders;
    }

    public List<Order> findByStatus(Order.OrderStatus status) {
        String sql = "SELECT * FROM orders WHERE status = ? ORDER BY order_date DESC";
        return jdbcTemplate.query(sql, new OrderRowMapper(), status.getValue());
    }

    public List<Order> findAll() {
        String sql = "SELECT * FROM orders ORDER BY order_date DESC";
        return jdbcTemplate.query(sql, new OrderRowMapper());
    }

    public List<Order> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = "SELECT * FROM orders WHERE order_date BETWEEN ? AND ? ORDER BY order_date DESC";
        return jdbcTemplate.query(sql, new OrderRowMapper(),
                Timestamp.valueOf(startDate), Timestamp.valueOf(endDate));
    }

    public List<Order> findRecentOrders(int limit) {
        String sql = "SELECT * FROM orders ORDER BY order_date DESC LIMIT ?";
        return jdbcTemplate.query(sql, new OrderRowMapper(), limit);
    }

    public int countItemsByOrderId(Long orderId) {
        String sql = "SELECT COUNT(*) FROM order_items WHERE order_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, orderId);
        return count != null ? count : 0;
    }

    public int delete(Long orderId) {
        // First delete order items
        orderItemDAO.deleteByOrderId(orderId);
        // Then delete order
        String sql = "DELETE FROM orders WHERE order_id = ?";
        return jdbcTemplate.update(sql, orderId);
    }

    public int countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM orders WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null ? count : 0;
    }

    /**
     * Count by status string
     */
    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM orders WHERE status = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, status);
        return count != null ? count : 0;
    }

    /**
     * Get total count
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM orders";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Get monthly revenue
     */
    public java.math.BigDecimal getMonthlyRevenue(int year, int month) {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM orders " +
                "WHERE YEAR(order_date) = ? AND MONTH(order_date) = ? AND status = 'COMPLETED'";
        return jdbcTemplate.queryForObject(sql, java.math.BigDecimal.class, year, month);
    }

    /**
     * Get monthly revenue stats for a year
     */
    public List<java.util.Map<String, Object>> getMonthlyRevenueStats(int year) {
        String sql = "SELECT MONTH(order_date) as month, " +
                "COALESCE(SUM(total_amount), 0) as revenue, " +
                "COUNT(*) as order_count " +
                "FROM orders " +
                "WHERE YEAR(order_date) = ? AND status = 'COMPLETED' " +
                "GROUP BY MONTH(order_date) " +
                "ORDER BY month";
        return jdbcTemplate.queryForList(sql, year);
    }

    /**
     * Get daily revenue stats for a month
     */
    public List<java.util.Map<String, Object>> getDailyRevenueStats(int year, int month) {
        String sql = "SELECT DAY(order_date) as day, " +
                "COALESCE(SUM(total_amount), 0) as revenue, " +
                "COUNT(*) as order_count " +
                "FROM orders " +
                "WHERE YEAR(order_date) = ? AND MONTH(order_date) = ? AND status = 'COMPLETED' " +
                "GROUP BY DAY(order_date) " +
                "ORDER BY day";
        return jdbcTemplate.queryForList(sql, year, month);
    }

    /**
     * Get yearly revenue stats
     */
    public List<java.util.Map<String, Object>> getYearlyRevenueStats() {
        String sql = "SELECT YEAR(order_date) as year, " +
                "COALESCE(SUM(total_amount), 0) as revenue, " +
                "COUNT(*) as order_count " +
                "FROM orders " +
                "WHERE status = 'COMPLETED' " +
                "GROUP BY YEAR(order_date) " +
                "ORDER BY year DESC";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Add status history
     */
    public void addStatusHistory(Long orderId, Order.OrderStatus status, String notes) {
        String sql = "INSERT INTO order_status_history (order_id, status, notes, created_at) " +
                "VALUES (?, ?, ?, NOW())";
        jdbcTemplate.update(sql, orderId, status.getValue(), notes);
    }

    /**
     * Update tracking number
     */
    public int updateTrackingNumber(Long orderId, String trackingNumber) {
        String sql = "UPDATE orders SET tracking_number = ?, updated_at = NOW() WHERE order_id = ?";
        return jdbcTemplate.update(sql, trackingNumber, orderId);
    }

    /**
     * Find order by order code
     */
    public Optional<Order> findByOrderCode(String orderCode) {
        String sql = "SELECT * FROM orders WHERE order_code = ?";
        List<Order> orders = jdbcTemplate.query(sql, new OrderRowMapper(), orderCode);

        if (orders.isEmpty()) {
            return Optional.empty();
        }

        Order order = orders.get(0);
        order.setOrderItems(orderItemDAO.findByOrderId(order.getOrderId()));
        return Optional.of(order);
    }

    /**
     * Count orders by status
     */
    public int countByStatus(Order.OrderStatus status) {
        String sql = "SELECT COUNT(*) FROM orders WHERE status = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, status.getValue());
        return count != null ? count : 0;
    }

    /**
     * Get orders with customer details using view
     */
    public List<java.util.Map<String, Object>> findAllWithCustomerDetails() {
        String sql = "SELECT * FROM v_orders_with_customer ORDER BY order_date DESC";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Get orders by status with customer details
     */
    public List<java.util.Map<String, Object>> findByStatusWithCustomerDetails(Order.OrderStatus status) {
        String sql = "SELECT * FROM v_orders_with_customer WHERE status = ? ORDER BY order_date DESC";
        return jdbcTemplate.queryForList(sql, status.getValue());
    }
}
