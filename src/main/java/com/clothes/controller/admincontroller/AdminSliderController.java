package com.clothes.controller.admincontroller;

import com.clothes.dao.SliderDAO;
import com.clothes.model.Slider;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Admin Controller for Slider Management
 */
@Controller
@RequestMapping("/admin/sliders")
public class AdminSliderController {

    private final SliderDAO sliderDAO;

    public AdminSliderController(SliderDAO sliderDAO) {
        this.sliderDAO = sliderDAO;
    }

    /**
     * Check if user is admin
     */
    private boolean isAdmin(HttpSession session) {
        String role = (String) session.getAttribute("role");
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }

    /**
     * List all sliders
     */
    @GetMapping
    public String listSliders(
            @RequestParam(required = false) Boolean active,
            HttpSession session,
            Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        List<Slider> sliders;

        if (active != null && active) {
            sliders = sliderDAO.findAllActive();
        } else {
            sliders = sliderDAO.findAll();
        }

        model.addAttribute("sliders", sliders);
        model.addAttribute("totalSliders", sliderDAO.count());
        model.addAttribute("activeSliders", sliderDAO.countActive());
        model.addAttribute("selectedActive", active);

        return "admin/sliders";
    }

    /**
     * Show create slider form
     */
    @GetMapping("/create")
    public String showCreateForm(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        model.addAttribute("slider", new Slider());
        return "admin/slider-form";
    }

    /**
     * Create new slider
     */
    @PostMapping("/create")
    public String createSlider(
            @RequestParam String title,
            @RequestParam(required = false) String subtitle,
            @RequestParam(required = false) String description,
            @RequestParam String imageUrl,
            @RequestParam(required = false) String buttonText,
            @RequestParam(required = false) String buttonLink,
            @RequestParam(defaultValue = "0") Integer displayOrder,
            @RequestParam(defaultValue = "true") Boolean isActive,
            @RequestParam(required = false) String backgroundColor,
            @RequestParam(required = false) String textColor,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            Slider slider = new Slider();
            slider.setTitle(title);
            slider.setSubtitle(subtitle);
            slider.setDescription(description);
            slider.setImageUrl(imageUrl);
            slider.setButtonText(buttonText);
            slider.setButtonLink(buttonLink);
            slider.setDisplayOrder(displayOrder);
            slider.setIsActive(isActive);
            slider.setBackgroundColor(backgroundColor);
            slider.setTextColor(textColor);

            Long sliderId = sliderDAO.save(slider);

            redirectAttributes.addFlashAttribute("success",
                    "Slider đã được tạo thành công! ID: " + sliderId);
            return "redirect:/admin/sliders";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi khi tạo slider: " + e.getMessage());
            return "redirect:/admin/sliders/create";
        }
    }

    /**
     * Show edit slider form
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

        Optional<Slider> sliderOpt = sliderDAO.findById(id);
        if (sliderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy slider!");
            return "redirect:/admin/sliders";
        }

        model.addAttribute("slider", sliderOpt.get());
        return "admin/slider-form";
    }

    /**
     * Update slider
     */
    @PostMapping("/edit/{id}")
    public String updateSlider(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String subtitle,
            @RequestParam(required = false) String description,
            @RequestParam String imageUrl,
            @RequestParam(required = false) String buttonText,
            @RequestParam(required = false) String buttonLink,
            @RequestParam(defaultValue = "0") Integer displayOrder,
            @RequestParam(defaultValue = "true") Boolean isActive,
            @RequestParam(required = false) String backgroundColor,
            @RequestParam(required = false) String textColor,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            Optional<Slider> sliderOpt = sliderDAO.findById(id);
            if (sliderOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy slider!");
                return "redirect:/admin/sliders";
            }

            Slider slider = sliderOpt.get();
            slider.setTitle(title);
            slider.setSubtitle(subtitle);
            slider.setDescription(description);
            slider.setImageUrl(imageUrl);
            slider.setButtonText(buttonText);
            slider.setButtonLink(buttonLink);
            slider.setDisplayOrder(displayOrder);
            slider.setIsActive(isActive);
            slider.setBackgroundColor(backgroundColor);
            slider.setTextColor(textColor);

            sliderDAO.update(slider);

            redirectAttributes.addFlashAttribute("success", "Slider đã được cập nhật!");
            return "redirect:/admin/sliders";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi khi cập nhật slider: " + e.getMessage());
            return "redirect:/admin/sliders/edit/" + id;
        }
    }

    /**
     * Toggle slider status (active/inactive)
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
            Optional<Slider> sliderOpt = sliderDAO.findById(id);
            if (sliderOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy slider!");
                return "redirect:/admin/sliders";
            }

            Slider slider = sliderOpt.get();
            sliderDAO.updateStatus(id, !slider.getIsActive());

            String status = slider.getIsActive() ? "ẩn" : "hiển thị";
            redirectAttributes.addFlashAttribute("success",
                    "Slider đã được " + status + "!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi khi thay đổi trạng thái: " + e.getMessage());
        }

        return "redirect:/admin/sliders";
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
            sliderDAO.updateDisplayOrder(id, displayOrder);
            redirectAttributes.addFlashAttribute("success", "Thứ tự hiển thị đã được cập nhật!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi khi cập nhật thứ tự: " + e.getMessage());
        }

        return "redirect:/admin/sliders";
    }

    /**
     * Delete slider
     */
    @PostMapping("/delete/{id}")
    public String deleteSlider(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            Optional<Slider> sliderOpt = sliderDAO.findById(id);
            if (sliderOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy slider!");
                return "redirect:/admin/sliders";
            }

            sliderDAO.delete(id);
            redirectAttributes.addFlashAttribute("success", "Slider đã được xóa!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi khi xóa slider: " + e.getMessage());
        }

        return "redirect:/admin/sliders";
    }
}
