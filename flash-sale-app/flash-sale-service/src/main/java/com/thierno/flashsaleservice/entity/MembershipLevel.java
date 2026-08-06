package com.thierno.flashsaleservice.entity;


public enum MembershipLevel {
    STANDARD,
    SILVER,
    GOLD,
    PLATINUM;

    public static MembershipLevel eligibleFor(int purchaseCount) {
        if (purchaseCount >= 20) return PLATINUM;
        if (purchaseCount >= 10) return GOLD;
        if (purchaseCount >= 5) return SILVER;
        return STANDARD;
    }
}
