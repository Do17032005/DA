package com.clothes.service;

import com.clothes.dao.VoucherDAO;
import com.clothes.model.Voucher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for Voucher management
 */
@Service
public class VoucherService {

    private final VoucherDAO voucherDAO;

    public VoucherService(VoucherDAO voucherDAO) {
        this.voucherDAO = voucherDAO;
    }

    /**
     * Create a new voucher
     */
    public Long createVoucher(String code, String description, Voucher.DiscountType discountType,
            BigDecimal discountValue, BigDecimal minOrderAmount,
            BigDecimal maxDiscountAmount, Integer usageLimit,
            LocalDateTime validFrom, LocalDateTime validUntil) {
        // Check if code already exists
        Optional<Voucher> existingVoucher = voucherDAO.findByCode(code);
        if (existingVoucher.isPresent()) {
            throw new IllegalArgumentException("Mã voucher đã tồn tại");
        }

        Voucher voucher = new Voucher();
        voucher.setVoucherCode(code);
        voucher.setDescription(description);
        voucher.setDiscountType(discountType);
        voucher.setDiscountValue(discountValue);
        voucher.setMinOrderValue(minOrderAmount);
        voucher.setMaxDiscount(maxDiscountAmount);
        voucher.setQuantity(usageLimit);
        voucher.setUsedCount(0);
        voucher.setStartDate(validFrom != null ? validFrom.toLocalDate() : null);
        voucher.setEndDate(validUntil != null ? validUntil.toLocalDate() : null);
        voucher.setIsActive(true);

        return voucherDAO.save(voucher);
    }

    /**
     * Update voucher
     */
    public boolean updateVoucher(Long voucherId, String code, String description,
            Voucher.DiscountType discountType, BigDecimal discountValue,
            BigDecimal minOrderAmount, BigDecimal maxDiscountAmount,
            Integer usageLimit, LocalDateTime validFrom,
            LocalDateTime validUntil, Boolean isActive) {
        Optional<Voucher> voucherOpt = voucherDAO.findById(voucherId);
        if (voucherOpt.isEmpty()) {
            return false;
        }

        Voucher voucher = voucherOpt.get();

        // Check if code is being changed and if new code already exists
        if (!voucher.getVoucherCode().equals(code)) {
            Optional<Voucher> existingVoucher = voucherDAO.findByCode(code);
            if (existingVoucher.isPresent()) {
                throw new IllegalArgumentException("Mã voucher đã tồn tại");
            }
        }

        voucher.setVoucherCode(code);
        voucher.setDescription(description);
        voucher.setDiscountType(discountType);
        voucher.setDiscountValue(discountValue);
        voucher.setMinOrderValue(minOrderAmount);
        voucher.setMaxDiscount(maxDiscountAmount);
        voucher.setQuantity(usageLimit);
        voucher.setStartDate(validFrom != null ? validFrom.toLocalDate() : null);
        voucher.setEndDate(validUntil != null ? validUntil.toLocalDate() : null);
        if (isActive != null) {
            voucher.setIsActive(isActive);
        }

        return voucherDAO.update(voucher) > 0;
    }

    /**
     * Get voucher by ID
     */
    public Optional<Voucher> getVoucherById(Long voucherId) {
        return voucherDAO.findById(voucherId);
    }

    /**
     * Get voucher by code
     */
    public Optional<Voucher> getVoucherByCode(String code) {
        return voucherDAO.findByCode(code);
    }

    /**
     * Get all vouchers
     */
    public List<Voucher> getAllVouchers() {
        return voucherDAO.findAll();
    }

    /**
     * Get all active vouchers
     */
    public List<Voucher> getAllActiveVouchers() {
        return voucherDAO.findAllActive();
    }

    /**
     * Get all valid vouchers (active, within date range, not exceeded usage limit)
     */
    public List<Voucher> getValidVouchers() {
        return voucherDAO.findValidVouchers();
    }

    /**
     * Apply voucher to order
     */
    public BigDecimal applyVoucher(String code, BigDecimal orderAmount) {
        Optional<Voucher> voucherOpt = voucherDAO.findByCode(code);
        if (voucherOpt.isEmpty()) {
            throw new IllegalArgumentException("Voucher không tồn tại");
        }

        Voucher voucher = voucherOpt.get();

        // Validate voucher
        if (!voucher.isValid()) {
            throw new IllegalArgumentException("Voucher không hợp lệ hoặc đã hết hạn");
        }

        // Check minimum order amount
        if (voucher.getMinOrderValue() != null && orderAmount.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng voucher");
        }

        // Calculate discount
        BigDecimal discount = voucher.calculateDiscount(orderAmount);

        // Increment usage count
        voucherDAO.incrementUsedCount(voucher.getVoucherId());

        return discount;
    }

    /**
     * Validate voucher without applying
     */
    public boolean validateVoucher(String code, BigDecimal orderAmount) {
        Optional<Voucher> voucherOpt = voucherDAO.findByCode(code);
        if (voucherOpt.isEmpty()) {
            return false;
        }

        Voucher voucher = voucherOpt.get();
        if (!voucher.isValid()) {
            return false;
        }

        // Check minimum order amount
        return voucher.getMinOrderValue() == null ||
                orderAmount.compareTo(voucher.getMinOrderValue()) >= 0;
    }

    /**
     * Calculate discount amount without applying
     */
    public BigDecimal calculateDiscount(String code, BigDecimal orderAmount) {
        Optional<Voucher> voucherOpt = voucherDAO.findByCode(code);
        if (voucherOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Voucher voucher = voucherOpt.get();
        if (!voucher.isValid()) {
            return BigDecimal.ZERO;
        }

        // Check minimum order amount
        if (voucher.getMinOrderValue() != null && orderAmount.compareTo(voucher.getMinOrderValue()) < 0) {
            return BigDecimal.ZERO;
        }

        return voucher.calculateDiscount(orderAmount);
    }

    /**
     * Deactivate voucher
     */
    public boolean deactivateVoucher(Long voucherId) {
        return voucherDAO.deactivate(voucherId) > 0;
    }

    /**
     * Delete voucher
     */
    public boolean deleteVoucher(Long voucherId) {
        return voucherDAO.delete(voucherId) > 0;
    }

    /**
     * Get total voucher count
     */
    public int getTotalVouchers() {
        return voucherDAO.count();
    }

    /**
     * Save or update voucher for admin
     */
    public Voucher saveVoucher(Voucher voucher) {
        if (voucher.getVoucherId() == null) {
            // Check if code already exists
            Optional<Voucher> existing = voucherDAO.findByCode(voucher.getVoucherCode());
            if (existing.isPresent()) {
                throw new IllegalArgumentException("Mã voucher đã tồn tại");
            }
            Long id = voucherDAO.save(voucher);
            voucher.setVoucherId(id);
        } else {
            voucherDAO.update(voucher);
        }
        return voucher;
    }

    /**
     * Toggle voucher active status
     */
    public void toggleVoucherStatus(Long voucherId) {
        Optional<Voucher> voucher = voucherDAO.findById(voucherId);
        voucher.ifPresent(v -> {
            v.setIsActive(!Boolean.TRUE.equals(v.getIsActive()));
            voucherDAO.update(v);
        });
    }
}
