package dev.ccosta.aisha.application.asset;

public class AssetNotFoundException extends RuntimeException {

    public AssetNotFoundException(Long id) {
        super("Asset not found: " + id);
    }
}
