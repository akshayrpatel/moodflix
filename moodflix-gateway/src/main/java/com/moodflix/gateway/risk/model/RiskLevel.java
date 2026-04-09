package com.moodflix.gateway.risk.model;

/**
 * Severity of a detected threat or access decision.
 * Aligns with Okta's Low/Medium/High risk categories.
 */
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}