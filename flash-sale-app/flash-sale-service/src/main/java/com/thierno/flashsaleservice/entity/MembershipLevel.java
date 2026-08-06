package com.thierno.flashsaleservice.entity;

/**
 * Declared low-to-high on purpose: ordinal() doubles as the priority weight used when
 * ranking purchase requests and when checking the early-access threshold.
 */
public enum MembershipLevel {
    STANDARD,
    SILVER,
    GOLD,
    PLATINUM;

    /**
     * The highest tier a customer's cumulative purchaseCount alone qualifies them for.
     * Thresholds are cumulative purchase counts across all flash sales.
     */
    public static MembershipLevel eligibleFor(int purchaseCount) {
        if (purchaseCount >= 20) return PLATINUM;
        if (purchaseCount >= 10) return GOLD;
        if (purchaseCount >= 5) return SILVER;
        return STANDARD;
    }
}
