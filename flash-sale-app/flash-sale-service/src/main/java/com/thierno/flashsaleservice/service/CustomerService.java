package com.thierno.flashsaleservice.service;

import com.thierno.flashsaleservice.dto.CreateCustomerRequest;
import com.thierno.flashsaleservice.entity.Customer;
import com.thierno.flashsaleservice.exception.CustomerNotFoundException;
import com.thierno.flashsaleservice.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * Upsert on purpose: registering an existing customerId again (e.g. a membership
     * upgrade) just updates their tier/name rather than failing.
     */
    @Transactional
    public Customer register(CreateCustomerRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseGet(Customer::new);
        customer.setCustomerId(request.customerId());
        customer.setName(request.name());
        customer.setMembershipLevel(request.membershipLevel());
        if (customer.getPurchaseCount() == null) {
            customer.setPurchaseCount(0);
        }
        return customerRepository.save(customer);
    }

    public Customer findById(String customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
}
