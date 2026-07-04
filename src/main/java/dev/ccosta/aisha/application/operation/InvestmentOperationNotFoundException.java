package dev.ccosta.aisha.application.operation;

public class InvestmentOperationNotFoundException extends RuntimeException {

    public InvestmentOperationNotFoundException(Long id) {
        super("Investment operation not found: " + id);
    }
}
