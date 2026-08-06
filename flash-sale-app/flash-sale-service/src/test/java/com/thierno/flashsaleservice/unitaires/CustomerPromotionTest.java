package com.thierno.flashsaleservice.unitaires;

import com.thierno.flashsaleservice.entity.Customer;
import com.thierno.flashsaleservice.entity.MembershipLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerPromotionTest {

    @Test
    void eligibleFor_belowFive_isStandard() {
        assertThat(MembershipLevel.eligibleFor(0)).isEqualTo(MembershipLevel.STANDARD);
        assertThat(MembershipLevel.eligibleFor(4)).isEqualTo(MembershipLevel.STANDARD);
    }

    @Test
    void eligibleFor_atFive_isSilver() {
        assertThat(MembershipLevel.eligibleFor(5)).isEqualTo(MembershipLevel.SILVER);
        assertThat(MembershipLevel.eligibleFor(9)).isEqualTo(MembershipLevel.SILVER);
    }

    @Test
    void eligibleFor_atTen_isGold() {
        assertThat(MembershipLevel.eligibleFor(10)).isEqualTo(MembershipLevel.GOLD);
        assertThat(MembershipLevel.eligibleFor(19)).isEqualTo(MembershipLevel.GOLD);
    }

    @Test
    void eligibleFor_atTwenty_isPlatinum() {
        assertThat(MembershipLevel.eligibleFor(20)).isEqualTo(MembershipLevel.PLATINUM);
        assertThat(MembershipLevel.eligibleFor(1000)).isEqualTo(MembershipLevel.PLATINUM);
    }

    @Test
    void promoteIfEligible_crossingThreshold_promotesAndReturnsTrue() {
        Customer customer = new Customer("c1", "Alice", MembershipLevel.STANDARD, 5);

        boolean promoted = customer.promoteIfEligible();

        assertThat(promoted).isTrue();
        assertThat(customer.getMembershipLevel()).isEqualTo(MembershipLevel.SILVER);
    }

    @Test
    void promoteIfEligible_belowThreshold_doesNothing() {
        Customer customer = new Customer("c1", "Alice", MembershipLevel.STANDARD, 4);

        boolean promoted = customer.promoteIfEligible();

        assertThat(promoted).isFalse();
        assertThat(customer.getMembershipLevel()).isEqualTo(MembershipLevel.STANDARD);
    }

    @Test
    void promoteIfEligible_neverDemotes_evenIfCountWouldOnlyQualifyForLowerTier() {
        Customer customer = new Customer("c1", "Alice", MembershipLevel.PLATINUM, 2);

        boolean promoted = customer.promoteIfEligible();

        assertThat(promoted).isFalse();
        assertThat(customer.getMembershipLevel()).isEqualTo(MembershipLevel.PLATINUM);
    }

    @Test
    void promoteIfEligible_alreadyAtEligibleTier_doesNothing() {
        Customer customer = new Customer("c1", "Alice", MembershipLevel.SILVER, 7);

        boolean promoted = customer.promoteIfEligible();

        assertThat(promoted).isFalse();
        assertThat(customer.getMembershipLevel()).isEqualTo(MembershipLevel.SILVER);
    }
}
