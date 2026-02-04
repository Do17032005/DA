package com.clothes.controller.admincontroller;

import com.clothes.dao.BannerDAO;
import com.clothes.model.Banner;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Admin Controller for Banner Management
 */
@Controller
@RequestMapping("/admin/banners")
public class AdminBannerController {

    private final BannerDAO bannerDAO;

    public AdminBannerController(BannerDAO bannerDAO) {
        this.bannerDAO = bannerDAO;
    }

    /**
     * Check if user is admin
     */
    private boolean isAdmin(HttpSession session) {
        String role = (String) session.getAttribute("role");
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }

    /**
     * List all banners with filter
     */
    @GetMapping
    public String listBanners(
            @RequestParam(required = false) String position,
            @RequestParam(required = false) Boolean active,
            HttpSession session,
            Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        List<Banner> banners;

        if (position != null && !position.isEmpty()) {
            banners = bannerDAO.findByPosition(position);
        } else if (active != null && active) {
            banners = bannerDAO.findAllActive();
        } else {
            banners = bannerDAO.findAll();
        }

        model.addAttribute("banners", banners);
        model.addAttribute("totalBanners", bannerDAO.count());
        model.addAttribute("activeBanners", bannerDAO.countActive());
        model.addAttribute("selectedPosition", position);
        model.addAttribute("selectedActive", active);

        return "admin/banners";
    }

    /**
     * Show create banner form
     */
    @GetMapping("/create")
    public String showCreateForm(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        model.addAttribute("banner", new Banner());
        return "admin/banner-form";
    }

    /**
     * Create new banner
     */
    @PostMapping("/create")
    public String createBanner(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String imageUrl,
            @RequestParam(required = false) String linkUrl,
            @RequestParam(defaultValue = "0") Integer displayOrder,
            @RequestParam(defaultValue = "true") Boolean isActive,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "main") String position,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            Banner banner = new Banner();
            banner.setTitle(title);
            banner.setDescription(description);
            banner.setImageUrl(imageUrl);
            banner.setLinkUrl(linkUrl);
            banner.setDisplayOrder(displayOrder);
            banner.setIsActive(isActive);
            banner.setPosition(position);

            // Parse dates if provided
            if (startDate != null && !startDate.isEmpty()) {
                banner.setStartDate(LocalDateTime.parse(startDate));
            }
            if (endDate != null && !endDate.isEmpty()) {
                banner.setEndDate(LocalDateTime.parse(endDate));
            }

            Long bannerId = bannerDAO.save(banner);

            redirectAttributes.addFlashAttribute("success",
                    "Banner đã được tạo thành công! ID: " + bannerId);
            return "redirect:/admin/banners";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi khi tạo banner: " + e.getMessage());
            return "redirect:/admin/banners/create";
        }
    }

    /**
     * Show edit banner form
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

        Optional<Banner> bannerOpt = bannerDAO.findById(id);
        if (bannerOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy banner!");
            return "redirect:/admin/banners";
        }

        model.addAttribute("banner", bannerOpt.get());
        return "admin/banner-form";
    }

    /**
     * Update banner
     */
    @PostMapping("/edit/{id}")
    public String updateBanner(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String imageUrl,
            @RequestParam(required = false) String linkUrl,
            @RequestParam(defaultValue = "0") Integer displayOrder,
            @RequestParam(defaultValue = "true") Boolean isActive,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "main") String position,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            Optional<Banner> bannerOpt = bannerDAO.findById(id);
            if (bannerOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy banner!");
                return "redirect:/admin/banners";
            }

            Banner banner = bannerOpt.get();
            banner.setTitle(title);
            banner.setDescription(description);
            banner.setImageUrl(imageUrl);
            banner.setLinkUrl(linkUrl);
            banner.setDisplayOrder(displayOrder);
            banner.setIsActive(isActive);
            banner.setPosition(position);

            // Parse dates if provided
            if (startDate != null && !startDate.isEmpty()) {
                banner.setStartDate(LocalDateTime.parse(startDate));
            } else {
                banner.setStartDate(null);
            }

            if (endDate != null && !endDate.isEmpty()) {
                banner.setEndDate(LocalDateTime.parse(endDate));
            } else {
                banner.setEndDate(null);
            }

            bannerDAO.update(banner);

            redirectAttributes.addFlashAttribute("success", "Banner đã được cập nhật!");
            return "redirect:/admin/banners";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi khi cập nhật banner: " + e.getMessage());
            return "redirect:/admin/banners/edit/" + id;
        }
    }

    /**
     * Toggle banner status (active/inactive)
     */
    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            Optional<Banner> bannerOpt = bannerDAO.findById(id);
            if (bannerOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy banner!");
                return "redirect:/admin/banners";
            }

            Banner banner = bannerOpt.get();
            bannerDAO.updateStatus(id, !banner.getIsActive());

            String status = banner.getIsActive() ? "ẩn" : "hiển thị";
            redirectAttributes.addFlashAttribute("success",
                    "Banner đã được " + status + "!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi khi thay đổi trạng thái: " + e.getMessage());
        }

        return "redirect:/admin/banners";
    }

    /**
     * Update display order
     */
    @PostMapping("/{id}/reorder")
    public String updateDisplayOrder(
            @PathVariable Long id,
            @RequestParam Integer displayOrder,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            bannerDAO.updateDisplayOrder(id, displayOrder);
            redirectAttributes.addFlashAttribute("success", "Thứ tự hiển thị đã được cập nhật!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi khi cập nhật thứ tự: " + e.getMessage());
        }

        return "redirect:/admin/banners";
    }

    /**
     * Delete banner
     */
    @PostMapping("/delete/{id}")
    public String deleteBanner(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            Optional<Banner> bannerOpt = bannerDAO.findById(id);
            if (bannerOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy banner!");
                return "redirect:/admin/banners";
            }

            bannerDAO.delete(id);
            redirectAttributes.addFlashAttribute("success", "Banner đã được xóa!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi khi xóa banner: " + e.getMessage());
        }

        return "redirect:/admin/banners";
    }
}
