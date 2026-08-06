package com.thierno.flashsaleservice.controller;

import com.thierno.flashsaleservice.entity.Product;
import com.thierno.flashsaleservice.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product API", description = "Products available to be put on flash sale")
public class ProductController {

    private final ProductRepository productRepository;

    @Operation(summary = "List all products")
    @GetMapping
    public List<Product> getAll() {
        return productRepository.findAll();
    }
}
