package com.thierno.flashsaleservice.entity;

/**
 * Declared low-to-high on purpose: ordinal() doubles as the priority weight used when
 * ranking purchase requests and when checking the early-access threshold.
 */
public enum MembershipLevel {
    STANDARD,
    SILVER,
    GOLD,
    PLATINUM
}
