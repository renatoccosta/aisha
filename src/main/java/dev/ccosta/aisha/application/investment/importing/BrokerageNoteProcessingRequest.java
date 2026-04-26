package dev.ccosta.aisha.application.investment.importing;

/**
 * Input for a brokerage note processor.
 *
 * @param accountId selected account used by the import routine
 * @param originalFileName uploaded file name
 * @param fileHash SHA-256 hash of the uploaded file
 * @param fileContent raw uploaded file bytes
 */
public record BrokerageNoteProcessingRequest(
    Long accountId,
    String originalFileName,
    String fileHash,
    byte[] fileContent
) {

    public BrokerageNoteProcessingRequest {
        fileContent = fileContent == null ? new byte[0] : fileContent.clone();
    }

    @Override
    public byte[] fileContent() {
        return fileContent.clone();
    }
}
