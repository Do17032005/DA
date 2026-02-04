package com.clothes.service;

import com.clothes.dao.ProductDAO;
import com.clothes.model.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for Product operations
 */
@Service
@Transactional
public class ProductService {

    private final ProductDAO productDAO;

    public ProductService(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    public List<Product> getAllProducts() {
        return productDAO.findAll();
    }

    public List<Product> searchProducts(String keyword, Long categoryId, String status, String sortBy) {
        // Implementation will use ProductDAO methods
        return productDAO.findAll(); // Placeholder
    }

    public Optional<Product> getProductById(Long id) {
        return productDAO.findById(id);
    }

    public Product saveProduct(Product product) {
        if (product.getProductId() == null) {
            Long id = productDAO.save(product);
            product.setProductId(id);
        } else {
            productDAO.update(product);
        }
        return product;
    }

    public void deleteProduct(Long id) {
        productDAO.delete(id);
    }

    public void toggleProductStatus(Long id) {
        Optional<Product> product = productDAO.findById(id);
        product.ifPresent(p -> {
            p.setIsActive(!Boolean.TRUE.equals(p.getIsActive()));
            productDAO.update(p);
        });
    }

    public Product duplicateProduct(Long id) {
        Optional<Product> original = productDAO.findById(id);
        if (original.isPresent()) {
            Product duplicate = original.get();
            duplicate.setProductId(null);
            duplicate.setProductName(duplicate.getProductName() + " (Copy)");
            duplicate.setSku(duplicate.getSku() + "-COPY");
            Long newId = productDAO.save(duplicate);
            duplicate.setProductId(newId);
            return duplicate;
        }
        throw new RuntimeException("Product not found");
    }

    public void deleteMultiple(List<Long> ids) {
        ids.forEach(productDAO::delete);
    }

    public int countProducts() {
        return productDAO.findAll().size();
    }

    public int countLowStock(int threshold) {
        return (int) productDAO.findAll().stream()
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() <= threshold)
                .count();
    }
}
