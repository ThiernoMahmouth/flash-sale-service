package com.thierno.flashsaleservice.unitaires;

import com.thierno.flashsaleservice.dto.CreateCustomerRequest;
import com.thierno.flashsaleservice.entity.Customer;
import com.thierno.flashsaleservice.entity.MembershipLevel;
import com.thierno.flashsaleservice.exception.CustomerNotFoundException;
import com.thierno.flashsaleservice.repository.CustomerRepository;
import com.thierno.flashsaleservice.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void register_newCustomer_createsWithZeroPurchaseCount() {
        when(customerRepository.findById("cust-1")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer saved = customerService.register(new CreateCustomerRequest("cust-1", "Alice", MembershipLevel.GOLD));

        assertThat(saved.getCustomerId()).isEqualTo("cust-1");
        assertThat(saved.getMembershipLevel()).isEqualTo(MembershipLevel.GOLD);
        assertThat(saved.getPurchaseCount()).isZero();
    }

    @Test
    void register_existingCustomer_upgradesLevelWithoutResettingPurchaseCount() {
        Customer existing = new Customer("cust-1", "Alice", MembershipLevel.STANDARD, 7);
        when(customerRepository.findById("cust-1")).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer saved = customerService.register(new CreateCustomerRequest("cust-1", "Alice", MembershipLevel.PLATINUM));

        assertThat(saved.getMembershipLevel()).isEqualTo(MembershipLevel.PLATINUM);
        assertThat(saved.getPurchaseCount()).isEqualTo(7);
    }

    @Test
    void findById_whenMissing_throwsCustomerNotFound() {
        when(customerRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById("missing"))
                .isInstanceOf(CustomerNotFoundException.class);
    }
}
