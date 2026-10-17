package com.Cesta.controller;

import com.Cesta.model.Product;
import com.Cesta.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class Homecontroller {

    private final ProductRepository productRepository;

    public Homecontroller(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/")
    public String home(Model model) {

        // Retrieve active products from the database
        List<Product> products = productRepository.findByActiveTrue();

        // Pass all active products to the homepage
        model.addAttribute("products", products);

        // Show discounted products in the deals section
        List<Product> deals = products.stream()
                .filter(product ->
                        product.getOriginalPrice() != null
                                && product.getOriginalPrice()
                                .compareTo(product.getPrice()) > 0)
                .collect(Collectors.toList());

        model.addAttribute("deals", deals);

        return "index";
    }
}
