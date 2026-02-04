package com.clothes.controller;

import com.clothes.model.Address;
import com.clothes.service.AddressService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;

/**
 * Controller for address management
 */
@Controller
@RequestMapping("/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    /**
     * Show addresses page
     */
    @GetMapping
    public String showAddresses(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/user/login";
        }

        List<Address> addresses = addressService.getAddressesByUserId(userId);

        model.addAttribute("addresses", addresses);

        return "address";
    }

    /**
     * Add new address
     */
    @PostMapping("/add")
    public String addAddress(@RequestParam String recipientName,
            @RequestParam String phoneNumber,
            @RequestParam String addressLine,
            @RequestParam String ward,
            @RequestParam String district,
            @RequestParam String city,
            @RequestParam(required = false) Boolean isDefault,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        try {
            addressService.createAddress(userId, recipientName, phoneNumber,
                    addressLine, ward, district, city, isDefault);

            redirectAttributes.addFlashAttribute("success", "Đã thêm địa chỉ mới");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/addresses";
    }

    /**
     * Update address
     */
    @PostMapping("/update/{id}")
    public String updateAddress(@PathVariable Long id,
            @RequestParam String recipientName,
            @RequestParam String phoneNumber,
            @RequestParam String addressLine,
            @RequestParam String ward,
            @RequestParam String district,
            @RequestParam String city,
            @RequestParam(required = false) Boolean isDefault,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        try {
            boolean updated = addressService.updateAddress(id, userId, recipientName,
                    phoneNumber, addressLine, ward, district, city, isDefault);

            if (updated) {
                redirectAttributes.addFlashAttribute("success", "Đã cập nhật địa chỉ");
            } else {
                redirectAttributes.addFlashAttribute("error", "Cập nhật thất bại");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/addresses";
    }

    /**
     * Set address as default
     */
    @PostMapping("/set-default/{id}")
    public String setDefaultAddress(@PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        try {
            boolean updated = addressService.setAsDefault(id, userId);

            if (updated) {
                redirectAttributes.addFlashAttribute("success", "Đã đặt làm địa chỉ mặc định");
            } else {
                redirectAttributes.addFlashAttribute("error", "Cập nhật thất bại");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/addresses";
    }

    /**
     * Delete address
     */
    @PostMapping("/delete/{id}")
    public String deleteAddress(@PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        try {
            boolean deleted = addressService.deleteAddress(id, userId);

            if (deleted) {
                redirectAttributes.addFlashAttribute("success", "Đã xóa địa chỉ");
            } else {
                redirectAttributes.addFlashAttribute("error", "Xóa thất bại");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/addresses";
    }

    /**
     * Get districts by province (API)
     * Hardcoded data for demo since we don't have a full database of locations
     */
    @GetMapping("/api/districts")
    @ResponseBody
    public List<String> getDistricts(@RequestParam String province) {
        if ("Hà Nội".equals(province)) {
            return List.of("Ba Đình", "Hoàn Kiếm", "Tây Hồ", "Long Biên", "Cầu Giấy", "Đống Đa", "Hai Bà Trưng",
                    "Hoàng Mai", "Thanh Xuân", "Hà Đông");
        } else if ("TP.HCM".equals(province)) {
            return List.of("Quận 1", "Quận 3", "Quận 4", "Quận 5", "Quận 6", "Quận 7", "Quận 8", "Quận 10", "Quận 11",
                    "Quận 12", "Bình Thạnh", "Gò Vấp");
        } else if ("Đà Nẵng".equals(province)) {
            return List.of("Hải Châu", "Thanh Khê", "Sơn Trà", "Ngũ Hành Sơn", "Liên Chiểu", "Cẩm Lệ");
        } else {
            return List.of("Quận/Huyện 1", "Quận/Huyện 2");
        }
    }

    /**
     * Get wards by district (API)
     */
    @GetMapping("/api/wards")
    @ResponseBody
    public List<String> getWards(@RequestParam String district) {
        return List.of("Phường 1", "Phường 2", "Phường 3", "Phường 4", "Xã 1", "Xã 2");
    }
}
