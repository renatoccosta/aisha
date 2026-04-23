package dev.ccosta.aisha.application.investment;

public class InvestmentOperationNotFoundException extends RuntimeException {

    public InvestmentOperationNotFoundException(Long id) {
        super("Investment operation not found: " + id);
    }
}
