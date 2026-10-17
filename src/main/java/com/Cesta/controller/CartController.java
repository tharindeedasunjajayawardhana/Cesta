
package com.Cesta.controller;

import com.Cesta.model.Product;
import com.Cesta.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CartController {

    private static final String CART_KEY = "cestaCart";

    private final ProductRepository productRepository;

    public CartController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostMapping("/cart/add")
    public String addToCart(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Product product = productRepository.findById(productId)
                .orElse(null);

        if (product == null) {
            redirectAttributes.addFlashAttribute(
                    "cartMessage", "Sorry, this product could not be found."
            );
            return "redirect:/";
        }

        if (quantity < 1 || quantity > 99) {
            redirectAttributes.addFlashAttribute(
                    "cartMessage", "Please choose a quantity between 1 and 99."
            );
            return "redirect:/";
        }

        Map<Long, Integer> cart = getCart(session);

        int currentQuantity = cart.getOrDefault(productId, 0);

        if (currentQuantity + quantity > 99) {
            redirectAttributes.addFlashAttribute(
                    "cartMessage", "The maximum quantity for an item is 99."
            );
            return "redirect:/";
        }

        cart.put(productId, currentQuantity + quantity);
        session.setAttribute(CART_KEY, cart);

        redirectAttributes.addFlashAttribute(
                "cartMessage", product.getName() + " added to your cart!"
        );

        return "redirect:/cart";
    }

    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {

        Map<Long, Integer> cart = getCart(session);

        List<CartLine> cartItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {

            Product product = productRepository.findById(entry.getKey())
                    .orElse(null);

            if (product == null) {
                continue;
            }

            int quantity = entry.getValue();

            BigDecimal lineTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(quantity));

            cartItems.add(new CartLine(product, quantity, lineTotal));
            subtotal = subtotal.add(lineTotal);
        }

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("subtotal", subtotal);

        return "cart";
    }

    @PostMapping("/cart/update")
    public String updateQuantity(
            @RequestParam Long productId,
            @RequestParam int quantity,
            HttpSession session) {

        Map<Long, Integer> cart = getCart(session);

        if (cart.containsKey(productId)) {
            if (quantity <= 0) {
                cart.remove(productId);
            } else if (quantity <= 99) {
                cart.put(productId, quantity);
            }
        }

        session.setAttribute(CART_KEY, cart);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(
            @RequestParam Long productId,
            HttpSession session) {

        Map<Long, Integer> cart = getCart(session);
        cart.remove(productId);
        session.setAttribute(CART_KEY, cart);

        return "redirect:/cart";
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getCart(HttpSession session) {

        Object savedCart = session.getAttribute(CART_KEY);

        if (savedCart instanceof Map<?, ?>) {
            return (Map<Long, Integer>) savedCart;
        }

        Map<Long, Integer> newCart = new LinkedHashMap<>();
        session.setAttribute(CART_KEY, newCart);

        return newCart;
    }

    public static class CartLine {

        private final Product product;
        private final int quantity;
        private final BigDecimal lineTotal;

        public CartLine(
                Product product,
                int quantity,
                BigDecimal lineTotal) {
            this.product = product;
            this.quantity = quantity;
            this.lineTotal = lineTotal;
        }

        public Product getProduct() {
            return product;
        }

        public int getQuantity() {
            return quantity;
        }

        public BigDecimal getLineTotal() {
            return lineTotal;
        }
    }
}
