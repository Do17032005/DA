package com.clothes.controller;

import com.clothes.dao.AddressDAO;

import com.clothes.model.Address;
import com.clothes.dao.OrderDAO;
import com.clothes.dao.WishlistDAO;
import com.clothes.dao.VoucherDAO;
import com.clothes.model.User;
import com.clothes.model.Order;
import com.clothes.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.io.IOException;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

/**
 * Controller for user authentication and profile management
 */
@Controller
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final AddressDAO addressDAO;
    private final OrderDAO orderDAO;
    private final WishlistDAO wishlistDAO;
    private final VoucherDAO voucherDAO;

    public UserController(UserService userService, AddressDAO addressDAO,
            OrderDAO orderDAO, WishlistDAO wishlistDAO, VoucherDAO voucherDAO) {
        this.userService = userService;
        this.addressDAO = addressDAO;
        this.orderDAO = orderDAO;
        this.wishlistDAO = wishlistDAO;
        this.voucherDAO = voucherDAO;
    }

    /**
     * Show login page
     */
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    /**
     * Handle login
     */
    @PostMapping("/login")
    public String login(@RequestParam String usernameOrEmail,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            Optional<User> userOpt = userService.authenticate(usernameOrEmail, password);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                session.setAttribute("userId", user.getUserId());
                session.setAttribute("username", user.getUsername());
                session.setAttribute("fullName", user.getFullName());

                return "redirect:/";
            } else {
                redirectAttributes.addFlashAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng");
                return "redirect:/user/login";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/login";
        }
    }

    /**
     * Show register page
     */
    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    /**
     * Handle registration
     */
    @PostMapping("/register")
    public String register(@RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String fullName,
            @RequestParam(required = false) String phone,
            RedirectAttributes redirectAttributes) {
        try {
            // Validate password confirmation
            if (!password.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp");
                return "redirect:/user/register";
            }

            // Validate password strength
            if (password.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự");
                return "redirect:/user/register";
            }

            userService.register(username, email, password, fullName, phone);

            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/user/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/register";
        }
    }

    /**
     * Logout
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    /**
     * Show profile page
     */
    @GetMapping("/profile")
    public String showProfilePage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/user/login";
        }

        Optional<User> userOpt = userService.getUserById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            model.addAttribute("user", user);

            // Add counts for profile statistics
            model.addAttribute("orderCount", orderDAO.countByUserId(userId));
            model.addAttribute("wishlistCount", wishlistDAO.countByUserId(userId));
            model.addAttribute("voucherCount", voucherDAO.count()); // Global active vouchers for now

            return "profile";
        }

        return "redirect:/user/login";
    }

    /**
     * Update profile
     */
    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String email,
            @RequestParam String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String dateOfBirth,
            @RequestParam(required = false) MultipartFile avatar,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        try {
            java.time.LocalDate dob = null;
            if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
                dob = java.time.LocalDate.parse(dateOfBirth);
            }

            String avatarUrl = null;
            if (avatar != null && !avatar.isEmpty()) {
                try {
                    String uploadDir = "uploads/avatars/";
                    Path uploadPath = Paths.get(uploadDir);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }

                    String fileName = userId + "_" + System.currentTimeMillis() + "_" + avatar.getOriginalFilename();
                    try (var inputStream = avatar.getInputStream()) {
                        Files.copy(inputStream, uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                        avatarUrl = "/uploads/avatars/" + fileName;
                    }
                } catch (IOException e) {
                    redirectAttributes.addFlashAttribute("error", "Lỗi upload ảnh: " + e.getMessage());
                    return "redirect:/user/profile";
                }
            }

            boolean updated = userService.updateProfile(userId, email, fullName, phone, gender, dob, avatarUrl);

            if (updated) {
                session.setAttribute("fullName", fullName);
                redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công");
            } else {
                redirectAttributes.addFlashAttribute("error", "Cập nhật thất bại");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/user/profile";
    }

    /**
     * Change password
     */
    /**
     * Change password POST
     */
    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        try {
            // Validate password confirmation
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp");
                return "redirect:/user/change-password";
            }

            // Validate password strength
            if (newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự");
                return "redirect:/user/change-password";
            }

            boolean changed = userService.changePassword(userId, oldPassword, newPassword);

            if (changed) {
                redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công");
            } else {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu cũ không chính xác");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/user/change-password";
    }

    /**
     * Change password page
     */
    @GetMapping("/change-password")
    public String showChangePasswordPage(HttpSession session, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/user/login";
        }
        return "change-password";
    }

    /**
     * Vouchers page
     */
    @GetMapping("/vouchers")
    public String showVouchersPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/user/login";
        }

        // For now, show all active vouchers (can be filtered by user eligibility later)
        model.addAttribute("vouchers", voucherDAO.findValidVouchers());

        return "vouchers";
    }

    // ========== QUẢN LÝ ĐƠN HÀNG ==========

    /**
     * Danh sách đơn hàng của người dùng
     */
    @GetMapping("/orders")
    public String listOrders(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/user/login";
        }

        try {
            List<Order> orders = orderDAO.findByUserId(userId);
            model.addAttribute("orders", orders);
            return "orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/user/profile";
        }
    }

    // ========== QUẢN LÝ ĐỊA CHỈ GIAO HÀNG ==========

    /**
     * Danh sách địa chỉ giao hàng (singular alias for /addresses)
     */
    @GetMapping("/address")
    public String showAddress(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        return listAddresses(session, model, redirectAttributes);
    }

    /**
     * Danh sách địa chỉ giao hàng
     */
    @GetMapping("/addresses")
    public String listAddresses(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/user/login";
        }

        try {
            List<Address> addresses = addressDAO.findByUserId(userId);
            model.addAttribute("addresses", addresses);
            model.addAttribute("totalAddresses", addresses.size());
            return "address";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/user/profile";
        }
    }

    /**
     * Thêm địa chỉ mới
     */
    @PostMapping("/addresses/create")
    public String createAddress(@RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam String addressLine,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String district,
            @RequestParam String city,
            @RequestParam(required = false) Boolean isDefault,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        try {
            Address address = new Address();
            address.setUserId(userId);
            address.setRecipientName(fullName);
            address.setPhoneNumber(phone);
            address.setAddressLine(addressLine);
            address.setWard(ward);
            address.setDistrict(district);
            address.setCity(city);
            address.setIsDefault(isDefault != null && isDefault);

            addressDAO.save(address);
            redirectAttributes.addFlashAttribute("success", "Thêm địa chỉ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/user/addresses";
    }

    /**
     * Xóa địa chỉ
     */
    @PostMapping("/addresses/delete/{id}")
    public String deleteAddress(@PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        try {
            // Verify address belongs to user
            var addressOpt = addressDAO.findById(id);
            if (addressOpt.isPresent() && addressOpt.get().getUserId().equals(userId)) {
                addressDAO.delete(id);
                redirectAttributes.addFlashAttribute("success", "Xóa địa chỉ thành công!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy địa chỉ");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/user/addresses";
    }

    /**
     * Đặt địa chỉ mặc định
     */
    @PostMapping("/addresses/set-default/{id}")
    public String setDefaultAddress(@PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        try {
            addressDAO.setAsDefault(id, userId);
            redirectAttributes.addFlashAttribute("success", "Đặt địa chỉ mặc định thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/user/addresses";
    }
}
