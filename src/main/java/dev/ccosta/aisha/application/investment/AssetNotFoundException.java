package dev.ccosta.aisha.application.investment;

public class AssetNotFoundException extends RuntimeException {

    public AssetNotFoundException(Long id) {
        super("Asset not found: " + id);
    }
}
