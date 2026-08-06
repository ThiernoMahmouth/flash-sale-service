package com.thierno.flashsaleservice.repository;

import com.thierno.flashsaleservice.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
}
