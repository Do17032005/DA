package com.clothes.controller.admincontroller;

import com.clothes.dao.UserDAO;
import com.clothes.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

/**
 * Controller for admin authentication
 */
@Controller
@RequestMapping("/admin")
public class AdminLoginController {

    private final UserDAO userDAO;

    public AdminLoginController(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @GetMapping("/login")
    public String showLoginPage(HttpSession session) {
        // If already logged in, redirect to dashboard
        if (session.getAttribute("adminId") != null) {
            return "redirect:/admin/dashboard";
        }
        return "admin/admin-login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) Boolean rememberMe,
            HttpSession session,
            Model model) {

        // Find user by username
        Optional<User> userOpt = userDAO.findByUsername(username);

        if (userOpt.isEmpty()) {
            model.addAttribute("error", "Tên đăng nhập không tồn tại!");
            return "admin/admin-login";
        }

        User user = userOpt.get();

        // Check if user is admin
        if (!"ADMIN".equals(user.getRole())) {
            model.addAttribute("error", "Bạn không có quyền truy cập!");
            return "admin/admin-login";
        }

        // Verify password (in production, use BCrypt)
        if (!password.equals(user.getPassword())) {
            model.addAttribute("error", "Mật khẩu không đúng!");
            return "admin/admin-login";
        }

        // Check if account is active
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            model.addAttribute("error", "Tài khoản đã bị khóa!");
            return "admin/admin-login";
        }

        // Login successful - set session attributes
        session.setAttribute("adminId", user.getUserId());
        session.setAttribute("adminName", user.getFullName() != null ? user.getFullName() : user.getUsername());
        session.setAttribute("adminEmail", user.getEmail());

        // Set session timeout based on remember me
        if (Boolean.TRUE.equals(rememberMe)) {
            session.setMaxInactiveInterval(30 * 24 * 60 * 60); // 30 days
        } else {
            session.setMaxInactiveInterval(8 * 60 * 60); // 8 hours
        }

        return "redirect:/admin/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, Model model) {
        session.invalidate();
        model.addAttribute("message", "Đăng xuất thành công!");
        return "redirect:/admin/login";
    }

    @GetMapping("/register")
    public String showRegisterPage() {
        return "admin/admin-register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String fullName,
            @RequestParam(required = false) String phone,
            Model model) {

        // Check if username already exists
        if (userDAO.existsByUsername(username)) {
            model.addAttribute("error", "Tên đăng nhập đã tồn tại!");
            return "admin/admin-register";
        }

        // Check if email already exists
        if (userDAO.existsByEmail(email)) {
            model.addAttribute("error", "Email đã tồn tại!");
            return "admin/admin-register";
        }

        // Create new admin user
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password); // In production, hash this
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setRole("ADMIN");
        user.setIsActive(true);

        try {
            userDAO.save(user);
            model.addAttribute("success", "Đăng ký tài khoản Admin thành công! Vui lòng đăng nhập.");
            return "admin/admin-login";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi đăng ký: " + e.getMessage());
            return "admin/admin-register";
        }
    }
}
