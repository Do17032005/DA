package com.clothes.controller;

import com.clothes.dao.ProductDAO;
import com.clothes.model.Product;
import com.clothes.service.CartService;
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

    public CartController(CartService cartService, ProductDAO productDAO) {
        this.cartService = cartService;
        this.productDAO = productDAO;
    }

    /**
     * Show cart page
     */
    @GetMapping
    public String showCart(Model model) {
        model.addAttribute("cart", cartService.getCart());
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
     * Update cart item quantity
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
}
