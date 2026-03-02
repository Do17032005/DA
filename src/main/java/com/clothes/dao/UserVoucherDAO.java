package com.clothes.dao;

import com.clothes.model.UserVoucher;
import com.clothes.model.Voucher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import jakarta.annotation.PostConstruct;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class UserVoucherDAO {

    private final JdbcTemplate jdbcTemplate;

    public UserVoucherDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        // Create user_vouchers table
        String createTableSql = "CREATE TABLE IF NOT EXISTS user_vouchers (" +
                "  user_voucher_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id BIGINT NOT NULL," +
                "  voucher_id BIGINT NOT NULL," +
                "  collected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  is_used BOOLEAN DEFAULT FALSE," +
                "  used_at TIMESTAMP NULL," +
                "  UNIQUE KEY unique_user_voucher (user_id, voucher_id)," +
                "  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE," +
                "  FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
        jdbcTemplate.execute(createTableSql);

        // Ensure vouchers table has new columns (gracefully)
        try {
            // Check if voucher_code exists, if not rename code to voucher_code
            jdbcTemplate.execute("ALTER TABLE vouchers CHANGE COLUMN code voucher_code VARCHAR(50)");
        } catch (Exception e) {
            /* Column might already be renamed */ }

        try {
            jdbcTemplate.execute("ALTER TABLE vouchers ADD COLUMN voucher_name VARCHAR(100) AFTER voucher_code");
        } catch (Exception e) {
            /* Column might already exist */ }

        try {
            jdbcTemplate.execute(
                    "ALTER TABLE vouchers ADD COLUMN min_order_value DECIMAL(10,2) DEFAULT 0.00 AFTER discount_value");
        } catch (Exception e) {
            /* Column might already exist */ }

        try {
            jdbcTemplate.execute(
                    "ALTER TABLE vouchers ADD COLUMN max_discount DECIMAL(10,2) DEFAULT NULL AFTER min_order_value");
        } catch (Exception e) {
            /* Column might already exist */ }

        try {
            jdbcTemplate.execute("ALTER TABLE vouchers ADD COLUMN quantity INT DEFAULT NULL AFTER max_discount");
        } catch (Exception e) {
            /* Column might already exist */ }

        try {
            jdbcTemplate.execute("ALTER TABLE vouchers ADD COLUMN used_count INT DEFAULT 0 AFTER quantity");
        } catch (Exception e) {
            /* Column might already exist */ }

        try {
            jdbcTemplate.execute("ALTER TABLE vouchers ADD COLUMN start_date DATE NULL AFTER used_count");
        } catch (Exception e) {
            /* Column might already exist */ }

        try {
            jdbcTemplate.execute("ALTER TABLE vouchers ADD COLUMN end_date DATE NULL AFTER start_date");
        } catch (Exception e) {
            /* Column might already exist */ }
    }

    private static class UserVoucherRowMapper implements RowMapper<UserVoucher> {
        @Override
        public UserVoucher mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserVoucher uv = new UserVoucher();
            uv.setUserVoucherId(rs.getLong("user_voucher_id"));
            uv.setUserId(rs.getLong("user_id"));
            uv.setVoucherId(rs.getLong("voucher_id"));

            Timestamp collected = rs.getTimestamp("collected_at");
            if (collected != null) {
                uv.setCollectedAt(collected.toLocalDateTime());
            }

            uv.setIsUsed(rs.getBoolean("is_used"));

            Timestamp used = rs.getTimestamp("used_at");
            if (used != null) {
                uv.setUsedAt(used.toLocalDateTime());
            }

            // Map voucher data if available in join
            try {
                Voucher v = new Voucher();
                v.setVoucherId(rs.getLong("voucher_id"));
                v.setVoucherCode(rs.getString("voucher_code"));
                v.setVoucherName(rs.getString("voucher_name"));
                v.setDescription(rs.getString("description"));
                v.setDiscountType(Voucher.DiscountType.fromValue(rs.getString("discount_type")));
                v.setDiscountValue(rs.getBigDecimal("discount_value"));
                v.setMinOrderValue(rs.getBigDecimal("min_order_value"));
                v.setMaxDiscount(rs.getBigDecimal("max_discount"));

                java.sql.Date endDate = rs.getDate("end_date");
                if (endDate != null) {
                    v.setEndDate(endDate.toLocalDate());
                }

                uv.setVoucher(v);
            } catch (SQLException e) {
                // Voucher columns might not be present in the ResultSet
            }

            return uv;
        }
    }

    public int save(Long userId, Long voucherId) {
        String sql = "INSERT INTO user_vouchers (user_id, voucher_id, collected_at, is_used) " +
                "VALUES (?, ?, NOW(), FALSE) " +
                "ON DUPLICATE KEY UPDATE collected_at = NOW()";
        return jdbcTemplate.update(sql, userId, voucherId);
    }

    public List<UserVoucher> findByUserId(Long userId) {
        String sql = "SELECT uv.*, v.voucher_code, v.voucher_name, v.description, v.discount_type, " +
                "v.discount_value, v.min_order_value, v.max_discount, v.end_date " +
                "FROM user_vouchers uv " +
                "JOIN vouchers v ON uv.voucher_id = v.voucher_id " +
                "WHERE uv.user_id = ? AND uv.is_used = FALSE " +
                "ORDER BY uv.collected_at DESC";
        return jdbcTemplate.query(sql, new UserVoucherRowMapper(), userId);
    }

    public boolean existsByUserAndVoucher(Long userId, Long voucherId) {
        String sql = "SELECT COUNT(*) FROM user_vouchers WHERE user_id = ? AND voucher_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, voucherId);
        return count != null && count > 0;
    }

    public int markAsUsed(Long userId, Long voucherId) {
        String sql = "UPDATE user_vouchers SET is_used = TRUE, used_at = NOW() " +
                "WHERE user_id = ? AND voucher_id = ?";
        return jdbcTemplate.update(sql, userId, voucherId);
    }
}
