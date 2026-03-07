package com.clothes.controller.admincontroller;

import com.clothes.model.Category;
import com.clothes.model.Product;
import com.clothes.service.CategoryService;
import com.clothes.service.CloudinaryService;
import com.clothes.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.List;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.io.IOException;

/**
 * Admin Product Management Controller
 */
@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CloudinaryService cloudinaryService;

    public AdminProductController(ProductService productService, CategoryService categoryService,
            CloudinaryService cloudinaryService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping
    public String listProducts(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Boolean isNew,
            @RequestParam(required = false) Boolean isHot,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        if (session.getAttribute("adminId") == null) {
            return "redirect:/admin/login";
        }

        List<Product> products = productService.searchProductsPaginated(keyword, categoryId, status, sortBy, isNew,
                isHot, page, size);
        int totalProducts = productService.countProductsAdmin(keyword, categoryId, status, isNew, isHot);
        int totalPages = (int) Math.ceil((double) totalProducts / size);
        List<Category> categories = categoryService.getAllCategories();

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        return "admin/products";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Optional<Product> product = productService.getProductById(id);
        return product.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/add")
    public String showAddForm(HttpSession session, Model model) {
        if (session.getAttribute("adminId") == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/product-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, HttpSession session, Model model) {
        if (session.getAttribute("adminId") == null) {
            return "redirect:/admin/login";
        }

        Optional<Product> productOpt = productService.getProductById(id);
        if (productOpt.isEmpty()) {
            return "redirect:/admin/products";
        }

        model.addAttribute("product", productOpt.get());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/product-form";
    }

    @PostMapping("/save")
    public String saveProduct(@ModelAttribute Product product,
            @RequestParam(required = false) MultipartFile imageFile,
            HttpSession session) {
        if (session.getAttribute("adminId") == null) {
            return "redirect:/admin/login";
        }

        // Handle image upload via Cloudinary
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imageUrl = cloudinaryService.uploadImage(imageFile, "products");
                product.setImageUrl(imageUrl);
            } catch (IOException e) {
                // Log error
            }
        }

        productService.saveProduct(product);
        return "redirect:/admin/products";
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/toggle-status")
    @ResponseBody
    public ResponseEntity<Void> toggleStatus(@PathVariable Long id) {
        productService.toggleProductStatus(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/toggle-new")
    @ResponseBody
    public ResponseEntity<Void> toggleNew(@PathVariable Long id) {
        productService.toggleProductNew(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/toggle-hot")
    @ResponseBody
    public ResponseEntity<Void> toggleHot(@PathVariable Long id) {
        productService.toggleProductHot(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/duplicate")
    @ResponseBody
    public ResponseEntity<Product> duplicateProduct(@PathVariable Long id) {
        try {
            Product duplicate = productService.duplicateProduct(id);
            return ResponseEntity.ok(duplicate);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/delete-multiple")
    @ResponseBody
    public ResponseEntity<Void> deleteMultiple(@RequestBody List<Long> ids) {
        try {
            productService.deleteMultiple(ids);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export")
    public String exportProducts(HttpSession session) {
        if (session.getAttribute("adminId") == null) {
            return "redirect:/admin/login";
        }
        // TODO: Implement Excel export
        return "redirect:/admin/products";
    }
}
