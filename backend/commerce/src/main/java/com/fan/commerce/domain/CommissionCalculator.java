package com.fan.commerce.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Calculates commission on a source-provided final GMV that already includes refunds. */
public final class CommissionCalculator {
    private CommissionCalculator() { }

    public static BigDecimal calculate(BigDecimal finalGmv, BigDecimal rate) {
        Objects.requireNonNull(finalGmv, "finalGmv");
        Objects.requireNonNull(rate, "rate");
        if (finalGmv.signum() < 0 || rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("GMV must be non-negative and rate must be between 0 and 1");
        }
        return finalGmv.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
