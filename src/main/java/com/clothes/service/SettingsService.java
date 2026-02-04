package com.clothes.service;

import com.clothes.dao.SystemSettingDAO;
import com.clothes.model.SystemSetting;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for system settings management
 */
@Service
public class SettingsService {

    private final SystemSettingDAO settingDAO;
    private Map<String, String> settingsCache;

    public SettingsService(SystemSettingDAO settingDAO) {
        this.settingDAO = settingDAO;
        this.settingsCache = new HashMap<>();
        loadSettingsToCache();
    }

    /**
     * Load all settings into memory cache
     */
    private void loadSettingsToCache() {
        List<SystemSetting> allSettings = settingDAO.findAll();
        settingsCache.clear();
        for (SystemSetting setting : allSettings) {
            settingsCache.put(setting.getSettingKey(), setting.getSettingValue());
        }
    }

    /**
     * Get setting value by key
     */
    public String getSetting(String key) {
        return settingsCache.getOrDefault(key, null);
    }

    /**
     * Get setting with default value
     */
    public String getSetting(String key, String defaultValue) {
        return settingsCache.getOrDefault(key, defaultValue);
    }

    /**
     * Get setting as boolean
     */
    public boolean getSettingAsBoolean(String key, boolean defaultValue) {
        String value = getSetting(key);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value)
                || "on".equalsIgnoreCase(value);
    }

    /**
     * Get setting as boolean (Legacy support)
     */
    public boolean getSettingAsBoolean(String key) {
        return getSettingAsBoolean(key, false);
    }

    /**
     * Get setting as integer
     */
    public int getSettingAsInt(String key, int defaultValue) {
        String value = getSetting(key);
        if (value == null)
            return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get setting as double
     */
    public double getSettingAsDouble(String key, double defaultValue) {
        String value = getSetting(key);
        if (value == null)
            return defaultValue;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Update setting value
     */
    public boolean updateSetting(String key, String value, Long updatedBy) {
        int result = settingDAO.updateValue(key, value, updatedBy);
        if (result > 0) {
            settingsCache.put(key, value);
            return true;
        }
        return false;
    }

    /**
     * Get all settings by category
     */
    public List<SystemSetting> getSettingsByCategory(String category) {
        return settingDAO.findByCategory(category);
    }

    /**
     * Get settings as map by category
     */
    public Map<String, String> getSettingsMapByCategory(String category) {
        List<SystemSetting> settings = settingDAO.findByCategory(category);
        Map<String, String> map = new HashMap<>();
        for (SystemSetting setting : settings) {
            map.put(setting.getSettingKey(), setting.getSettingValue());
        }
        return map;
    }

    /**
     * Create or update setting
     */
    public void saveSetting(String key, String value, String category, Long updatedBy) {
        if (settingDAO.exists(key)) {
            settingDAO.updateValue(key, value, updatedBy);
        } else {
            SystemSetting setting = new SystemSetting(key, value, category);
            setting.setUpdatedBy(updatedBy);
            settingDAO.save(setting);
        }
        settingsCache.put(key, value);
    }

    /**
     * Batch update settings
     */
    public void batchUpdateSettings(Map<String, String> settings, String category, Long updatedBy) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            saveSetting(entry.getKey(), entry.getValue(), category, updatedBy);
        }
    }

    /**
     * Calculate shipping fee based on province
     */
    public double getShippingFee(String province) {
        double defaultFee = getSettingAsDouble("shipping_fee_default", 30000);

        // Check if province has custom fee (stored in JSON or separate settings)
        String customFeeKey = "shipping_fee_" + normalizeProvinceName(province);
        String customFee = getSetting(customFeeKey);

        if (customFee != null) {
            try {
                return Double.parseDouble(customFee);
            } catch (NumberFormatException e) {
                return defaultFee;
            }
        }

        return defaultFee;
    }

    /**
     * Check if free shipping applies
     */
    public boolean isFreeShipping(double orderTotal) {
        double threshold = getSettingAsDouble("free_shipping_threshold", 500000);
        return orderTotal >= threshold;
    }

    /**
     * Check if payment method is enabled
     */
    public boolean isPaymentMethodEnabled(String method) {
        String key = "payment_" + method.toLowerCase() + "_enabled";
        return getSettingAsBoolean(key);
    }

    /**
     * Get site information
     */
    public Map<String, String> getSiteInfo() {
        Map<String, String> siteInfo = new HashMap<>();
        siteInfo.put("siteName", getSetting("site_name", "Clothes Shop"));
        siteInfo.put("siteLogo", getSetting("site_logo", "/images/logo.png"));
        siteInfo.put("contactEmail", getSetting("contact_email", ""));
        siteInfo.put("contactPhone", getSetting("contact_phone", ""));
        siteInfo.put("contactAddress", getSetting("contact_address", ""));
        siteInfo.put("facebookUrl", getSetting("facebook_url", ""));
        siteInfo.put("instagramUrl", getSetting("instagram_url", ""));
        return siteInfo;
    }

    /**
     * Get payment methods configuration
     */
    public Map<String, Boolean> getPaymentMethods() {
        Map<String, Boolean> methods = new HashMap<>();
        methods.put("cod", getSettingAsBoolean("payment_cod_enabled"));
        methods.put("bank", getSettingAsBoolean("payment_bank_enabled"));
        methods.put("momo", getSettingAsBoolean("payment_momo_enabled"));
        methods.put("vnpay", getSettingAsBoolean("payment_vnpay_enabled"));
        return methods;
    }

    /**
     * Reload cache from database
     */
    public void reloadCache() {
        loadSettingsToCache();
    }

    /**
     * Normalize province name for setting key
     */
    private String normalizeProvinceName(String province) {
        if (province == null)
            return "";
        return province.toLowerCase()
                .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                .replaceAll("[ìíịỉĩ]", "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                .replaceAll("[ùúụủũưừứựửữ]", "u")
                .replaceAll("[ỳýỵỷỹ]", "y")
                .replaceAll("[đ]", "d")
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_]", "");
    }

    /**
     * Initialize default settings
     */
    public void initializeDefaults() {
        settingDAO.initializeDefaults();
        reloadCache();
    }

    /**
     * Get setting object by key
     */
    public Optional<SystemSetting> getSettingObject(String key) {
        return settingDAO.findByKey(key);
    }

    /**
     * Get all settings as map for admin display
     */
    public Map<String, String> getAllSettingsMap() {
        return new HashMap<>(settingsCache);
    }

    /**
     * Save multiple settings from admin form
     */
    public void saveAllSettings(Map<String, String> settings, Long adminUserId) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // Determine category from key
            String category = determineCategoryFromKey(key);
            saveSetting(key, value, category, adminUserId);
        }
    }

    private String determineCategoryFromKey(String key) {
        if (key.startsWith("site_") || key.startsWith("contact_"))
            return "general";
        if (key.startsWith("shipping_"))
            return "shipping";
        if (key.startsWith("payment_") || key.startsWith("bank_"))
            return "payment";
        if (key.startsWith("smtp_"))
            return "email";
        if (key.contains("facebook") || key.contains("instagram") || key.contains("youtube"))
            return "social";
        return "general";
    }
}
