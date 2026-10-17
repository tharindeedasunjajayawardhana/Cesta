package com.Cesta.controller;

import com.Cesta.model.CustomerOrder;
import com.Cesta.repository.CustomerOrderRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class CustomerOrderController {

    private final CustomerOrderRepository customerOrderRepository;

    public CustomerOrderController(
            CustomerOrderRepository customerOrderRepository) {
        this.customerOrderRepository = customerOrderRepository;
    }

    // Display the signed-in customer's order history
    @GetMapping("/orders")
    public String showOrders(HttpSession session, Model model) {

        Object sessionCustomerId =
                session.getAttribute("cestaCustomerId");

        if (!(sessionCustomerId instanceof Long)) {
            return "redirect:/login";
        }

        Long customerId = (Long) sessionCustomerId;

        List<CustomerOrder> orders =
                customerOrderRepository
                        .findByCustomerIdOrderByCreatedAtDesc(customerId);

        model.addAttribute("orders", orders);

        return "orders";
    }

    // Display one order, but only if it belongs to the signed-in customer
    @GetMapping("/orders/{id}")
    public String showOrderDetails(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        Object sessionCustomerId =
                session.getAttribute("cestaCustomerId");

        if (!(sessionCustomerId instanceof Long)) {
            return "redirect:/login";
        }

        Long customerId = (Long) sessionCustomerId;

        CustomerOrder order = customerOrderRepository
                .findByIdAndCustomerId(id, customerId)
                .orElse(null);

        if (order == null) {
            // Do not expose another customer's order
            return "redirect:/orders";
        }

        model.addAttribute("order", order);

        return "order-detail";
    }
}