package com.thierno.flashsaleservice.controller;

import com.thierno.flashsaleservice.dto.CreateCustomerRequest;
import com.thierno.flashsaleservice.entity.Customer;
import com.thierno.flashsaleservice.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customer API", description = "Register customers and their membership level")
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Register a customer or update their membership level")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public Customer register(@Valid @RequestBody CreateCustomerRequest request) {
        return customerService.register(request);
    }

    @Operation(summary = "Get a customer's profile and purchase history")
    @GetMapping("/{id}")
    public Customer getById(@PathVariable String id) {
        return customerService.findById(id);
    }
}
