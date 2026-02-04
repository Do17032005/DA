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
}
