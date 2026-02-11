package dev.ccosta.aisha.application.account;

import java.time.LocalDate;

public record AccountBalanceBucket(LocalDate startDate, LocalDate endDate) {
}
