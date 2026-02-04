package com.clothes.controller;

import com.clothes.model.Address;
import com.clothes.model.Cart;
import com.clothes.model.CartItem;
import com.clothes.model.Order;
import com.clothes.model.Voucher;
import com.clothes.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Controller for order management and checkout
 */
@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final AddressService addressService;
    private final VoucherService voucherService;

    public OrderController(OrderService orderService, CartService cartService,
            AddressService addressService, VoucherService voucherService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.addressService = addressService;
        this.voucherService = voucherService;
    }

    /**
     * Show checkout page
     */
    @GetMapping("/checkout")
    public String showCheckout(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để tiếp tục");
            return "redirect:/user/login";
        }

        Cart cart = cartService.getCart();
        if (cart.getItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống");
            return "redirect:/cart";
        }

        // Get user addresses
        List<Address> addresses = addressService.getAddressesByUserId(userId);
        Optional<Address> defaultAddress = addressService.getDefaultAddress(userId);

        // Calculate totals
        BigDecimal subtotal = cart.getTotalAmount();
        BigDecimal shippingFee = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;

        // Check for voucher in session
        Object voucherObj = session.getAttribute("appliedVoucher");
        Voucher appliedVoucher = null;
        if (voucherObj instanceof Voucher) {
            appliedVoucher = (Voucher) voucherObj;
            try {
                // Calculate discount without incrementing usage
                discount = appliedVoucher.calculateDiscount(subtotal);
            } catch (Exception e) {
                // Ignore calculation errors
            }
        }

        BigDecimal total = subtotal.add(shippingFee).subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        model.addAttribute("cart", cart);
        model.addAttribute("cartItems", cart.getItems());
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("discount", discount);
        model.addAttribute("total", total);
        model.addAttribute("appliedVoucher", appliedVoucher);

        model.addAttribute("addresses", addresses);
        model.addAttribute("defaultAddress", defaultAddress.orElse(null));

        return "checkout";
    }

    /**
     * Process checkout
     */
    @PostMapping("/checkout")
    public String processCheckout(
            @RequestParam(required = false) Long addressId,
            @RequestParam(defaultValue = "COD") String paymentMethod,
            @RequestParam(required = false) String voucherCode,
            @RequestParam(name = "note", required = false) String notes,
            // New fields for address creation
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) Boolean saveAddress,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        try {
            Cart cart = cartService.getCart();
            if (cart.getItems().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống");
                return "redirect:/cart";
            }

            String shippingAddress = "";

            // Logic to determine address
            if (addressId != null) {
                // Get shipping address from ID
                Optional<Address> addressOpt = addressService.getAddressById(addressId);
                if (addressOpt.isPresent() && addressOpt.get().getUserId().equals(userId)) {
                    shippingAddress = addressOpt.get().getFullAddress();
                }
            }

            // If addressId invalid or not provided, try to construct from fields
            if (shippingAddress.isEmpty()) {
                if (province != null && district != null && ward != null && address != null) {
                    shippingAddress = String.format("%s, %s, %s, %s (Người nhận: %s, SĐT: %s)",
                            address, ward, district, province, fullName, phone);

                    // Save new address if requested
                    if (Boolean.TRUE.equals(saveAddress)) {
                        try {
                            addressService.createAddress(userId, fullName, phone, address, ward, district, province,
                                    false);
                        } catch (Exception e) {
                            // Ignore save error, continue with order
                        }
                    }
                } else {
                    // Fail if no address info at all
                    redirectAttributes.addFlashAttribute("error", "Vui lòng cung cấp địa chỉ giao hàng hợp lệ");
                    return "redirect:/orders/checkout";
                }
            }

            // Apply voucher if provided
            BigDecimal totalAmount = cart.getTotalAmount();
            BigDecimal discount = BigDecimal.ZERO;

            if (voucherCode != null && !voucherCode.trim().isEmpty()) {
                try {
                    discount = voucherService.applyVoucher(voucherCode, totalAmount);
                    totalAmount = totalAmount.subtract(discount);
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("warning", "Voucher: " + e.getMessage());
                }
            }

            // Create order
            Long orderId = orderService.createOrder(userId, cart, shippingAddress, paymentMethod, notes);

            // Clear cart
            cartService.clearCart();

            redirectAttributes.addFlashAttribute("success", "Đặt hàng thành công!");
            redirectAttributes.addFlashAttribute("orderId", orderId);

            return "redirect:/orders/success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xử lý đơn hàng: " + e.getMessage());
            return "redirect:/orders/checkout";
        }
    }

    /**
     * Show order success page
     */
    @GetMapping("/success")
    public String showOrderSuccess() {
        return "order-success";
    }

    /**
     * Show user's order history
     */
    @GetMapping
    public String showOrders(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/user/login";
        }

        List<Order> orders = orderService.getOrdersByUserId(userId);

        model.addAttribute("orders", orders);

        return "orders";
    }

    /**
     * Show order detail
     */
    @GetMapping("/{id}")
    public String showOrderDetail(@PathVariable Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/user/login";
        }

        Optional<Order> orderOpt = orderService.getOrderById(id);

        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Đơn hàng không tồn tại");
            return "redirect:/orders";
        }

        Order order = orderOpt.get();

        // Check if user owns this order
        if (!order.getUserId().equals(userId)) {
            redirectAttributes.addFlashAttribute("error", "Không có quyền xem đơn hàng này");
            return "redirect:/orders";
        }

        model.addAttribute("order", order);

        return "order-detail";
    }

    /**
     * Cancel order
     */
    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        try {
            boolean cancelled = orderService.cancelOrder(id, userId);

            if (cancelled) {
                redirectAttributes.addFlashAttribute("success", "Đã hủy đơn hàng");
            } else {
                redirectAttributes.addFlashAttribute("error", "Không thể hủy đơn hàng");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/orders/" + id;
    }
}
