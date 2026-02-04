package com.clothes.dao;

import com.clothes.model.Voucher;
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
 * DAO for Voucher entity
 */
@Repository
public class VoucherDAO {

    private final JdbcTemplate jdbcTemplate;

    public VoucherDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static class VoucherRowMapper implements RowMapper<Voucher> {
        @Override
        public Voucher mapRow(ResultSet rs, int rowNum) throws SQLException {
            Voucher voucher = new Voucher();
            voucher.setVoucherId(rs.getLong("voucher_id"));

            // Use new column names from migration
            voucher.setVoucherCode(rs.getString("voucher_code"));
            voucher.setVoucherName(rs.getString("voucher_name"));
            voucher.setDescription(rs.getString("description"));
            voucher.setDiscountType(Voucher.DiscountType.fromValue(rs.getString("discount_type")));
            voucher.setDiscountValue(rs.getBigDecimal("discount_value"));
            voucher.setMinOrderValue(rs.getBigDecimal("min_order_value"));
            voucher.setMaxDiscount(rs.getBigDecimal("max_discount"));
            voucher.setQuantity(rs.getInt("quantity"));
            voucher.setUsedCount(rs.getInt("used_count"));

            java.sql.Date startDate = rs.getDate("start_date");
            if (startDate != null) {
                voucher.setStartDate(startDate.toLocalDate());
            }

            java.sql.Date endDate = rs.getDate("end_date");
            if (endDate != null) {
                voucher.setEndDate(endDate.toLocalDate());
            }

            voucher.setIsActive(rs.getBoolean("is_active"));

            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) {
                voucher.setCreatedAt(created.toLocalDateTime());
            }

            return voucher;
        }
    }

    public Long save(Voucher voucher) {
        String sql = "INSERT INTO vouchers (voucher_code, voucher_name, description, discount_type, discount_value, " +
                "min_order_value, max_discount, quantity, used_count, " +
                "start_date, end_date, is_active, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        jdbcTemplate.update(sql,
                voucher.getVoucherCode(),
                voucher.getVoucherName(),
                voucher.getDescription(),
                voucher.getDiscountType().getValue(),
                voucher.getDiscountValue(),
                voucher.getMinOrderValue(),
                voucher.getMaxDiscount(),
                voucher.getQuantity(),
                voucher.getUsedCount(),
                voucher.getStartDate() != null ? java.sql.Date.valueOf(voucher.getStartDate()) : null,
                voucher.getEndDate() != null ? java.sql.Date.valueOf(voucher.getEndDate()) : null,
                voucher.getIsActive());

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public int update(Voucher voucher) {
        String sql = "UPDATE vouchers SET voucher_code = ?, voucher_name = ?, description = ?, discount_type = ?, " +
                "discount_value = ?, min_order_value = ?, max_discount = ?, " +
                "quantity = ?, used_count = ?, start_date = ?, end_date = ?, " +
                "is_active = ? WHERE voucher_id = ?";

        return jdbcTemplate.update(sql,
                voucher.getVoucherCode(),
                voucher.getVoucherName(),
                voucher.getDescription(),
                voucher.getDiscountType().getValue(),
                voucher.getDiscountValue(),
                voucher.getMinOrderValue(),
                voucher.getMaxDiscount(),
                voucher.getQuantity(),
                voucher.getUsedCount(),
                voucher.getStartDate() != null ? java.sql.Date.valueOf(voucher.getStartDate()) : null,
                voucher.getEndDate() != null ? java.sql.Date.valueOf(voucher.getEndDate()) : null,
                voucher.getIsActive(),
                voucher.getVoucherId());
    }

    public Optional<Voucher> findById(Long voucherId) {
        String sql = "SELECT * FROM vouchers WHERE voucher_id = ?";
        List<Voucher> vouchers = jdbcTemplate.query(sql, new VoucherRowMapper(), voucherId);
        return vouchers.isEmpty() ? Optional.empty() : Optional.of(vouchers.get(0));
    }

    public Optional<Voucher> findByCode(String code) {
        String sql = "SELECT * FROM vouchers WHERE voucher_code = ?";
        List<Voucher> vouchers = jdbcTemplate.query(sql, new VoucherRowMapper(), code);
        return vouchers.isEmpty() ? Optional.empty() : Optional.of(vouchers.get(0));
    }

    public List<Voucher> findAll() {
        String sql = "SELECT * FROM vouchers ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new VoucherRowMapper());
    }

    public List<Voucher> findAllActive() {
        String sql = "SELECT * FROM vouchers WHERE is_active = TRUE " +
                "AND (start_date IS NULL OR start_date <= NOW()) " +
                "AND (end_date IS NULL OR end_date >= NOW()) " +
                "ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new VoucherRowMapper());
    }

    public List<Voucher> findValidVouchers() {
        String sql = "SELECT * FROM vouchers WHERE is_active = TRUE " +
                "AND (start_date IS NULL OR start_date <= NOW()) " +
                "AND (end_date IS NULL OR end_date >= NOW()) " +
                "AND (quantity IS NULL OR used_count < quantity) " +
                "ORDER BY discount_value DESC";
        return jdbcTemplate.query(sql, new VoucherRowMapper());
    }

    public int incrementUsedCount(Long voucherId) {
        String sql = "UPDATE vouchers SET used_count = used_count + 1 WHERE voucher_id = ?";
        return jdbcTemplate.update(sql, voucherId);
    }

    public int delete(Long voucherId) {
        String sql = "DELETE FROM vouchers WHERE voucher_id = ?";
        return jdbcTemplate.update(sql, voucherId);
    }

    public int deactivate(Long voucherId) {
        String sql = "UPDATE vouchers SET is_active = FALSE WHERE voucher_id = ?";
        return jdbcTemplate.update(sql, voucherId);
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM vouchers WHERE is_active = TRUE";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Find all inactive vouchers
     */
    public List<Voucher> findAllInactive() {
        String sql = "SELECT * FROM vouchers WHERE is_active = FALSE ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new VoucherRowMapper());
    }

    /**
     * Update voucher status
     */
    public int updateStatus(Long voucherId, boolean isActive) {
        String sql = "UPDATE vouchers SET is_active = ?, updated_at = NOW() WHERE voucher_id = ?";
        return jdbcTemplate.update(sql, isActive, voucherId);
    }
}
