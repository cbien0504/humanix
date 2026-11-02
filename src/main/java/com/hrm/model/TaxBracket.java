package com.hrm.model;

import java.math.BigDecimal;

/** One bracket of the progressive personal income tax schedule ({@code upperBound} null = no limit). */
public record TaxBracket(int bracketNo, BigDecimal lowerBound, BigDecimal upperBound, BigDecimal rate) {
}
