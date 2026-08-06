package com.thierno.flashsaleservice.repository;

import com.thierno.flashsaleservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
}
