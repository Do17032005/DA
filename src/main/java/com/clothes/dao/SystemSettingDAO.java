package com.clothes.dao;

import com.clothes.model.SystemSetting;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO for SystemSetting management
 */
@Repository
public class SystemSettingDAO {

    private final JdbcTemplate jdbcTemplate;

    public SystemSettingDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<SystemSetting> settingRowMapper = (rs, rowNum) -> {
        SystemSetting setting = new SystemSetting();
        setting.setSettingId(rs.getLong("setting_id"));
        setting.setSettingKey(rs.getString("setting_key"));
        setting.setSettingValue(rs.getString("setting_value"));
        setting.setCategory(rs.getString("category"));
        setting.setDataType(rs.getString("data_type"));
        setting.setDescription(rs.getString("description"));
        setting.setIsEditable(rs.getBoolean("is_editable"));

        Long updatedBy = rs.getLong("updated_by");
        if (!rs.wasNull()) {
            setting.setUpdatedBy(updatedBy);
        }

        if (rs.getTimestamp("updated_at") != null) {
            setting.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }

        setting.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        return setting;
    };

    /**
     * Find setting by key
     */
    public Optional<SystemSetting> findByKey(String settingKey) {
        String sql = "SELECT * FROM system_settings WHERE setting_key = ?";

        List<SystemSetting> settings = jdbcTemplate.query(sql, settingRowMapper, settingKey);
        return settings.isEmpty() ? Optional.empty() : Optional.of(settings.get(0));
    }

    /**
     * Get setting value by key (returns null if not found)
     */
    public String getValue(String settingKey) {
        Optional<SystemSetting> setting = findByKey(settingKey);
        return setting.map(SystemSetting::getSettingValue).orElse(null);
    }

    /**
     * Get setting value with default
     */
    public String getValue(String settingKey, String defaultValue) {
        String value = getValue(settingKey);
        return value != null ? value : defaultValue;
    }

    /**
     * Find all settings by category
     */
    public List<SystemSetting> findByCategory(String category) {
        String sql = "SELECT * FROM system_settings WHERE category = ? ORDER BY setting_key";
        return jdbcTemplate.query(sql, settingRowMapper, category);
    }

    /**
     * Get all settings
     */
    public List<SystemSetting> findAll() {
        String sql = "SELECT * FROM system_settings ORDER BY category, setting_key";
        return jdbcTemplate.query(sql, settingRowMapper);
    }

    /**
     * Create new setting
     */
    public Long save(SystemSetting setting) {
        String sql = "INSERT INTO system_settings (setting_key, setting_value, category, data_type, " +
                "description, is_editable, updated_by, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, setting.getSettingKey());
            ps.setString(2, setting.getSettingValue());
            ps.setString(3, setting.getCategory());
            ps.setString(4, setting.getDataType());
            ps.setString(5, setting.getDescription());
            ps.setBoolean(6, setting.getIsEditable());
            ps.setObject(7, setting.getUpdatedBy());
            ps.setObject(8, setting.getCreatedAt());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    /**
     * Update setting value
     */
    public int updateValue(String settingKey, String settingValue, Long updatedBy) {
        String sql = "UPDATE system_settings SET setting_value = ?, updated_by = ?, updated_at = ? " +
                "WHERE setting_key = ?";
        return jdbcTemplate.update(sql, settingValue, updatedBy, LocalDateTime.now(), settingKey);
    }

    /**
     * Update entire setting
     */
    public int update(SystemSetting setting) {
        String sql = "UPDATE system_settings SET setting_value = ?, category = ?, data_type = ?, " +
                "description = ?, is_editable = ?, updated_by = ?, updated_at = ? " +
                "WHERE setting_key = ?";

        return jdbcTemplate.update(sql,
                setting.getSettingValue(),
                setting.getCategory(),
                setting.getDataType(),
                setting.getDescription(),
                setting.getIsEditable(),
                setting.getUpdatedBy(),
                LocalDateTime.now(),
                setting.getSettingKey());
    }

    /**
     * Delete setting
     */
    public int delete(String settingKey) {
        String sql = "DELETE FROM system_settings WHERE setting_key = ?";
        return jdbcTemplate.update(sql, settingKey);
    }

    /**
     * Check if setting exists
     */
    public boolean exists(String settingKey) {
        String sql = "SELECT COUNT(*) FROM system_settings WHERE setting_key = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, settingKey);
        return count != null && count > 0;
    }

    /**
     * Batch update multiple settings
     */
    public void batchUpdate(List<SystemSetting> settings, Long updatedBy) {
        String sql = "INSERT INTO system_settings (setting_key, setting_value, category, data_type, " +
                "description, is_editable, updated_by, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), " +
                "updated_by = VALUES(updated_by), updated_at = NOW()";

        jdbcTemplate.batchUpdate(sql, settings, settings.size(), (ps, setting) -> {
            ps.setString(1, setting.getSettingKey());
            ps.setString(2, setting.getSettingValue());
            ps.setString(3, setting.getCategory());
            ps.setString(4, setting.getDataType());
            ps.setString(5, setting.getDescription());
            ps.setBoolean(6, setting.getIsEditable());
            ps.setObject(7, updatedBy);
            ps.setObject(8, LocalDateTime.now());
        });
    }

    /**
     * Count settings by category
     */
    public int countByCategory(String category) {
        String sql = "SELECT COUNT(*) FROM system_settings WHERE category = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, category);
        return count != null ? count : 0;
    }

    /**
     * Initialize default settings if not exist
     */
    public void initializeDefaults() {
        // Only insert if table is empty
        String countSql = "SELECT COUNT(*) FROM system_settings";
        Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);

        if (count != null && count == 0) {
            String sql = "INSERT INTO system_settings (setting_key, setting_value, category, data_type, " +
                    "description, is_editable, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";

            // General settings
            jdbcTemplate.update(sql, "site_name", "Clothes Shop", "general", "string", "Tên website", true);
            jdbcTemplate.update(sql, "site_logo", "/images/logo.png", "general", "string", "Logo website", true);
            jdbcTemplate.update(sql, "contact_email", "contact@clothesshop.com", "general", "string", "Email liên hệ",
                    true);
            jdbcTemplate.update(sql, "contact_phone", "0123456789", "general", "string", "Số điện thoại", true);

            // Payment settings
            jdbcTemplate.update(sql, "payment_cod_enabled", "true", "payment", "boolean", "Bật COD", true);
            jdbcTemplate.update(sql, "payment_bank_enabled", "true", "payment", "boolean", "Bật chuyển khoản", true);

            // Shipping settings
            jdbcTemplate.update(sql, "shipping_fee_default", "30000", "shipping", "number", "Phí ship mặc định", true);
            jdbcTemplate.update(sql, "free_shipping_threshold", "500000", "shipping", "number", "Miễn phí ship từ",
                    true);
        }
    }
}
