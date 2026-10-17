
package com.Cesta.controller;

import com.Cesta.model.Customer;
import com.Cesta.repository.CustomerRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Controller
public class CustomerAccountController {

    private final CustomerRepository customerRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public CustomerAccountController(
            CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String registerCustomer(
            @Valid @ModelAttribute("registrationForm") RegistrationForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "register";
        }

        String email = form.getEmail().trim().toLowerCase();

        if (customerRepository.existsByEmailIgnoreCase(email)) {
            bindingResult.rejectValue(
                    "email",
                    "duplicate",
                    "An account with this email already exists."
            );
            return "register";
        }

        Customer customer = new Customer();
        customer.setFullName(form.getFullName().trim());
        customer.setEmail(email);
        customer.setPasswordHash(
                passwordEncoder.encode(form.getPassword())
        );
        customer.setPhone(form.getPhone().trim());

        // Registration must never grant admin privileges.
        customer.setRole("CUSTOMER");

        customerRepository.save(customer);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Your account was created successfully. Please log in."
        );

        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "login";
    }

    @PostMapping("/login")
    public String loginCustomer(
            @Valid @ModelAttribute("loginForm") LoginForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "login";
        }

        Customer customer = customerRepository
                .findByEmailIgnoreCase(form.getEmail().trim())
                .orElse(null);

        if (customer == null
                || !passwordEncoder.matches(
                        form.getPassword(),
                        customer.getPasswordHash())) {

            model.addAttribute(
                    "errorMessage",
                    "Invalid email or password."
            );
            return "login";
        }

        // Rotate the session ID after successful authentication.
        HttpSession session = request.getSession();
        request.changeSessionId();

        session.setAttribute("cestaCustomerId", customer.getId());
        session.setAttribute("cestaRole", customer.getRole());

        if ("ADMIN".equals(customer.getRole())) {
            return "redirect:/admin/orders";
        }

        return "redirect:/account";
    }

    @GetMapping("/account")
    public String showAccount(HttpSession session, Model model) {
        Object customerId = session.getAttribute("cestaCustomerId");

        if (!(customerId instanceof Long)) {
            return "redirect:/login";
        }

        Customer customer = customerRepository
                .findById((Long) customerId)
                .orElse(null);

        if (customer == null) {
            session.invalidate();
            return "redirect:/login";
        }

        model.addAttribute("customer", customer);
        return "account";
    }

    @PostMapping("/logout")
    public String logoutCustomer(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    public static class RegistrationForm {

        @NotBlank(message = "Please enter your full name.")
        @Size(max = 100,
                message = "Name must be 100 characters or fewer.")
        private String fullName;

        @NotBlank(message = "Please enter your email.")
        @Email(message = "Please enter a valid email address.")
        @Size(max = 150,
                message = "Email must be 150 characters or fewer.")
        private String email;

        @NotBlank(message = "Please enter a password.")
        @Size(min = 8, max = 72,
                message = "Password must contain between 8 and 72 characters.")
        private String password;

        @NotBlank(message = "Please enter your phone number.")
        @Size(max = 30,
                message = "Phone number is too long.")
        private String phone;

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }

    public static class LoginForm {

        @NotBlank(message = "Please enter your email.")
        @Email(message = "Please enter a valid email address.")
        private String email;

        @NotBlank(message = "Please enter your password.")
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
