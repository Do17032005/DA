package com.clothes.service;

import com.clothes.dao.OrderDAO;
import com.clothes.dao.ProductDAO;
import com.clothes.dao.UserDAO;
import com.clothes.model.Order;
import com.clothes.model.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for admin dashboard statistics and data
 */
@Service
public class AdminDashboardService {

    private final OrderDAO orderDAO;
    private final UserDAO userDAO;
    private final ProductDAO productDAO;

    public AdminDashboardService(OrderDAO orderDAO, UserDAO userDAO, ProductDAO productDAO) {
        this.orderDAO = orderDAO;
        this.userDAO = userDAO;
        this.productDAO = productDAO;
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // Calculate current month stats
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        stats.put("totalRevenue", calculateMonthlyRevenue());
        stats.put("totalOrders", countMonthlyOrders());
        stats.put("totalUsers", userDAO.findAll().size());
        stats.put("totalProducts", productDAO.findAll().size());

        return stats;
    }

    public BigDecimal calculateMonthlyRevenue() {
        List<Order> orders = orderDAO.findAll();
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        return orders.stream()
                .filter(o -> o.getOrderDate() != null && o.getOrderDate().isAfter(monthStart))
                .filter(o -> o.getStatus() == Order.OrderStatus.COMPLETED)
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int countMonthlyOrders() {
        List<Order> orders = orderDAO.findAll();
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        return (int) orders.stream()
                .filter(o -> o.getOrderDate() != null && o.getOrderDate().isAfter(monthStart))
                .count();
    }

    public List<Order> getRecentOrders(int limit) {
        return orderDAO.findAll().stream()
                .sorted((a, b) -> b.getOrderDate().compareTo(a.getOrderDate()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Product> getTopProducts(int limit) {
        List<Product> topProducts = productDAO.findAll().stream()
                .sorted((a, b) -> {
                    Integer countA = a.getPurchaseCount() != null ? a.getPurchaseCount() : 0;
                    Integer countB = b.getPurchaseCount() != null ? b.getPurchaseCount() : 0;
                    return countB.compareTo(countA);
                })
                .limit(limit)
                .collect(Collectors.toList());

        // Calculate total for percentage
        int totalSales = topProducts.stream()
                .mapToInt(p -> p.getPurchaseCount() != null ? p.getPurchaseCount() : 0)
                .sum();

        if (totalSales == 0)
            totalSales = 1; // Avoid divide by zero

        for (Product p : topProducts) {
            int sold = p.getPurchaseCount() != null ? p.getPurchaseCount() : 0;
            p.setSoldCount(sold);
            p.setPercentage((sold * 100) / totalSales);
        }

        return topProducts;
    }

    public List<Product> getLowStockProducts(int threshold) {
        return productDAO.findAll().stream()
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() <= threshold)
                .sorted((a, b) -> a.getStockQuantity().compareTo(b.getStockQuantity()))
                .collect(Collectors.toList());
    }

    public Map<String, Long> getOrderStatusCounts() {
        List<Order> orders = orderDAO.findAll();
        Map<String, Long> counts = new HashMap<>();

        counts.put("pending", orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.PENDING).count());
        counts.put("processing", orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.PROCESSING).count());
        counts.put("shipping", orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.SHIPPING).count());
        counts.put("completed", orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.COMPLETED).count());

        return counts;
    }

    public List<BigDecimal> getMonthlyRevenueData() {
        List<BigDecimal> monthlyData = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            LocalDateTime monthStart = LocalDateTime.now().minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1);

            BigDecimal revenue = orderDAO.findAll().stream()
                    .filter(o -> o.getOrderDate() != null)
                    .filter(o -> o.getOrderDate().isAfter(monthStart) && o.getOrderDate().isBefore(monthEnd))
                    .filter(o -> o.getStatus() == Order.OrderStatus.COMPLETED)
                    .map(Order::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            monthlyData.add(revenue);
        }
        return monthlyData;
    }
}
