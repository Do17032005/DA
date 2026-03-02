package com.clothes.service;

import com.clothes.dao.OrderDAO;
import com.clothes.dao.OrderItemDAO;
import com.clothes.dao.ProductDAO;
import com.clothes.dao.UserDAO;
import com.clothes.model.Order;
import com.clothes.model.OrderItem;
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
    private final OrderItemDAO orderItemDAO;
    private final UserDAO userDAO;
    private final ProductDAO productDAO;

    public AdminDashboardService(OrderDAO orderDAO, OrderItemDAO orderItemDAO, UserDAO userDAO, ProductDAO productDAO) {
        this.orderDAO = orderDAO;
        this.orderItemDAO = orderItemDAO;
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
        List<Product> topProducts = productDAO.findBestSelling(limit);

        // Calculate total for percentage
        int totalSales = topProducts.stream()
                .mapToInt(p -> p.getSoldCount() != null ? p.getSoldCount() : 0)
                .sum();

        if (totalSales == 0)
            totalSales = 1; // Avoid divide by zero

        for (Product p : topProducts) {
            int sold = p.getSoldCount() != null ? p.getSoldCount() : 0;
            p.setPercentage((sold * 100) / totalSales);
        }

        return topProducts;
    }

    /**
     * Get revenue breakdown by product category
     * Returns a map with category name as key and a map containing revenue and
     * percentage
     */
    public List<Map<String, Object>> getRevenueByCategory() {
        Map<String, BigDecimal> categoryRevenue = new HashMap<>();

        // Get all completed orders
        List<Order> completedOrders = orderDAO.findAll().stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.COMPLETED)
                .collect(Collectors.toList());

        System.out.println("DEBUG: Found " + completedOrders.size() + " completed orders");

        // Calculate revenue per category from order items
        for (Order order : completedOrders) {
            // Load order items for this order
            List<OrderItem> orderItems = orderItemDAO.findByOrderId(order.getOrderId());
            System.out.println("DEBUG: Order " + order.getOrderId() + " has " + orderItems.size() + " items");

            for (OrderItem item : orderItems) {
                // Get product to access category name
                Product product = productDAO.findById(item.getProductId()).orElse(null);

                if (product != null) {
                    String categoryName = product.getCategoryName();
                    System.out.println("DEBUG: Product " + product.getProductId() +
                            " - Name: " + product.getProductName() +
                            " - Category: " + categoryName);

                    if (categoryName == null || categoryName.isEmpty()) {
                        categoryName = "Khác";
                    }

                    BigDecimal itemRevenue = item.getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));

                    System.out.println("DEBUG: Item revenue " + item.getPrice() +
                            " x " + item.getQuantity() + " = " + itemRevenue);

                    categoryRevenue.merge(categoryName, itemRevenue, BigDecimal::add);
                }
            }
        }

        System.out.println("DEBUG: Category Revenue Map: " + categoryRevenue);

        // Calculate total revenue for percentage
        BigDecimal totalRevenue = categoryRevenue.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Convert to list with percentage
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : categoryRevenue.entrySet()) {
            Map<String, Object> categoryData = new HashMap<>();
            categoryData.put("name", entry.getKey());
            categoryData.put("revenue", entry.getValue());

            // Calculate percentage
            if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                double percentage = entry.getValue()
                        .divide(totalRevenue, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
                categoryData.put("percentage", Math.round(percentage * 10) / 10.0);
            } else {
                categoryData.put("percentage", 0.0);
            }

            result.add(categoryData);
        }

        // Sort by revenue descending
        result.sort((a, b) -> {
            BigDecimal revA = (BigDecimal) a.get("revenue");
            BigDecimal revB = (BigDecimal) b.get("revenue");
            return revB.compareTo(revA);
        });

        return result;
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
        int currentYear = LocalDateTime.now().getYear();

        // Get data for each month of current year (January to December)
        for (int month = 1; month <= 12; month++) {
            LocalDateTime monthStart = LocalDateTime.of(currentYear, month, 1, 0, 0);
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

    /**
     * Get revenue data for today (hourly)
     */
    public List<BigDecimal> getTodayRevenueData() {
        List<BigDecimal> dailyData = new ArrayList<>();
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        for (int hour = 0; hour < 24; hour++) {
            LocalDateTime hourStart = today.plusHours(hour);
            LocalDateTime hourEnd = hourStart.plusHours(1);

            BigDecimal revenue = orderDAO.findAll().stream()
                    .filter(o -> o.getOrderDate() != null)
                    .filter(o -> o.getOrderDate().isAfter(hourStart) && o.getOrderDate().isBefore(hourEnd))
                    .filter(o -> o.getStatus() == Order.OrderStatus.COMPLETED)
                    .map(Order::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            dailyData.add(revenue);
        }
        return dailyData;
    }

    /**
     * Get revenue data for the last 7 days (daily)
     */
    public List<BigDecimal> getWeeklyRevenueData() {
        List<BigDecimal> weeklyData = new ArrayList<>();
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        for (int day = 6; day >= 0; day--) {
            LocalDateTime dayStart = today.minusDays(day).withHour(0).withMinute(0);
            LocalDateTime dayEnd = dayStart.plusDays(1);

            BigDecimal revenue = orderDAO.findAll().stream()
                    .filter(o -> o.getOrderDate() != null)
                    .filter(o -> o.getOrderDate().isAfter(dayStart) && o.getOrderDate().isBefore(dayEnd))
                    .filter(o -> o.getStatus() == Order.OrderStatus.COMPLETED)
                    .map(Order::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            weeklyData.add(revenue);
        }
        return weeklyData;
    }

    /**
     * Get revenue data for the last 12 months or all months (monthly)
     * Same as getMonthlyRevenueData() - returns current year data
     */
    public List<BigDecimal> getYearlyRevenueData() {
        return getMonthlyRevenueData();
    }
}
