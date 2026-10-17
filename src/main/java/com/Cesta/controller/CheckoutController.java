
package com.Cesta.controller;

import com.Cesta.model.Customer;
import com.Cesta.model.CustomerOrder;
import com.Cesta.model.OrderItem;
import com.Cesta.model.Product;
import com.Cesta.repository.CustomerOrderRepository;
import com.Cesta.repository.CustomerRepository;
import com.Cesta.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Controller
public class CheckoutController {

    private static final String CART_KEY = "cestaCart";

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CustomerOrderRepository customerOrderRepository;

    public CheckoutController(
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            CustomerOrderRepository customerOrderRepository) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.customerOrderRepository = customerOrderRepository;
    }

    @GetMapping("/checkout")
    public String showCheckout(HttpSession session, Model model) {
        Long customerId = getCustomerId(session);

        if (customerId == null) {
            return "redirect:/login";
        }

        Map<Long, Integer> cart = getCart(session);

        if (cart.isEmpty()) {
            return "redirect:/cart";
        }

        Customer customer = customerRepository.findById(customerId)
                .orElse(null);

        if (customer == null) {
            session.invalidate();
            return "redirect:/login";
        }

        BigDecimal subtotal = calculateSubtotal(cart);

        model.addAttribute("customer", customer);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("total", subtotal);
        model.addAttribute(
                "checkoutMessage",
                model.asMap().get("checkoutMessage")
        );

        return "checkout";
    }

    @PostMapping("/checkout/place-order")
    @Transactional
    public String placeOrder(
            @RequestParam String deliveryName,
            @RequestParam String deliveryPhone,
            @RequestParam String deliveryAddress,
            @RequestParam String deliveryCity,
            @RequestParam(required = false, defaultValue = "")
            String deliveryPostalCode,
            @RequestParam String paymentMethod,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long customerId = getCustomerId(session);

        if (customerId == null) {
            return "redirect:/login";
        }

        Map<Long, Integer> cart = getCart(session);

        if (cart.isEmpty()) {
            return "redirect:/cart";
        }

        Customer customer = customerRepository.findById(customerId)
                .orElse(null);

        if (customer == null) {
            session.invalidate();
            return "redirect:/login";
        }

        if (isBlank(deliveryName)
                || isBlank(deliveryPhone)
                || isBlank(deliveryAddress)
                || isBlank(deliveryCity)) {

            redirectAttributes.addFlashAttribute(
                    "checkoutMessage",
                    "Please complete all required delivery details."
            );
            return "redirect:/checkout";
        }

        // Online payments are not connected to a gateway yet.
        if (!"CASH_ON_DELIVERY".equals(paymentMethod)) {
            redirectAttributes.addFlashAttribute(
                    "checkoutMessage",
                    "Online payment is not available yet. "
                            + "Please choose Cash on Delivery."
            );
            return "redirect:/checkout";
        }

        /*
         * Sort by product ID before acquiring locks.
         * Consistent lock order reduces the risk of deadlocks.
         */
        Map<Long, Integer> sortedCart = new TreeMap<>(cart);

        /*
         * Lock and validate every product before changing any stock.
         * Prices are always read from the database.
         */
        Map<Product, Integer> productsToOrder = new LinkedHashMap<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : sortedCart.entrySet()) {
            Long productId = entry.getKey();
            Integer requestedQuantity = entry.getValue();

            if (productId == null
                    || requestedQuantity == null
                    || requestedQuantity < 1
                    || requestedQuantity > 99) {

                redirectAttributes.addFlashAttribute(
                        "checkoutMessage",
                        "One of the quantities in your cart is invalid."
                );
                return "redirect:/cart";
            }

            Optional<Product> productResult =
                    productRepository.findByIdForUpdate(productId);

            if (productResult.isEmpty()) {
                redirectAttributes.addFlashAttribute(
                        "checkoutMessage",
                        "A product in your cart is no longer available. "
                                + "Please review your cart."
                );
                return "redirect:/cart";
            }

            Product product = productResult.get();

            if (!product.isActive()) {
                redirectAttributes.addFlashAttribute(
                        "checkoutMessage",
                        product.getName()
                                + " is no longer available for purchase."
                );
                return "redirect:/cart";
            }

            Integer availableStock = product.getStockQuantity();

            if (availableStock == null
                    || availableStock < 0
                    || requestedQuantity > availableStock) {

                redirectAttributes.addFlashAttribute(
                        "checkoutMessage",
                        "Not enough stock for " + product.getName()
                                + ". Available quantity: "
                                + Math.max(availableStock == null
                                        ? 0 : availableStock, 0)
                                + ". Please update your cart."
                );
                return "redirect:/cart";
            }

            productsToOrder.put(product, requestedQuantity);

            subtotal = subtotal.add(
                    product.getPrice().multiply(
                            BigDecimal.valueOf(requestedQuantity)
                    )
            );
        }

        // All products passed validation. Now deduct stock.
        for (Map.Entry<Product, Integer> entry
                : productsToOrder.entrySet()) {

            Product product = entry.getKey();
            int quantity = entry.getValue();

            product.setStockQuantity(
                    product.getStockQuantity() - quantity
            );
        }

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setSubtotal(subtotal);
        order.setTotal(subtotal);
        order.setStatus("PLACED");
        order.setDeliveryName(deliveryName.trim());
        order.setDeliveryPhone(deliveryPhone.trim());
        order.setDeliveryAddress(deliveryAddress.trim());
        order.setDeliveryCity(deliveryCity.trim());
        order.setDeliveryPostalCode(deliveryPostalCode.trim());
        order.setPaymentMethod("CASH_ON_DELIVERY");

        for (Map.Entry<Product, Integer> entry
                : productsToOrder.entrySet()) {

            Product product = entry.getKey();
            int quantity = entry.getValue();

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setQuantity(quantity);
            item.setUnitPrice(product.getPrice());
            item.setLineTotal(
                    product.getPrice().multiply(
                            BigDecimal.valueOf(quantity)
                    )
            );

            order.addItem(item);
        }

        /*
         * Stock updates and order saving participate in the same
         * transaction. A database failure should roll both back.
         */
        CustomerOrder savedOrder = customerOrderRepository.save(order);

        // Clear the cart only after saving succeeds.
        session.setAttribute(
                CART_KEY,
                new LinkedHashMap<Long, Integer>()
        );

        return "redirect:/order-confirmation/" + savedOrder.getId();
    }

    @GetMapping("/order-confirmation/{id}")
    public String showConfirmation(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        Long customerId = getCustomerId(session);

        if (customerId == null) {
            return "redirect:/login";
        }

        CustomerOrder order = customerOrderRepository
                .findByIdAndCustomerId(id, customerId)
                .orElse(null);

        if (order == null) {
            return "redirect:/orders";
        }

        model.addAttribute("order", order);
        return "order-confirmation";
    }

    private BigDecimal calculateSubtotal(Map<Long, Integer> cart) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            Product product = productRepository.findById(entry.getKey())
                    .orElse(null);

            Integer quantity = entry.getValue();

            if (product != null
                    && quantity != null
                    && quantity > 0) {

                subtotal = subtotal.add(
                        product.getPrice().multiply(
                                BigDecimal.valueOf(quantity)
                        )
                );
            }
        }

        return subtotal;
    }

    private Long getCustomerId(HttpSession session) {
        Object value = session.getAttribute("cestaCustomerId");
        return value instanceof Long ? (Long) value : null;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getCart(HttpSession session) {
        Object value = session.getAttribute(CART_KEY);

        if (value instanceof Map<?, ?>) {
            return (Map<Long, Integer>) value;
        }

        Map<Long, Integer> cart = new LinkedHashMap<>();
        session.setAttribute(CART_KEY, cart);
        return cart;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
