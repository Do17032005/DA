package com.clothes.controller;

import com.clothes.dao.ProductDAO;
import com.clothes.model.Product;
import com.clothes.model.Voucher;
import com.clothes.service.CartService;
import com.clothes.service.VoucherService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Controller for shopping cart operations
 */
@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final ProductDAO productDAO;
    private final VoucherService voucherService;

    public CartController(CartService cartService, ProductDAO productDAO, VoucherService voucherService) {
        this.cartService = cartService;
        this.productDAO = productDAO;
        this.voucherService = voucherService;
    }

    /**
     * Show cart page
     */
    @GetMapping
    public String showCart(Model model, HttpSession session) {
        com.clothes.model.Cart cart = cartService.getCart();
        java.util.List<com.clothes.model.CartItem> items = cart.getItems();

        BigDecimal subtotal = items.stream()
                .map(item -> item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Default shipping logic: 30k, free over 500k
        BigDecimal shippingFee = BigDecimal.ZERO;
        // Check setting or use default
        BigDecimal freeShippingThreshold = new BigDecimal("500000"); // Should get from settings service if possible

        if (subtotal.compareTo(BigDecimal.ZERO) > 0) {
            shippingFee = subtotal.compareTo(freeShippingThreshold) >= 0 ? BigDecimal.ZERO : new BigDecimal("30000");
        }

        // Voucher logic
        BigDecimal discount = BigDecimal.ZERO;
        Voucher appliedVoucher = (Voucher) session.getAttribute("appliedVoucher");

        if (appliedVoucher != null) {
            // Re-validate voucher
            try {
                // Refresh voucher from DB to check current validity
                Optional<Voucher> currentVoucher = voucherService.getVoucherById(appliedVoucher.getVoucherId());
                if (currentVoucher.isPresent() && currentVoucher.get().isValid()) {
                    discount = currentVoucher.get().calculateDiscount(subtotal);
                    appliedVoucher = currentVoucher.get(); // Update with latest info
                    session.setAttribute("appliedVoucher", appliedVoucher);
                } else {
                    session.removeAttribute("appliedVoucher");
                    appliedVoucher = null;
                }
            } catch (Exception e) {
                session.removeAttribute("appliedVoucher");
                appliedVoucher = null;
            }
        }

        BigDecimal total = subtotal.add(shippingFee).subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0)
            total = BigDecimal.ZERO;

        model.addAttribute("cart", cart);
        model.addAttribute("cartItems", items);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("discount", discount);
        model.addAttribute("total", total);
        model.addAttribute("appliedVoucher", appliedVoucher);

        // Get valid vouchers for display
        model.addAttribute("availableVouchers", voucherService.getValidVouchers());

        return "cart";
    }

    /**
     * Add item to cart (Legacy form support)
     */
    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String color,
            RedirectAttributes redirectAttributes) {
        try {
            // Get product details
            Optional<Product> productOpt = productDAO.findById(productId);

            if (productOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Sản phẩm không tồn tại");
                return "redirect:/products";
            }

            Product product = productOpt.get();

            // Check if product is active
            if (!product.getIsActive()) {
                redirectAttributes.addFlashAttribute("error", "Sản phẩm không còn kinh doanh");
                return "redirect:/products";
            }

            // Check stock
            if (product.getStockQuantity() < quantity) {
                redirectAttributes.addFlashAttribute("error", "Số lượng sản phẩm không đủ");
                return "redirect:/products/" + productId;
            }

            cartService.addItem(
                    product.getProductId(),
                    product.getProductName(),
                    product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice(),
                    quantity,
                    product.getImageUrl(),
                    size,
                    color);

            redirectAttributes.addFlashAttribute("success", "Đã thêm sản phẩm vào giỏ hàng");
            return "redirect:/cart";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/products";
        }
    }

    /**
     * Add item to cart (API for AJAX)
     */
    @PostMapping("/api/add")
    @ResponseBody
    public java.util.Map<String, Object> addToCartApi(@RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String color) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            Optional<Product> productOpt = productDAO.findById(productId);
            if (productOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Sản phẩm không tồn tại");
                return response;
            }

            Product product = productOpt.get();
            if (product.getStockQuantity() < quantity) {
                response.put("success", false);
                response.put("message", "Số lượng không đủ");
                return response;
            }

            cartService.addItem(
                    product.getProductId(),
                    product.getProductName(),
                    product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice(),
                    quantity,
                    product.getImageUrl(),
                    size,
                    color);

            response.put("success", true);
            response.put("count", cartService.getTotalItems());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * Update cart item quantity (AJAX by CartItemId)
     */
    @PostMapping("/update/{cartItemId}")
    @ResponseBody
    public java.util.Map<String, Object> updateCartItemApi(@PathVariable Long cartItemId,
            @RequestParam Integer quantity) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            cartService.updateCartItemQuantity(cartItemId, quantity);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * Remove item from cart (AJAX by CartItemId)
     */
    @PostMapping("/remove/{cartItemId}")
    @ResponseBody
    public java.util.Map<String, Object> removeCartItemApi(@PathVariable Long cartItemId) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            cartService.removeCartItem(cartItemId);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * Update cart item quantity (Legacy)
     */
    @PostMapping("/update")
    public String updateCart(@RequestParam Long productId,
            @RequestParam Integer quantity,
            RedirectAttributes redirectAttributes) {
        try {
            if (quantity <= 0) {
                cartService.removeItem(productId);
                redirectAttributes.addFlashAttribute("success", "Đã xóa sản phẩm khỏi giỏ hàng");
            } else {
                // Check stock
                Optional<Product> productOpt = productDAO.findById(productId);
                if (productOpt.isPresent()) {
                    Product product = productOpt.get();
                    if (product.getStockQuantity() < quantity) {
                        redirectAttributes.addFlashAttribute("error",
                                "Chỉ còn " + product.getStockQuantity() + " sản phẩm trong kho");
                        return "redirect:/cart";
                    }
                }

                cartService.updateQuantity(productId, quantity);
                redirectAttributes.addFlashAttribute("success", "Đã cập nhật giỏ hàng");
            }

            return "redirect:/cart";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        }
    }

    /**
     * Remove item from cart
     */
    @PostMapping("/remove")
    public String removeFromCart(@RequestParam Long productId,
            RedirectAttributes redirectAttributes) {
        try {
            cartService.removeItem(productId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa sản phẩm khỏi giỏ hàng");
            return "redirect:/cart";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        }
    }

    /**
     * Clear cart
     */
    @PostMapping("/clear")
    public String clearCart(RedirectAttributes redirectAttributes) {
        cartService.clearCart();
        redirectAttributes.addFlashAttribute("success", "Đã xóa toàn bộ giỏ hàng");
        return "redirect:/cart";
    }

    /**
     * Get cart count (AJAX)
     */
    @GetMapping("/count")
    @ResponseBody
    public int getCartCount() {
        return cartService.getTotalItems();
    }

    /**
     * Get cart total (AJAX)
     */
    @GetMapping("/total")
    @ResponseBody
    public BigDecimal getCartTotal() {
        return cartService.getTotalAmount();
    }

    /**
     * Apply voucher
     */
    @PostMapping("/apply-voucher")
    @ResponseBody
    public java.util.Map<String, Object> applyVoucher(@RequestParam String code, HttpSession session) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            Optional<Voucher> voucherOpt = voucherService.getVoucherByCode(code);
            if (voucherOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Mã giảm giá không tồn tại");
                return response;
            }

            Voucher voucher = voucherOpt.get();
            if (!voucher.isValid()) {
                response.put("success", false);
                response.put("message", "Mã giảm giá đã hết hạn hoặc không khả dụng");
                return response;
            }

            // Check min order value
            com.clothes.model.Cart cart = cartService.getCart();
            java.util.List<com.clothes.model.CartItem> items = cart.getItems();
            BigDecimal subtotal = items.stream()
                    .map(item -> item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (voucher.getMinOrderValue() != null && subtotal.compareTo(voucher.getMinOrderValue()) < 0) {
                response.put("success", false);
                response.put("message", "Giá trị đơn hàng tối thiểu là "
                        + new java.text.DecimalFormat("#,###").format(voucher.getMinOrderValue()) + "đ");
                return response;
            }

            session.setAttribute("appliedVoucher", voucher);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * Remove voucher
     */
    @PostMapping("/remove-voucher")
    @ResponseBody
    public java.util.Map<String, Object> removeVoucher(HttpSession session) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        session.removeAttribute("appliedVoucher");
        response.put("success", true);
        return response;
    }
}
