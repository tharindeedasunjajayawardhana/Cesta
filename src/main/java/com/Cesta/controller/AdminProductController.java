package com.Cesta.controller;

import com.Cesta.model.Product;
import com.Cesta.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductRepository productRepository;
    private final Path uploadDirectory;

    public AdminProductController(
            ProductRepository productRepository,
            @Value("${cesta.upload-dir:uploads/products}") String uploadDir) {

        this.productRepository = productRepository;

        this.uploadDirectory = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();
    }

    @GetMapping
    public String listProducts(Model model) {
        model.addAttribute("products", productRepository.findAll());
        return "admin/products";
    }

    @GetMapping("/new")
    public String newProduct(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("pageTitle", "Add Product");
        return "admin/product-form";
    }

    @GetMapping("/edit/{id}")
    public String editProduct(@PathVariable Long id, Model model) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Product not found: " + id));

        model.addAttribute("product", product);
        model.addAttribute("pageTitle", "Edit Product");

        return "admin/product-form";
    }

    @PostMapping("/save")
    public String saveProduct(
            @ModelAttribute Product formProduct,
            @RequestParam(value = "imageFile", required = false)
            MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {

        Product product;

        // Editing an existing product
        if (formProduct.getId() != null) {

            product = productRepository.findById(formProduct.getId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Product not found: " + formProduct.getId()));

            product.setName(formProduct.getName());
            product.setCategory(formProduct.getCategory());
            product.setDescription(formProduct.getDescription());
            product.setPrice(formProduct.getPrice());
            product.setOriginalPrice(formProduct.getOriginalPrice());
            product.setImageEmoji(formProduct.getImageEmoji());
            product.setStockQuantity(formProduct.getStockQuantity());
            product.setActive(formProduct.isActive());

        } else {
            // Creating a new product
            product = formProduct;
        }

        // Save uploaded image
        if (imageFile != null && !imageFile.isEmpty()) {

            try {

                String originalName = imageFile.getOriginalFilename();

                if (originalName == null) {
                    throw new IllegalArgumentException("Invalid image file.");
                }

                String lowerName = originalName.toLowerCase();

                String extension;

                if (lowerName.endsWith(".jpg")) {
                    extension = ".jpg";
                } else if (lowerName.endsWith(".jpeg")) {
                    extension = ".jpeg";
                } else if (lowerName.endsWith(".png")) {
                    extension = ".png";
                } else if (lowerName.endsWith(".webp")) {
                    extension = ".webp";
                } else {
                    throw new IllegalArgumentException(
                            "Only JPG, JPEG, PNG and WebP images are allowed.");
                }

                Files.createDirectories(uploadDirectory);

                String fileName = UUID.randomUUID() + extension;

                Path destination = uploadDirectory
                        .resolve(fileName)
                        .normalize();

                Files.copy(
                        imageFile.getInputStream(),
                        destination,
                        StandardCopyOption.REPLACE_EXISTING
                );

                product.setImagePath("/uploads/products/" + fileName);

            } catch (IOException e) {

                throw new IllegalStateException(
                        "Could not save image.", e);
            }
        }

        productRepository.save(product);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Product saved successfully!");

        return "redirect:/admin/products";
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        if (productRepository.existsById(id)) {

            productRepository.deleteById(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Product deleted successfully!");
        }

        return "redirect:/admin/products";
    }
}