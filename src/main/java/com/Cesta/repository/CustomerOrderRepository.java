package com.Cesta.repository;

import com.Cesta.model.CustomerOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository
        extends JpaRepository<CustomerOrder, Long> {

    @EntityGraph(attributePaths = "items")
    List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @EntityGraph(attributePaths = "items")
    Optional<CustomerOrder> findByIdAndCustomerId(
            Long orderId,
            Long customerId
    );
}