package com.clothes.controller;

import com.clothes.dao.BlogPostDAO;
import com.clothes.model.BlogPost;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;

/**
 * Public Controller for Blog
 */
@Controller
@RequestMapping("/blog")
public class BlogController {

    private final BlogPostDAO blogPostDAO;

    public BlogController(BlogPostDAO blogPostDAO) {
        this.blogPostDAO = blogPostDAO;
    }

    /**
     * Show blog list page
     */
    @GetMapping
    public String showBlogList(Model model) {
        List<BlogPost> posts = blogPostDAO.findAllPublished();
        model.addAttribute("posts", posts);
        model.addAttribute("recentPosts", blogPostDAO.findRecent(5));
        model.addAttribute("pageTitle", "Tin tức thời trang");
        return "blog";
    }

    /**
     * Show blog detail page
     */
    @GetMapping("/{slug}")
    public String showBlogDetail(@PathVariable String slug, Model model) {
        Optional<BlogPost> postOpt = blogPostDAO.findBySlug(slug);

        if (postOpt.isEmpty()) {
            return "redirect:/blog";
        }

        BlogPost post = postOpt.get();
        blogPostDAO.incrementViewCount(post.getPostId());

        model.addAttribute("post", post);
        model.addAttribute("recentPosts", blogPostDAO.findRecent(5));
        return "blog-detail";
    }
}
