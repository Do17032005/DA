package com.clothes.controller;

import com.clothes.dao.VoucherDAO;
import com.clothes.dao.UserVoucherDAO;
import com.clothes.model.Voucher;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;
import java.util.List;

/**
 * Controller for public voucher collection page
 */
@Controller
@RequestMapping("/vouchers")
public class VoucherController {

    private final VoucherDAO voucherDAO;
    private final UserVoucherDAO userVoucherDAO;

    public VoucherController(VoucherDAO voucherDAO, UserVoucherDAO userVoucherDAO) {
        this.voucherDAO = voucherDAO;
        this.userVoucherDAO = userVoucherDAO;
    }

    /**
     * Show public voucher collection page
     */
    @GetMapping
    public String showVoucherList(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        List<Voucher> availableVouchers;

        if (userId != null) {
            // Filter out vouchers already collected by user
            availableVouchers = voucherDAO.findAvailableVouchersForUser(userId);
        } else {
            // Show all active valid vouchers for guest
            availableVouchers = voucherDAO.findValidVouchers();
        }

        model.addAttribute("vouchers", availableVouchers);
        model.addAttribute("pageTitle", "Săn Voucher");

        return "voucher-list";
    }
}
