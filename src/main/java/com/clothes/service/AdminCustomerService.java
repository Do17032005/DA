package com.clothes.service;

import com.clothes.dao.OrderDAO;
import com.clothes.dao.UserDAO;
import com.clothes.model.Order;
import com.clothes.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for admin customer management
 */
@Service
@Transactional
public class AdminCustomerService {

    private final UserDAO userDAO;
    private final OrderDAO orderDAO;

    public AdminCustomerService(UserDAO userDAO, OrderDAO orderDAO) {
        this.userDAO = userDAO;
        this.orderDAO = orderDAO;
    }

    public List<User> getAllCustomers() {
        List<User> customers = userDAO.findAll().stream()
                .filter(u -> !"ADMIN".equals(u.getRole()))
                .collect(Collectors.toList());
        enrichCustomersWithOrderData(customers);
        return customers;
    }

    public Optional<User> getCustomerById(Long id) {
        Optional<User> customer = userDAO.findById(id);
        customer.ifPresent(c -> {
            Map<String, Object> orderData = getCustomerOrderData(c.getUserId());
            c.setOrderCount((Integer) orderData.get("orderCount"));
            c.setTotalSpent((BigDecimal) orderData.get("totalSpent"));
        });
        return customer;
    }

    public void toggleCustomerStatus(Long customerId) {
        Optional<User> customer = userDAO.findById(customerId);
        customer.ifPresent(c -> {
            c.setIsActive(!Boolean.TRUE.equals(c.getIsActive()));
            userDAO.update(c);
        });
    }

    public Map<String, Integer> getCustomerStats() {
        List<User> customers = userDAO.findAll().stream()
                .filter(u -> !"ADMIN".equals(u.getRole()))
                .collect(Collectors.toList());

        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);

        return Map.of(
                "total", customers.size(),
                "active", (int) customers.stream().filter(c -> Boolean.TRUE.equals(c.getIsActive())).count(),
                "newThisMonth", (int) customers.stream()
                        .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(monthStart))
                        .count(),
                "blocked", (int) customers.stream().filter(c -> !Boolean.TRUE.equals(c.getIsActive())).count());
    }

    private void enrichCustomersWithOrderData(List<User> customers) {
        customers.forEach(customer -> {
            Map<String, Object> orderData = getCustomerOrderData(customer.getUserId());
            customer.setOrderCount((Integer) orderData.get("orderCount"));
            customer.setTotalSpent((BigDecimal) orderData.get("totalSpent"));
        });
    }

    private Map<String, Object> getCustomerOrderData(Long userId) {
        List<Order> orders = orderDAO.findAll().stream()
                .filter(o -> userId.equals(o.getUserId()))
                .collect(Collectors.toList());

        BigDecimal totalSpent = orders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.COMPLETED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> data = new HashMap<>();
        data.put("orderCount", orders.size());
        data.put("totalSpent", totalSpent);

        return data;
    }
}
