package com.clothes.dao;

import com.clothes.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DAO for User entity
 */
@Repository
public class UserDAO {

    private final JdbcTemplate jdbcTemplate;

    public UserDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setUserId(rs.getLong("user_id"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setPassword(rs.getString("password"));
            user.setFullName(rs.getString("full_name"));
            user.setPhone(rs.getString("phone"));
            user.setRole(rs.getString("role"));

            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) {
                user.setCreatedAt(created.toLocalDateTime());
            }

            Timestamp updated = rs.getTimestamp("updated_at");
            if (updated != null) {
                user.setUpdatedAt(updated.toLocalDateTime());
            }

            user.setIsActive(rs.getBoolean("is_active"));

            user.setIsVip(rs.getBoolean("is_vip"));
            user.setAvatarUrl(rs.getString("avatar_url"));
            user.setGender(rs.getString("gender"));
            java.sql.Date dob = rs.getDate("date_of_birth");
            if (dob != null) {
                user.setDateOfBirth(dob.toLocalDate());
            }

            return user;
        }
    }

    public Long save(User user) {
        String sql = "INSERT INTO users (username, email, password, full_name, phone, role, " +
                "is_vip, avatar_url, gender, date_of_birth, created_at, updated_at, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), ?)";

        org.springframework.jdbc.support.GeneratedKeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getPhone());
            ps.setString(6, user.getRole() != null ? user.getRole() : "USER");
            ps.setBoolean(7, user.getIsVip() != null ? user.getIsVip() : false);
            ps.setString(8, user.getAvatarUrl());
            ps.setString(9, user.getGender());
            ps.setDate(10, user.getDateOfBirth() != null ? java.sql.Date.valueOf(user.getDateOfBirth()) : null);
            ps.setBoolean(11, user.getIsActive() != null ? user.getIsActive() : true);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    public int update(User user) {
        String sql = "UPDATE users SET email = ?, full_name = ?, phone = ?, " +
                "is_vip = ?, avatar_url = ?, gender = ?, date_of_birth = ?, " +
                "updated_at = NOW(), is_active = ? " +
                "WHERE user_id = ?";

        return jdbcTemplate.update(sql,
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getIsVip(),
                user.getAvatarUrl(),
                user.getGender(),
                user.getDateOfBirth() != null ? java.sql.Date.valueOf(user.getDateOfBirth()) : null,
                user.getIsActive(),
                user.getUserId());
    }

    public int updatePassword(Long userId, String newPassword) {
        String sql = "UPDATE users SET password = ?, updated_at = NOW() WHERE user_id = ?";
        return jdbcTemplate.update(sql, newPassword, userId);
    }

    public Optional<User> findById(Long userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        List<User> users = jdbcTemplate.query(sql, new UserRowMapper(), userId);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        List<User> users = jdbcTemplate.query(sql, new UserRowMapper(), username);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        List<User> users = jdbcTemplate.query(sql, new UserRowMapper(), email);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new UserRowMapper());
    }

    public List<User> findAllActive() {
        String sql = "SELECT * FROM users WHERE is_active = TRUE ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new UserRowMapper());
    }

    public int delete(Long userId) {
        String sql = "UPDATE users SET is_active = FALSE, updated_at = NOW() WHERE user_id = ?";
        return jdbcTemplate.update(sql, userId);
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
        return count != null && count > 0;
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM users WHERE is_active = TRUE";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Search users by username, email, or full name
     */
    public List<User> search(String keyword) {
        String sql = "SELECT * FROM users WHERE username LIKE ? OR email LIKE ? OR full_name LIKE ?";
        String searchPattern = "%" + keyword + "%";
        return jdbcTemplate.query(sql, new UserRowMapper(), searchPattern, searchPattern, searchPattern);
    }

    /**
     * Find users by role
     */
    public List<User> findByRole(String role) {
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new UserRowMapper(), role);
    }

    /**
     * Update user status (active/inactive)
     */
    public int updateStatus(Long userId, boolean isActive) {
        String sql = "UPDATE users SET is_active = ?, updated_at = NOW() WHERE user_id = ?";
        return jdbcTemplate.update(sql, isActive, userId);
    }

    /**
     * Find VIP users
     */
    public List<User> findVipUsers() {
        String sql = "SELECT * FROM users WHERE is_vip = TRUE AND is_active = TRUE ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new UserRowMapper());
    }

    /**
     * Update VIP status
     */
    public int updateVipStatus(Long userId, Boolean isVip) {
        String sql = "UPDATE users SET is_vip = ?, updated_at = NOW() WHERE user_id = ?";
        return jdbcTemplate.update(sql, isVip, userId);
    }

    /**
     * Update avatar URL
     */
    public int updateAvatarUrl(Long userId, String avatarUrl) {
        String sql = "UPDATE users SET avatar_url = ?, updated_at = NOW() WHERE user_id = ?";
        return jdbcTemplate.update(sql, avatarUrl, userId);
    }

    /**
     * Get user statistics using view
     */
    public List<Map<String, Object>> getUsersWithStats() {
        String sql = "SELECT * FROM v_customers_with_stats WHERE role = 'USER' ORDER BY created_at DESC";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Count VIP users
     */
    public int countVipUsers() {
        String sql = "SELECT COUNT(*) FROM users WHERE is_vip = TRUE AND is_active = TRUE";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }
}
