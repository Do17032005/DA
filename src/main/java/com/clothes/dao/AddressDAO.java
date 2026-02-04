package com.clothes.dao;

import com.clothes.model.Address;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * DAO for Address entity
 */
@Repository
public class AddressDAO {

    private final JdbcTemplate jdbcTemplate;

    public AddressDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static class AddressRowMapper implements RowMapper<Address> {
        @Override
        public Address mapRow(ResultSet rs, int rowNum) throws SQLException {
            Address address = new Address();
            address.setAddressId(rs.getLong("address_id"));
            address.setUserId(rs.getLong("user_id"));
            address.setRecipientName(rs.getString("full_name"));
            address.setPhoneNumber(rs.getString("phone"));
            address.setAddressLine(rs.getString("address_line"));
            address.setWard(rs.getString("ward"));
            address.setDistrict(rs.getString("district"));
            address.setCity(rs.getString("city"));
            address.setIsDefault(rs.getBoolean("is_default"));

            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) {
                address.setCreatedAt(created.toLocalDateTime());
            }

            Timestamp updated = rs.getTimestamp("updated_at");
            if (updated != null) {
                address.setUpdatedAt(updated.toLocalDateTime());
            }

            return address;
        }
    }

    public Long save(Address address) {
        // If this is set as default, unset other defaults for this user
        if (address.getIsDefault()) {
            unsetDefaultAddress(address.getUserId());
        }

        String sql = "INSERT INTO addresses (user_id, full_name, phone, address_line, " +
                "ward, district, city, is_default, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        jdbcTemplate.update(sql,
                address.getUserId(),
                address.getRecipientName(),
                address.getPhoneNumber(),
                address.getAddressLine(),
                address.getWard(),
                address.getDistrict(),
                address.getCity(),
                address.getIsDefault());

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public int update(Address address) {
        // If this is set as default, unset other defaults for this user
        if (address.getIsDefault()) {
            unsetDefaultAddress(address.getUserId());
        }

        String sql = "UPDATE addresses SET full_name = ?, phone = ?, address_line = ?, " +
                "ward = ?, district = ?, city = ?, is_default = ?, updated_at = NOW() " +
                "WHERE address_id = ?";

        return jdbcTemplate.update(sql,
                address.getRecipientName(),
                address.getPhoneNumber(),
                address.getAddressLine(),
                address.getWard(),
                address.getDistrict(),
                address.getCity(),
                address.getIsDefault(),
                address.getAddressId());
    }

    public Optional<Address> findById(Long addressId) {
        String sql = "SELECT * FROM addresses WHERE address_id = ?";
        List<Address> addresses = jdbcTemplate.query(sql, new AddressRowMapper(), addressId);
        return addresses.isEmpty() ? Optional.empty() : Optional.of(addresses.get(0));
    }

    public List<Address> findByUserId(Long userId) {
        String sql = "SELECT * FROM addresses WHERE user_id = ? ORDER BY is_default DESC, created_at DESC";
        return jdbcTemplate.query(sql, new AddressRowMapper(), userId);
    }

    public Optional<Address> findDefaultByUserId(Long userId) {
        String sql = "SELECT * FROM addresses WHERE user_id = ? AND is_default = TRUE";
        List<Address> addresses = jdbcTemplate.query(sql, new AddressRowMapper(), userId);
        return addresses.isEmpty() ? Optional.empty() : Optional.of(addresses.get(0));
    }

    public int delete(Long addressId) {
        String sql = "DELETE FROM addresses WHERE address_id = ?";
        return jdbcTemplate.update(sql, addressId);
    }

    public int setAsDefault(Long addressId, Long userId) {
        // First unset all defaults for this user
        unsetDefaultAddress(userId);

        // Then set this one as default
        String sql = "UPDATE addresses SET is_default = TRUE, updated_at = NOW() " +
                "WHERE address_id = ? AND user_id = ?";
        return jdbcTemplate.update(sql, addressId, userId);
    }

    private void unsetDefaultAddress(Long userId) {
        String sql = "UPDATE addresses SET is_default = FALSE WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }

    public int countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM addresses WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null ? count : 0;
    }
}
