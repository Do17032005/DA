package com.clothes.dao;

import com.clothes.model.Cart;
import com.clothes.model.CartItem;
import com.clothes.model.Product;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class CartDAO {

    private final JdbcTemplate jdbcTemplate;

    public CartDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Find cart by User ID
     */
    public Optional<Cart> findByUserId(Long userId) {
        try {
            String sql = "SELECT * FROM shopping_carts WHERE user_id = ?";
            Cart cart = jdbcTemplate.queryForObject(sql, new CartRowMapper(), userId);
            if (cart != null) {
                cart.setItems(findItemsByCartId(cart.getCartId()));
            }
            return Optional.ofNullable(cart);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Find cart by Session Token
     */
    public Optional<Cart> findBySessionToken(String sessionToken) {
        try {
            String sql = "SELECT * FROM shopping_carts WHERE session_token = ?";
            Cart cart = jdbcTemplate.queryForObject(sql, new CartRowMapper(), sessionToken);
            if (cart != null) {
                cart.setItems(findItemsByCartId(cart.getCartId()));
            }
            return Optional.ofNullable(cart);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Create new cart
     */
    public Cart createCart(Long userId, String sessionToken) {
        String sql = "INSERT INTO shopping_carts (user_id, session_token) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if (userId != null) {
                ps.setLong(1, userId);
            } else {
                ps.setNull(1, java.sql.Types.BIGINT);
            }
            ps.setString(2, sessionToken);
            return ps;
        }, keyHolder);

        long cartId = keyHolder.getKey().longValue();
        Cart cart = new Cart();
        cart.setCartId(cartId);
        cart.setUserId(userId);
        return cart;
    }

    /**
     * Delete existing item and add new item to cart
     */
    public void addItem(Long cartId, CartItem item) {
        // Check if item exists with same Product ID AND Size AND Color
        String checkSql = "SELECT cart_item_id, quantity FROM cart_items " +
                "WHERE cart_id = ? AND product_id = ? " +
                "AND (size = ? OR (size IS NULL AND ? IS NULL)) " +
                "AND (color = ? OR (color IS NULL AND ? IS NULL))";
        try {
            CartItem existing = jdbcTemplate.queryForObject(checkSql,
                    (rs, rowNum) -> {
                        CartItem i = new CartItem();
                        i.setCartItemId(rs.getLong("cart_item_id"));
                        i.setQuantity(rs.getInt("quantity"));
                        return i;
                    },
                    cartId, item.getProductId(),
                    item.getSize(), item.getSize(),
                    item.getColor(), item.getColor());

            if (existing != null) {
                // Update quantity
                updateItemQuantity(existing.getCartItemId(), existing.getQuantity() + item.getQuantity());
            }
        } catch (EmptyResultDataAccessException e) {
            // Insert new
            String sql = "INSERT INTO cart_items (cart_id, product_id, quantity, size, color) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, cartId, item.getProductId(), item.getQuantity(), item.getSize(), item.getColor());
        }
    }

    /**
     * Update item quantity
     */
    public void updateItemQuantity(Long cartItemId, int quantity) {
        String sql = "UPDATE cart_items SET quantity = ? WHERE cart_item_id = ?";
        jdbcTemplate.update(sql, quantity, cartItemId);
    }

    /**
     * Remove item
     */
    public void removeItem(Long cartItemId) {
        String sql = "DELETE FROM cart_items WHERE cart_item_id = ?";
        jdbcTemplate.update(sql, cartItemId);
    }

    /**
     * Remove item by product id and cart id
     */
    public void removeItemByProduct(Long cartId, Long productId) {
        String sql = "DELETE FROM cart_items WHERE cart_id = ? AND product_id = ?";
        jdbcTemplate.update(sql, cartId, productId);
    }

    /**
     * Clear cart
     */
    public void clearCart(Long cartId) {
        String sql = "DELETE FROM cart_items WHERE cart_id = ?";
        jdbcTemplate.update(sql, cartId);
    }

    /**
     * Find items for a cart
     */
    private List<CartItem> findItemsByCartId(Long cartId) {
        String sql = "SELECT ci.*, p.product_name, p.price, p.discount_price, p.image_url, p.stock_quantity " +
                "FROM cart_items ci " +
                "JOIN products p ON ci.product_id = p.product_id " +
                "WHERE ci.cart_id = ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            CartItem item = new CartItem();
            item.setCartItemId(rs.getLong("cart_item_id"));
            item.setProductId(rs.getLong("product_id"));
            item.setQuantity(rs.getInt("quantity"));
            item.setSize(rs.getString("size"));
            item.setColor(rs.getString("color"));

            Product p = new Product();
            p.setProductId(rs.getLong("product_id"));
            p.setProductName(rs.getString("product_name"));
            p.setPrice(rs.getBigDecimal("price"));
            p.setDiscountPrice(rs.getBigDecimal("discount_price"));
            p.setImageUrl(rs.getString("image_url"));
            p.setStockQuantity(rs.getInt("stock_quantity"));

            item.setProduct(p);

            // Calculate price based on discount
            java.math.BigDecimal finalPrice = p.getDiscountPrice() != null ? p.getDiscountPrice() : p.getPrice();
            item.setSubtotal(finalPrice.multiply(new java.math.BigDecimal(item.getQuantity())));

            return item;
        }, cartId);
    }

    /**
     * Update user ID for session cart (when logging in)
     */
    public void updateCartUserId(Long cartId, Long userId) {
        String sql = "UPDATE shopping_carts SET user_id = ?, session_token = NULL WHERE cart_id = ?";
        jdbcTemplate.update(sql, userId, cartId);
    }

    // Row Mappers
    private static class CartRowMapper implements RowMapper<Cart> {
        @Override
        public Cart mapRow(ResultSet rs, int rowNum) throws SQLException {
            Cart cart = new Cart();
            cart.setCartId(rs.getLong("cart_id"));
            long userId = rs.getLong("user_id");
            if (!rs.wasNull()) {
                cart.setUserId(userId);
            }
            return cart;
        }
    }
}
