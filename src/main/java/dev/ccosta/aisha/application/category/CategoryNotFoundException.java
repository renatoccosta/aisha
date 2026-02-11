package dev.ccosta.aisha.application.category;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Long id) {
        super("Category not found: " + id);
    }
}
