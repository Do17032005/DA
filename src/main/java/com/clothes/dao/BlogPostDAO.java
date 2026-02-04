package com.clothes.dao;

import com.clothes.model.BlogPost;
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
 * DAO for Blog Post management
 */
@Repository
public class BlogPostDAO {

    private final JdbcTemplate jdbcTemplate;

    public BlogPostDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<BlogPost> blogPostRowMapper = (rs, rowNum) -> {
        BlogPost post = new BlogPost();
        post.setPostId(rs.getLong("post_id"));
        post.setTitle(rs.getString("title"));
        post.setSlug(rs.getString("slug"));
        post.setExcerpt(rs.getString("excerpt"));
        post.setContent(rs.getString("content"));
        post.setFeaturedImage(rs.getString("featured_image"));
        post.setAuthorId(rs.getLong("author_id"));
        post.setAuthorName(rs.getString("author_name"));
        post.setCategory(rs.getString("category"));
        post.setTags(rs.getString("tags"));
        post.setIsPublished(rs.getBoolean("is_published"));
        post.setIsFeatured(rs.getBoolean("is_featured"));
        post.setViewCount(rs.getInt("view_count"));

        if (rs.getTimestamp("published_at") != null) {
            post.setPublishedAt(rs.getTimestamp("published_at").toLocalDateTime());
        }

        post.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        if (rs.getTimestamp("updated_at") != null) {
            post.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }

        return post;
    };

    /**
     * Create new blog post
     */
    public Long save(BlogPost post) {
        String sql = "INSERT INTO blog_posts (title, slug, excerpt, content, featured_image, author_id, " +
                "author_name, category, tags, is_published, is_featured, view_count, " +
                "published_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getSlug());
            ps.setString(3, post.getExcerpt());
            ps.setString(4, post.getContent());
            ps.setString(5, post.getFeaturedImage());
            ps.setLong(6, post.getAuthorId());
            ps.setString(7, post.getAuthorName());
            ps.setString(8, post.getCategory());
            ps.setString(9, post.getTags());
            ps.setBoolean(10, post.getIsPublished());
            ps.setBoolean(11, post.getIsFeatured());
            ps.setInt(12, post.getViewCount());
            ps.setObject(13, post.getPublishedAt());
            ps.setObject(14, post.getCreatedAt());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    /**
     * Update blog post
     */
    public int update(BlogPost post) {
        String sql = "UPDATE blog_posts SET title = ?, slug = ?, excerpt = ?, content = ?, " +
                "featured_image = ?, author_id = ?, author_name = ?, category = ?, tags = ?, " +
                "is_published = ?, is_featured = ?, published_at = ?, updated_at = ? WHERE post_id = ?";

        return jdbcTemplate.update(sql,
                post.getTitle(),
                post.getSlug(),
                post.getExcerpt(),
                post.getContent(),
                post.getFeaturedImage(),
                post.getAuthorId(),
                post.getAuthorName(),
                post.getCategory(),
                post.getTags(),
                post.getIsPublished(),
                post.getIsFeatured(),
                post.getPublishedAt(),
                LocalDateTime.now(),
                post.getPostId());
    }

    /**
     * Find blog post by ID
     */
    public Optional<BlogPost> findById(Long postId) {
        String sql = "SELECT * FROM blog_posts WHERE post_id = ?";

        List<BlogPost> posts = jdbcTemplate.query(sql, blogPostRowMapper, postId);
        return posts.isEmpty() ? Optional.empty() : Optional.of(posts.get(0));
    }

    /**
     * Find blog post by slug
     */
    public Optional<BlogPost> findBySlug(String slug) {
        String sql = "SELECT * FROM blog_posts WHERE slug = ?";

        List<BlogPost> posts = jdbcTemplate.query(sql, blogPostRowMapper, slug);
        return posts.isEmpty() ? Optional.empty() : Optional.of(posts.get(0));
    }

    /**
     * Get all blog posts
     */
    public List<BlogPost> findAll() {
        String sql = "SELECT * FROM blog_posts ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, blogPostRowMapper);
    }

    /**
     * Get published posts only
     */
    public List<BlogPost> findAllPublished() {
        String sql = "SELECT * FROM blog_posts WHERE is_published = true ORDER BY published_at DESC";
        return jdbcTemplate.query(sql, blogPostRowMapper);
    }

    /**
     * Get featured posts
     */
    public List<BlogPost> findFeatured(int limit) {
        String sql = "SELECT * FROM blog_posts WHERE is_published = true AND is_featured = true " +
                "ORDER BY published_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, blogPostRowMapper, limit);
    }

    /**
     * Get posts by category
     */
    public List<BlogPost> findByCategory(String category) {
        String sql = "SELECT * FROM blog_posts WHERE category = ? AND is_published = true " +
                "ORDER BY published_at DESC";
        return jdbcTemplate.query(sql, blogPostRowMapper, category);
    }

    /**
     * Search posts by keyword
     */
    public List<BlogPost> search(String keyword) {
        String sql = "SELECT * FROM blog_posts WHERE is_published = true " +
                "AND (title LIKE ? OR content LIKE ? OR tags LIKE ?) " +
                "ORDER BY published_at DESC";
        String searchPattern = "%" + keyword + "%";
        return jdbcTemplate.query(sql, blogPostRowMapper, searchPattern, searchPattern, searchPattern);
    }

    /**
     * Update publish status
     */
    public int updatePublishStatus(Long postId, Boolean isPublished) {
        String sql = "UPDATE blog_posts SET is_published = ?, published_at = ?, updated_at = ? " +
                "WHERE post_id = ?";
        LocalDateTime publishedAt = isPublished ? LocalDateTime.now() : null;
        return jdbcTemplate.update(sql, isPublished, publishedAt, LocalDateTime.now(), postId);
    }

    /**
     * Update featured status
     */
    public int updateFeaturedStatus(Long postId, Boolean isFeatured) {
        String sql = "UPDATE blog_posts SET is_featured = ?, updated_at = ? WHERE post_id = ?";
        return jdbcTemplate.update(sql, isFeatured, LocalDateTime.now(), postId);
    }

    /**
     * Increment view count
     */
    public int incrementViewCount(Long postId) {
        String sql = "UPDATE blog_posts SET view_count = view_count + 1 WHERE post_id = ?";
        return jdbcTemplate.update(sql, postId);
    }

    /**
     * Delete blog post
     */
    public int delete(Long postId) {
        String sql = "DELETE FROM blog_posts WHERE post_id = ?";
        return jdbcTemplate.update(sql, postId);
    }

    /**
     * Count total posts
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM blog_posts";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    /**
     * Count published posts
     */
    public int countPublished() {
        String sql = "SELECT COUNT(*) FROM blog_posts WHERE is_published = true";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    /**
     * Get recent posts
     */
    public List<BlogPost> findRecent(int limit) {
        String sql = "SELECT * FROM blog_posts WHERE is_published = true " +
                "ORDER BY published_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, blogPostRowMapper, limit);
    }

    /**
     * Get popular posts by view count
     */
    public List<BlogPost> findPopular(int limit) {
        String sql = "SELECT * FROM blog_posts WHERE is_published = true " +
                "ORDER BY view_count DESC LIMIT ?";
        return jdbcTemplate.query(sql, blogPostRowMapper, limit);
    }
}
