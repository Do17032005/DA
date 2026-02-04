package com.clothes.dao;

import com.clothes.model.Slider;
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
 * DAO for Slider management
 */
@Repository
public class SliderDAO {

    private final JdbcTemplate jdbcTemplate;

    public SliderDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Slider> sliderRowMapper = (rs, rowNum) -> {
        Slider slider = new Slider();
        slider.setSliderId(rs.getLong("slider_id"));
        slider.setTitle(rs.getString("title"));
        slider.setSubtitle(rs.getString("subtitle"));
        slider.setDescription(rs.getString("description"));
        slider.setImageUrl(rs.getString("image_url"));
        slider.setButtonText(rs.getString("button_text"));
        slider.setButtonLink(rs.getString("button_link"));
        slider.setDisplayOrder(rs.getInt("display_order"));
        slider.setIsActive(rs.getBoolean("is_active"));
        slider.setBackgroundColor(rs.getString("background_color"));
        slider.setTextColor(rs.getString("text_color"));
        slider.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        if (rs.getTimestamp("updated_at") != null) {
            slider.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }

        return slider;
    };

    /**
     * Create new slider
     */
    public Long save(Slider slider) {
        String sql = "INSERT INTO sliders (title, subtitle, description, image_url, button_text, " +
                "button_link, display_order, is_active, background_color, text_color, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, slider.getTitle());
            ps.setString(2, slider.getSubtitle());
            ps.setString(3, slider.getDescription());
            ps.setString(4, slider.getImageUrl());
            ps.setString(5, slider.getButtonText());
            ps.setString(6, slider.getButtonLink());
            ps.setInt(7, slider.getDisplayOrder());
            ps.setBoolean(8, slider.getIsActive());
            ps.setString(9, slider.getBackgroundColor());
            ps.setString(10, slider.getTextColor());
            ps.setObject(11, slider.getCreatedAt());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    /**
     * Update slider
     */
    public int update(Slider slider) {
        String sql = "UPDATE sliders SET title = ?, subtitle = ?, description = ?, image_url = ?, " +
                "button_text = ?, button_link = ?, display_order = ?, is_active = ?, " +
                "background_color = ?, text_color = ?, updated_at = ? WHERE slider_id = ?";

        return jdbcTemplate.update(sql,
                slider.getTitle(),
                slider.getSubtitle(),
                slider.getDescription(),
                slider.getImageUrl(),
                slider.getButtonText(),
                slider.getButtonLink(),
                slider.getDisplayOrder(),
                slider.getIsActive(),
                slider.getBackgroundColor(),
                slider.getTextColor(),
                LocalDateTime.now(),
                slider.getSliderId());
    }

    /**
     * Find slider by ID
     */
    public Optional<Slider> findById(Long sliderId) {
        String sql = "SELECT * FROM sliders WHERE slider_id = ?";

        List<Slider> sliders = jdbcTemplate.query(sql, sliderRowMapper, sliderId);
        return sliders.isEmpty() ? Optional.empty() : Optional.of(sliders.get(0));
    }

    /**
     * Get all sliders
     */
    public List<Slider> findAll() {
        String sql = "SELECT * FROM sliders ORDER BY display_order ASC, created_at DESC";
        return jdbcTemplate.query(sql, sliderRowMapper);
    }

    /**
     * Get active sliders only
     */
    public List<Slider> findAllActive() {
        String sql = "SELECT * FROM sliders WHERE is_active = true ORDER BY display_order ASC";
        return jdbcTemplate.query(sql, sliderRowMapper);
    }

    /**
     * Update slider status
     */
    public int updateStatus(Long sliderId, Boolean isActive) {
        String sql = "UPDATE sliders SET is_active = ?, updated_at = ? WHERE slider_id = ?";
        return jdbcTemplate.update(sql, isActive, LocalDateTime.now(), sliderId);
    }

    /**
     * Update display order
     */
    public int updateDisplayOrder(Long sliderId, Integer displayOrder) {
        String sql = "UPDATE sliders SET display_order = ?, updated_at = ? WHERE slider_id = ?";
        return jdbcTemplate.update(sql, displayOrder, LocalDateTime.now(), sliderId);
    }

    /**
     * Delete slider
     */
    public int delete(Long sliderId) {
        String sql = "DELETE FROM sliders WHERE slider_id = ?";
        return jdbcTemplate.update(sql, sliderId);
    }

    /**
     * Count total sliders
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM sliders";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    /**
     * Count active sliders
     */
    public int countActive() {
        String sql = "SELECT COUNT(*) FROM sliders WHERE is_active = true";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
