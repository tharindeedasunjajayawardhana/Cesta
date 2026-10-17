
package com.Cesta.controller;

import com.Cesta.model.CustomerOrder;
import com.Cesta.repository.CustomerOrderRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final CustomerOrderRepository customerOrderRepository;

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "PLACED",
            "CONFIRMED",
            "PACKED",
            "SHIPPED",
            "DELIVERED",
            "CANCELLED"
    );

    public AdminOrderController(
            CustomerOrderRepository customerOrderRepository) {
        this.customerOrderRepository = customerOrderRepository;
    }

    @GetMapping
    public String listOrders(Model model) {
        List<CustomerOrder> orders =
                customerOrderRepository.findAllByOrderByCreatedAtDesc();

        model.addAttribute("orders", orders);
        return "admin/orders";
    }

    @GetMapping("/{id}")
    public String orderDetails(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        CustomerOrder order = customerOrderRepository
                .findWithCustomerAndItemsById(id)
                .orElse(null);

        if (order == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Order not found."
            );
            return "redirect:/admin/orders";
        }

        model.addAttribute("order", order);
        model.addAttribute("statuses", ALLOWED_STATUSES);

        return "admin/order-detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {

        String newStatus = status == null
                ? ""
                : status.trim().toUpperCase();

        if (!ALLOWED_STATUSES.contains(newStatus)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Invalid order status."
            );
            return "redirect:/admin/orders/" + id;
        }

        CustomerOrder order = customerOrderRepository
                .findById(id)
                .orElse(null);

        if (order == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Order not found."
            );
            return "redirect:/admin/orders";
        }

        order.setStatus(newStatus);
        customerOrderRepository.save(order);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Order #" + id + " status updated to " + newStatus + "."
        );

        return "redirect:/admin/orders/" + id;
    }
}
