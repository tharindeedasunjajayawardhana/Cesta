
package com.Cesta.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(length = 25)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(length = 80)
    private String city;

    @Column(length = 20)
    private String postalCode;

    @Column(length = 20)
    private String role = "CUSTOMER";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Customer() {
    }

    public Long getId() {
        return id;
    }

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
        this.email = email == null
                ? null
                : email.trim().toLowerCase();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getRole() {
        return role == null || role.isBlank()
                ? "CUSTOMER"
                : role.trim().toUpperCase();
    }

    public void setRole(String role) {
        this.role = role == null || role.isBlank()
                ? "CUSTOMER"
                : role.trim().toUpperCase();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
