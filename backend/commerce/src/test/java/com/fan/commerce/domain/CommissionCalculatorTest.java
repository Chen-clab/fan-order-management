package com.fan.commerce.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CommissionCalculatorTest {
    @Test void usesAlreadyRefundAdjustedGmvAndProductSpecificRates() {
        assertEquals(new BigDecimal("65.00"), CommissionCalculator.calculate(new BigDecimal("1300"), new BigDecimal("0.05")));
        assertEquals(new BigDecimal("100.00"), CommissionCalculator.calculate(new BigDecimal("1000"), new BigDecimal("0.10")));
    }
    @Test void roundsToCentsAndRejectsInvalidInput() {
        assertEquals(new BigDecimal("0.01"), CommissionCalculator.calculate(new BigDecimal("0.10"), new BigDecimal("0.05")));
        assertThrows(IllegalArgumentException.class, () -> CommissionCalculator.calculate(new BigDecimal("-1"), BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> CommissionCalculator.calculate(BigDecimal.ONE, new BigDecimal("1.01")));
        assertThrows(NullPointerException.class, () -> CommissionCalculator.calculate(null, BigDecimal.ONE));
    }
}
