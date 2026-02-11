package dev.ccosta.aisha.application.category;

public class CategoryInUseException extends RuntimeException {

    public CategoryInUseException(Long id) {
        super("Category in use: " + id);
    }
}
