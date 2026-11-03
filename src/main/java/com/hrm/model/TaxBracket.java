package com.hrm.model;

import java.math.BigDecimal;

public record TaxBracket(int bracketNo, BigDecimal lowerBound, BigDecimal upperBound, BigDecimal rate) {
}
