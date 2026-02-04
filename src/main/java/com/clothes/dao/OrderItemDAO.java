package com.clothes.dao;

import com.clothes.model.OrderItem;
import com.clothes.model.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * DAO for OrderItem entity
 */
@Repository
public class OrderItemDAO {

    private final JdbcTemplate jdbcTemplate;

    public OrderItemDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static class OrderItemRowMapper implements RowMapper<OrderItem> {
        @Override
        public OrderItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            OrderItem item = new OrderItem();
            item.setOrderItemId(rs.getLong("order_item_id"));
            item.setOrderId(rs.getLong("order_id"));
            item.setProductId(rs.getLong("product_id"));
            item.setQuantity(rs.getInt("quantity"));
            item.setUnitPrice(rs.getBigDecimal("price"));
            if (item.getUnitPrice() != null) {
                item.setSubtotal(item.getUnitPrice().multiply(new java.math.BigDecimal(item.getQuantity())));
            }
            item.setSize(rs.getString("size"));
            item.setColor(rs.getString("color"));

            // Populate product details if present (from JOIN)
            try {
                String productName = rs.getString("product_name");
                if (productName != null) {
                    Product p = new Product();
                    p.setProductId(item.getProductId());
                    p.setProductName(productName);
                    p.setImageUrl(rs.getString("product_image"));
                    item.setProduct(p);
                }
            } catch (SQLException e) {
                // Product columns not in result set
            }
            return item;
        }
    }

    public Long save(OrderItem item) {
        String sql = "INSERT INTO order_items (order_id, product_id, quantity, price, size, color) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                item.getOrderId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSize(),
                item.getColor());

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public Optional<OrderItem> findById(Long orderItemId) {
        String sql = "SELECT * FROM order_items WHERE order_item_id = ?";
        List<OrderItem> items = jdbcTemplate.query(sql, new OrderItemRowMapper(), orderItemId);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    public List<OrderItem> findByOrderId(Long orderId) {
        String sql = "SELECT oi.*, p.product_name, p.image_url as product_image " +
                "FROM order_items oi " +
                "LEFT JOIN products p ON oi.product_id = p.product_id " +
                "WHERE oi.order_id = ?";
        return jdbcTemplate.query(sql, new OrderItemRowMapper(), orderId);
    }

    public List<OrderItem> findByProductId(Long productId) {
        String sql = "SELECT * FROM order_items WHERE product_id = ?";
        return jdbcTemplate.query(sql, new OrderItemRowMapper(), productId);
    }

    public int deleteByOrderId(Long orderId) {
        String sql = "DELETE FROM order_items WHERE order_id = ?";
        return jdbcTemplate.update(sql, orderId);
    }

    public int delete(Long orderItemId) {
        String sql = "DELETE FROM order_items WHERE order_item_id = ?";
        return jdbcTemplate.update(sql, orderItemId);
    }
}
