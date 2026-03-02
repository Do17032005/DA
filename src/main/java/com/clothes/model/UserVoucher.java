package com.clothes.model;

import java.time.LocalDateTime;

/**
 * Model class representing a voucher collected by a user
 */
public class UserVoucher {
    private Long userVoucherId;
    private Long userId;
    private Long voucherId;
    private LocalDateTime collectedAt;
    private Boolean isUsed;
    private LocalDateTime usedAt;

    // Associated voucher data (optional, for convenience)
    private Voucher voucher;

    public UserVoucher() {
        this.isUsed = false;
        this.collectedAt = LocalDateTime.now();
    }

    public Long getUserVoucherId() {
        return userVoucherId;
    }

    public void setUserVoucherId(Long userVoucherId) {
        this.userVoucherId = userVoucherId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }

    public Boolean getIsUsed() {
        return isUsed;
    }

    public void setIsUsed(Boolean used) {
        isUsed = used;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public Voucher getVoucher() {
        return voucher;
    }

    public void setVoucher(Voucher voucher) {
        this.voucher = voucher;
    }
}
