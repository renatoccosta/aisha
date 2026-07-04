package dev.ccosta.aisha.application.asset;

public class AssetInUseException extends RuntimeException {

    public AssetInUseException(Long id) {
        super("Asset is in use and cannot be deleted: " + id);
    }
}
