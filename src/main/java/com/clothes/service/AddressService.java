package com.clothes.service;

import com.clothes.dao.AddressDAO;
import com.clothes.model.Address;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for Address management
 */
@Service
public class AddressService {

    private final AddressDAO addressDAO;

    public AddressService(AddressDAO addressDAO) {
        this.addressDAO = addressDAO;
    }

    /**
     * Create a new address
     */
    public Long createAddress(Long userId, String recipientName, String phoneNumber,
            String addressLine, String ward, String district,
            String city, Boolean isDefault) {
        Address address = new Address();
        address.setUserId(userId);
        address.setRecipientName(recipientName);
        address.setPhoneNumber(phoneNumber);
        address.setAddressLine(addressLine);
        address.setWard(ward);
        address.setDistrict(district);
        address.setCity(city);
        address.setIsDefault(isDefault != null ? isDefault : false);

        return addressDAO.save(address);
    }

    /**
     * Update address
     */
    public boolean updateAddress(Long addressId, Long userId, String recipientName,
            String phoneNumber, String addressLine, String ward,
            String district, String city, Boolean isDefault) {
        Optional<Address> addressOpt = addressDAO.findById(addressId);
        if (addressOpt.isEmpty()) {
            return false;
        }

        Address address = addressOpt.get();

        // Check if user owns this address
        if (!address.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền chỉnh sửa địa chỉ này");
        }

        address.setRecipientName(recipientName);
        address.setPhoneNumber(phoneNumber);
        address.setAddressLine(addressLine);
        address.setWard(ward);
        address.setDistrict(district);
        address.setCity(city);
        if (isDefault != null) {
            address.setIsDefault(isDefault);
        }

        return addressDAO.update(address) > 0;
    }

    /**
     * Get address by ID
     */
    public Optional<Address> getAddressById(Long addressId) {
        return addressDAO.findById(addressId);
    }

    /**
     * Get all addresses for user
     */
    public List<Address> getAddressesByUserId(Long userId) {
        return addressDAO.findByUserId(userId);
    }

    /**
     * Get default address for user
     */
    public Optional<Address> getDefaultAddress(Long userId) {
        return addressDAO.findDefaultByUserId(userId);
    }

    /**
     * Set address as default
     */
    public boolean setAsDefault(Long addressId, Long userId) {
        Optional<Address> addressOpt = addressDAO.findById(addressId);
        if (addressOpt.isEmpty()) {
            return false;
        }

        Address address = addressOpt.get();

        // Check if user owns this address
        if (!address.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền thiết lập địa chỉ này");
        }

        return addressDAO.setAsDefault(addressId, userId) > 0;
    }

    /**
     * Delete address
     */
    public boolean deleteAddress(Long addressId, Long userId) {
        Optional<Address> addressOpt = addressDAO.findById(addressId);
        if (addressOpt.isEmpty()) {
            return false;
        }

        Address address = addressOpt.get();

        // Check if user owns this address
        if (!address.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền xóa địa chỉ này");
        }

        return addressDAO.delete(addressId) > 0;
    }

    /**
     * Get address count for user
     */
    public int getAddressCount(Long userId) {
        return addressDAO.countByUserId(userId);
    }
}
