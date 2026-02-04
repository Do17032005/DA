package com.clothes.controller;

import com.clothes.dao.UserDAO;
import com.clothes.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

/**
 * Controller for user authentication (login/register)
 */
import com.clothes.service.CartService; // Import CartService

@Controller
public class UserAuthController {

    private final UserDAO userDAO;
    private final CartService cartService;

    public UserAuthController(UserDAO userDAO, CartService cartService) {
        this.userDAO = userDAO;
        this.cartService = cartService;
    }

    // ===========================
    // LOGIN
    // ===========================

    @GetMapping("/login")
    public String showLoginPage(HttpSession session) {
        // If already logged in, redirect to home
        if (session.getAttribute("userId") != null) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) Boolean rememberMe,
            HttpSession session,
            Model model) {

        // Find user by username or email
        Optional<User> userOpt = userDAO.findByUsername(username);

        if (userOpt.isEmpty()) {
            // Try finding by email
            userOpt = userDAO.findByEmail(username);
        }

        if (userOpt.isEmpty()) {
            model.addAttribute("error", "Tên đăng nhập hoặc email không tồn tại!");
            return "login";
        }

        User user = userOpt.get();

        // Check if user is active
        if (!user.getIsActive()) {
            model.addAttribute("error", "Tài khoản của bạn đã bị vô hiệu hóa!");
            return "login";
        }

        // Check password (plain text for now - should use BCrypt in production)
        if (!password.equals(user.getPassword())) {
            model.addAttribute("error", "Mật khẩu không chính xác!");
            return "login";
        }

        // Login successful - store user info in session
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("fullName", user.getFullName());
        session.setAttribute("email", user.getEmail());
        session.setAttribute("isVip", user.getIsVip());

        // Merge session cart with persistent cart
        cartService.mergeCart(user.getUserId());

        // Set session timeout (30 minutes)
        session.setMaxInactiveInterval(1800);

        // Redirect to home or previous page
        String redirectUrl = (String) session.getAttribute("redirectAfterLogin");
        if (redirectUrl != null) {
            session.removeAttribute("redirectAfterLogin");
            return "redirect:" + redirectUrl;
        }

        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // ===========================
    // REGISTER
    // ===========================

    @GetMapping("/register")
    public String showRegisterPage(HttpSession session) {
        // If already logged in, redirect to home
        if (session.getAttribute("userId") != null) {
            return "redirect:/";
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String fullName,
            @RequestParam(required = false) String phone,
            HttpSession session,
            Model model) {

        // Validate password match
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp!");
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("phone", phone);
            return "register";
        }

        // Check if username already exists
        if (userDAO.findByUsername(username).isPresent()) {
            model.addAttribute("error", "Tên đăng nhập đã tồn tại!");
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("phone", phone);
            return "register";
        }

        // Check if email already exists
        if (userDAO.findByEmail(email).isPresent()) {
            model.addAttribute("error", "Email đã được đăng ký!");
            model.addAttribute("username", username);
            model.addAttribute("fullName", fullName);
            model.addAttribute("phone", phone);
            return "register";
        }

        // Create new user
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(password); // Should hash with BCrypt in production
        newUser.setFullName(fullName);
        newUser.setPhone(phone);
        newUser.setRole("USER");
        newUser.setIsActive(true);
        newUser.setIsVip(false);

        try {
            userDAO.save(newUser);

            // Auto-login after registration
            User savedUser = userDAO.findByUsername(username).get();
            session.setAttribute("userId", savedUser.getUserId());
            session.setAttribute("username", savedUser.getUsername());
            session.setAttribute("fullName", savedUser.getFullName());
            session.setAttribute("email", savedUser.getEmail());
            session.setAttribute("isVip", savedUser.getIsVip());

            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", "Đăng ký thất bại! Vui lòng thử lại.");
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("phone", phone);
            return "register";
        }
    }
}
