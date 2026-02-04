package com.clothes.dao;

import com.clothes.model.Banner;
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
 * DAO for Banner management
 */
@Repository
public class BannerDAO {

    private final JdbcTemplate jdbcTemplate;

    public BannerDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Banner> bannerRowMapper = (rs, rowNum) -> {
        Banner banner = new Banner();
        banner.setBannerId(rs.getLong("banner_id"));
        banner.setTitle(rs.getString("title"));
        banner.setDescription(rs.getString("description"));
        banner.setImageUrl(rs.getString("image_url"));
        banner.setLinkUrl(rs.getString("link_url"));
        banner.setDisplayOrder(rs.getInt("display_order"));
        banner.setIsActive(rs.getBoolean("is_active"));

        if (rs.getTimestamp("start_date") != null) {
            banner.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
        }
        if (rs.getTimestamp("end_date") != null) {
            banner.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());
        }

        banner.setPosition(rs.getString("position"));
        banner.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        if (rs.getTimestamp("updated_at") != null) {
            banner.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }

        return banner;
    };

    /**
     * Create new banner
     */
    public Long save(Banner banner) {
        String sql = "INSERT INTO banners (title, description, image_url, link_url, display_order, " +
                "is_active, start_date, end_date, position, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, banner.getTitle());
            ps.setString(2, banner.getDescription());
            ps.setString(3, banner.getImageUrl());
            ps.setString(4, banner.getLinkUrl());
            ps.setInt(5, banner.getDisplayOrder());
            ps.setBoolean(6, banner.getIsActive());
            ps.setObject(7, banner.getStartDate());
            ps.setObject(8, banner.getEndDate());
            ps.setString(9, banner.getPosition());
            ps.setObject(10, banner.getCreatedAt());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    /**
     * Update banner
     */
    public int update(Banner banner) {
        String sql = "UPDATE banners SET title = ?, description = ?, image_url = ?, link_url = ?, " +
                "display_order = ?, is_active = ?, start_date = ?, end_date = ?, position = ?, " +
                "updated_at = ? WHERE banner_id = ?";

        return jdbcTemplate.update(sql,
                banner.getTitle(),
                banner.getDescription(),
                banner.getImageUrl(),
                banner.getLinkUrl(),
                banner.getDisplayOrder(),
                banner.getIsActive(),
                banner.getStartDate(),
                banner.getEndDate(),
                banner.getPosition(),
                LocalDateTime.now(),
                banner.getBannerId());
    }

    /**
     * Find banner by ID
     */
    public Optional<Banner> findById(Long bannerId) {
        String sql = "SELECT * FROM banners WHERE banner_id = ?";

        List<Banner> banners = jdbcTemplate.query(sql, bannerRowMapper, bannerId);
        return banners.isEmpty() ? Optional.empty() : Optional.of(banners.get(0));
    }

    /**
     * Get all banners
     */
    public List<Banner> findAll() {
        String sql = "SELECT * FROM banners ORDER BY display_order ASC, created_at DESC";
        return jdbcTemplate.query(sql, bannerRowMapper);
    }

    /**
     * Get active banners only
     */
    public List<Banner> findAllActive() {
        String sql = "SELECT * FROM banners WHERE is_active = true " +
                "AND (start_date IS NULL OR start_date <= NOW()) " +
                "AND (end_date IS NULL OR end_date >= NOW()) " +
                "ORDER BY display_order ASC";
        return jdbcTemplate.query(sql, bannerRowMapper);
    }

    /**
     * Get banners by position
     */
    public List<Banner> findByPosition(String position) {
        String sql = "SELECT * FROM banners WHERE position = ? AND is_active = true " +
                "AND (start_date IS NULL OR start_date <= NOW()) " +
                "AND (end_date IS NULL OR end_date >= NOW()) " +
                "ORDER BY display_order ASC";
        return jdbcTemplate.query(sql, bannerRowMapper, position);
    }

    /**
     * Update banner status
     */
    public int updateStatus(Long bannerId, Boolean isActive) {
        String sql = "UPDATE banners SET is_active = ?, updated_at = ? WHERE banner_id = ?";
        return jdbcTemplate.update(sql, isActive, LocalDateTime.now(), bannerId);
    }

    /**
     * Update display order
     */
    public int updateDisplayOrder(Long bannerId, Integer displayOrder) {
        String sql = "UPDATE banners SET display_order = ?, updated_at = ? WHERE banner_id = ?";
        return jdbcTemplate.update(sql, displayOrder, LocalDateTime.now(), bannerId);
    }

    /**
     * Delete banner
     */
    public int delete(Long bannerId) {
        String sql = "DELETE FROM banners WHERE banner_id = ?";
        return jdbcTemplate.update(sql, bannerId);
    }

    /**
     * Count total banners
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM banners";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    /**
     * Count active banners
     */
    public int countActive() {
        String sql = "SELECT COUNT(*) FROM banners WHERE is_active = true " +
                "AND (start_date IS NULL OR start_date <= NOW()) " +
                "AND (end_date IS NULL OR end_date >= NOW())";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
