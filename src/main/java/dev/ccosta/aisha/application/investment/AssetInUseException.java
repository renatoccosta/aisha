package dev.ccosta.aisha.application.investment;

public class AssetInUseException extends RuntimeException {

    public AssetInUseException(Long id) {
        super("Asset is in use and cannot be deleted: " + id);
    }
}
