package com.clothes.service;

import com.clothes.dao.UserDAO;
import com.clothes.model.User;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for User management and authentication
 */
@Service
public class UserService {

    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Register a new user
     */
    public Long register(String username, String email, String password, String fullName, String phone) {
        // Check if username already exists
        if (userDAO.existsByUsername(username)) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }

        // Check if email already exists
        if (userDAO.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        // Create user
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setIsActive(true);

        return userDAO.save(user);
    }

    /**
     * Authenticate user with username/email and password
     */
    public Optional<User> authenticate(String usernameOrEmail, String password) {
        Optional<User> userOpt;

        // Try to find by username first
        if (usernameOrEmail.contains("@")) {
            userOpt = userDAO.findByEmail(usernameOrEmail);
        } else {
            userOpt = userDAO.findByUsername(usernameOrEmail);
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (password.equals(user.getPassword())) {
                if (!user.getIsActive()) {
                    throw new IllegalArgumentException("Tài khoản đã bị vô hiệu hóa");
                }
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    /**
     * Get user by ID
     */
    public Optional<User> getUserById(Long userId) {
        return userDAO.findById(userId);
    }

    /**
     * Get user by username
     */
    public Optional<User> getUserByUsername(String username) {
        return userDAO.findByUsername(username);
    }

    /**
     * Get user by email
     */
    public Optional<User> getUserByEmail(String email) {
        return userDAO.findByEmail(email);
    }

    /**
     * Update user profile
     */
    public boolean updateProfile(Long userId, String email, String fullName, String phone,
            String gender, java.time.LocalDate dateOfBirth, String avatarUrl) {
        Optional<User> userOpt = userDAO.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        // Check if email is being changed and if it's already taken
        if (!user.getEmail().equals(email) && userDAO.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        user.setEmail(email);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setGender(gender);
        user.setDateOfBirth(dateOfBirth);

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            user.setAvatarUrl(avatarUrl);
        }

        return userDAO.update(user) > 0;
    }

    /**
     * Change user password
     */
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        Optional<User> userOpt = userDAO.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        // Verify old password
        if (!oldPassword.equals(user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu cũ không đúng");
        }

        // Update new password
        userDAO.updatePassword(userId, newPassword);

        return userDAO.updatePassword(userId, newPassword) > 0;
    }

    /**
     * Deactivate user account
     */
    public boolean deactivateUser(Long userId) {
        return userDAO.delete(userId) > 0;
    }

    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    /**
     * Get all active users
     */
    public List<User> getAllActiveUsers() {
        return userDAO.findAllActive();
    }

    /**
     * Get total user count
     */
    public int getTotalUsers() {
        return userDAO.count();
    }
}
