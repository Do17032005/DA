package com.clothes.controller.admincontroller;

import com.clothes.model.User;
import com.clothes.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

/**
 * Controller for admin profile management
 */
@Controller
@RequestMapping("/admin/profile")
public class AdminProfileController {

    private final UserService userService;

    public AdminProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String showProfile(HttpSession session, Model model) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/admin/login";
        }

        Optional<User> adminOpt = userService.getUserById(adminId);
        if (adminOpt.isEmpty()) {
            return "redirect:/admin/login";
        }

        model.addAttribute("adminUser", adminOpt.get());
        return "admin/profile";
    }

    @PostMapping("/update")
    public String updateProfile(@RequestParam String fullName,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/admin/login";
        }

        try {
            // Re-using userService updateProfile (passing null for other fields not in
            // form)
            // Note: UserService.updateProfile requires full fields or we need a specific
            // method
            // Let's use the method we updated earlier:
            // updateProfile(Long userId, String email, String fullName, String phone,
            // String gender, LocalDate dateOfBirth, String avatarUrl)

            // First get current user to preserve other fields
            Optional<User> currentAdminOpt = userService.getUserById(adminId);
            if (currentAdminOpt.isEmpty()) {
                return "redirect:/admin/login";
            }
            User currentAdmin = currentAdminOpt.get();

            boolean updated = userService.updateProfile(
                    adminId,
                    email,
                    fullName,
                    phone,
                    currentAdmin.getGender(),
                    currentAdmin.getDateOfBirth(),
                    currentAdmin.getAvatarUrl());

            if (updated) {
                // Update session info if needed
                session.setAttribute("adminName", fullName);
                session.setAttribute("adminEmail", email);
                redirectAttributes.addFlashAttribute("success", "Cập nhật hồ sơ thành công!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Cập nhật thất bại.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/admin/login";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "redirect:/admin/profile";
        }

        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự!");
            return "redirect:/admin/profile";
        }

        try {
            boolean changed = userService.changePassword(adminId, currentPassword, newPassword);
            if (changed) {
                redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu hiện tại không đúng!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/profile";
    }
}
