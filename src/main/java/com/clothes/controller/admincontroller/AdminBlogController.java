package com.clothes.controller.admincontroller;

import com.clothes.dao.BlogPostDAO;
import com.clothes.model.BlogPost;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Admin Controller for Blog Post Management
 */
@Controller
@RequestMapping("/admin/blog")
public class AdminBlogController {

    private final BlogPostDAO blogPostDAO;

    public AdminBlogController(BlogPostDAO blogPostDAO) {
        this.blogPostDAO = blogPostDAO;
    }

    /**
     * Check if user is admin
     */
    private boolean isAdmin(HttpSession session) {
        String role = (String) session.getAttribute("role");
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }

    /**
     * List all blog posts with filters
     */
    @GetMapping
    public String listPosts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean published,
            HttpSession session,
            Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        List<BlogPost> posts;

        if (search != null && !search.isEmpty()) {
            posts = blogPostDAO.search(search);
        } else if (category != null && !category.isEmpty()) {
            posts = blogPostDAO.findByCategory(category);
        } else if (published != null && published) {
            posts = blogPostDAO.findAllPublished();
        } else {
            posts = blogPostDAO.findAll();
        }

        model.addAttribute("posts", posts);
        model.addAttribute("totalPosts", blogPostDAO.count());
        model.addAttribute("publishedPosts", blogPostDAO.countPublished());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("selectedPublished", published);

        return "admin/blog-posts";
    }

    /**
     * Show create blog post form
     */
    @GetMapping("/create")
    public String showCreateForm(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        model.addAttribute("post", new BlogPost());
        return "admin/blog-form";
    }

    /**
     * Create new blog post
     */
    @PostMapping("/create")
    public String createPost(
            @RequestParam String title,
            @RequestParam String slug,
            @RequestParam(required = false) String excerpt,
            @RequestParam String content,
            @RequestParam(required = false) String featuredImage,
            @RequestParam String authorName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tags,
            @RequestParam(defaultValue = "false") Boolean isPublished,
            @RequestParam(defaultValue = "false") Boolean isFeatured,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            Long authorId = (Long) session.getAttribute("userId");
            if (authorId == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin người dùng!");
                return "redirect:/admin/blog/create";
            }

            BlogPost post = new BlogPost();
            post.setTitle(title);
            post.setSlug(slug);
            post.setExcerpt(excerpt);
            post.setContent(content);
            post.setFeaturedImage(featuredImage);
            post.setAuthorId(authorId);
            post.setAuthorName(authorName);
            post.setCategory(category);
            post.setTags(tags);
            post.setIsPublished(isPublished);
            post.setIsFeatured(isFeatured);

            if (isPublished) {
                post.setPublishedAt(LocalDateTime.now());
            }

            Long postId = blogPostDAO.save(post);

            redirectAttributes.addFlashAttribute("success",
                    "Bài viết đã được tạo thành công! ID: " + postId);
            return "redirect:/admin/blog";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi khi tạo bài viết: " + e.getMessage());
            return "redirect:/admin/blog/create";
        }
    }

    /**
     * Show edit blog post form
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        Optional<BlogPost> postOpt = blogPostDAO.findById(id);
        if (postOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy bài viết!");
            return "redirect:/admin/blog";
        }

        model.addAttribute("post", postOpt.get());
        return "admin/blog-form";
    }

    /**
     * Update blog post
     */
    @PostMapping("/edit/{id}")
    public String updatePost(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String slug,
            @RequestParam(required = false) String excerpt,
            @RequestParam String content,
            @RequestParam(required = false) String featuredImage,
            @RequestParam String authorName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tags,
            @RequestParam(defaultValue = "false") Boolean isPublished,
            @RequestParam(defaultValue = "false") Boolean isFeatured,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            Optional<BlogPost> postOpt = blogPostDAO.findById(id);
            if (postOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy bài viết!");
                return "redirect:/admin/blog";
            }

            BlogPost post = postOpt.get();
            boolean wasPublished = post.getIsPublished();

            post.setTitle(title);
            post.setSlug(slug);
            post.setExcerpt(excerpt);
            post.setContent(content);
            post.setFeaturedImage(featuredImage);
            post.setAuthorName(authorName);
            post.setCategory(category);
            post.setTags(tags);
            post.setIsPublished(isPublished);
            post.setIsFeatured(isFeatured);

            // Set published date when first published
            if (isPublished && !wasPublished) {
                post.setPublishedAt(LocalDateTime.now());
            }

            blogPostDAO.update(post);

            redirectAttributes.addFlashAttribute("success", "Bài viết đã được cập nhật!");
            return "redirect:/admin/blog";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi khi cập nhật bài viết: " + e.getMessage());
            return "redirect:/admin/blog/edit/" + id;
        }
    }

    /**
     * Toggle publish status
     */
    @PostMapping("/{id}/toggle-publish")
    public String togglePublishStatus(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            Optional<BlogPost> postOpt = blogPostDAO.findById(id);
            if (postOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy bài viết!");
                return "redirect:/admin/blog";
            }

            BlogPost post = postOpt.get();
            blogPostDAO.updatePublishStatus(id, !post.getIsPublished());

            String status = post.getIsPublished() ? "ẩn" : "xuất bản";
            redirectAttributes.addFlashAttribute("success",
                    "Bài viết đã được " + status + "!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi khi thay đổi trạng thái: " + e.getMessage());
        }

        return "redirect:/admin/blog";
    }

    /**
     * Toggle featured status
     */
    @PostMapping("/{id}/toggle-featured")
    public String toggleFeaturedStatus(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            Optional<BlogPost> postOpt = blogPostDAO.findById(id);
            if (postOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy bài viết!");
                return "redirect:/admin/blog";
            }

            BlogPost post = postOpt.get();
            blogPostDAO.updateFeaturedStatus(id, !post.getIsFeatured());

            String status = post.getIsFeatured() ? "bỏ nổi bật" : "đánh dấu nổi bật";
            redirectAttributes.addFlashAttribute("success",
                    "Bài viết đã được " + status + "!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi khi thay đổi trạng thái: " + e.getMessage());
        }

        return "redirect:/admin/blog";
    }

    /**
     * Delete blog post
     */
    @PostMapping("/delete/{id}")
    public String deletePost(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            Optional<BlogPost> postOpt = blogPostDAO.findById(id);
            if (postOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy bài viết!");
                return "redirect:/admin/blog";
            }

            blogPostDAO.delete(id);
            redirectAttributes.addFlashAttribute("success", "Bài viết đã được xóa!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi khi xóa bài viết: " + e.getMessage());
        }

        return "redirect:/admin/blog";
    }

    /**
     * View blog post statistics
     */
    @GetMapping("/stats")
    public String viewStats(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        List<BlogPost> popularPosts = blogPostDAO.findPopular(10);
        List<BlogPost> recentPosts = blogPostDAO.findRecent(10);
        List<BlogPost> featuredPosts = blogPostDAO.findFeatured(5);

        model.addAttribute("popularPosts", popularPosts);
        model.addAttribute("recentPosts", recentPosts);
        model.addAttribute("featuredPosts", featuredPosts);
        model.addAttribute("totalPosts", blogPostDAO.count());
        model.addAttribute("publishedPosts", blogPostDAO.countPublished());

        return "admin/blog-stats";
    }
}
