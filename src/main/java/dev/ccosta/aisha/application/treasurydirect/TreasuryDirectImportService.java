package dev.ccosta.aisha.application.treasurydirect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.entry.categorization.EntryCategorySuggestion;
import dev.ccosta.aisha.application.entry.categorization.EntryCategorySuggestionRequest;
import dev.ccosta.aisha.application.entry.categorization.EntryCategorySuggestionService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntrySource;
import dev.ccosta.aisha.domain.entry.EntryType;
import dev.ccosta.aisha.domain.entry.categorization.EntryCategorySuggestionStatus;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import dev.ccosta.aisha.domain.asset.Asset;
import dev.ccosta.aisha.domain.asset.AssetIndexerType;
import dev.ccosta.aisha.domain.asset.AssetRepository;
import dev.ccosta.aisha.domain.asset.AssetType;
import dev.ccosta.aisha.domain.operation.InvestmentOperation;
import dev.ccosta.aisha.domain.operation.InvestmentEntryEffectPolicy;
import dev.ccosta.aisha.domain.operation.InvestmentOperationEntryLink;
import dev.ccosta.aisha.domain.operation.InvestmentOperationEntryLinkRepository;
import dev.ccosta.aisha.domain.operation.InvestmentOperationRepository;
import dev.ccosta.aisha.domain.operation.InvestmentOperationSourceType;
import dev.ccosta.aisha.domain.operation.InvestmentOperationType;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Imports Treasury Direct operations JSON files by creating assets, operations, financial entries, and links.
 */
@Service
public class TreasuryDirectImportService {

    private static final int SUPPORTED_FORMAT_VERSION = 1;
    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 6;
    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AccountService accountService;
    private final AssetRepository assetRepository;
    private final InvestmentOperationRepository operationRepository;
    private final InvestmentOperationEntryLinkRepository linkRepository;
    private final EntryRepository entryRepository;
    private final EntryCategorySuggestionService entryCategorySuggestionService;

    public TreasuryDirectImportService(
        AccountService accountService,
        AssetRepository assetRepository,
        InvestmentOperationRepository operationRepository,
        InvestmentOperationEntryLinkRepository linkRepository,
        EntryRepository entryRepository,
        EntryCategorySuggestionService entryCategorySuggestionService
    ) {
        this.accountService = accountService;
        this.assetRepository = assetRepository;
        this.operationRepository = operationRepository;
        this.linkRepository = linkRepository;
        this.entryRepository = entryRepository;
        this.entryCategorySuggestionService = entryCategorySuggestionService;
    }

    /**
     * Imports a Treasury Direct operations file for the selected account.
     *
     * @param accountId account where entries and operations will be recorded
     * @param originalFileName uploaded file name
     * @param fileHash SHA-256 hash of the uploaded file
     * @param fileContent raw uploaded file bytes
     * @return import summary
     */
    @Transactional
    public TreasuryDirectImportSummary importFile(Long accountId, String originalFileName, String fileHash, byte[] fileContent) {
        Instant startedAt = Instant.now();
        Account account = accountService.findById(accountId);
        JsonNode root = parseRoot(fileContent);
        validateRoot(root);

        JsonNode source = root.path("source");
        JsonNode title = root.path("title");
        JsonNode institution = root.path("institution");
        JsonNode operations = root.path("operations");
        Asset asset = resolveOrCreateAsset(title, institution);

        int importedOperations = 0;
        int importedEntries = 0;
        int skippedDuplicateOperations = 0;

        for (int index = 0; index < operations.size(); index++) {
            JsonNode operationNode = operations.get(index);
            validateOperation(operationNode, index);
            String externalId = externalId(source.path("url").asText(), title.path("code").asInt(), index, operationNode);
            if (operationRepository.existsByExternalId(externalId)) {
                skippedDuplicateOperations++;
                continue;
            }

            InvestmentOperation operation = toOperation(operationNode, asset, account, externalId, originalFileName, fileHash, index);
            InvestmentOperation savedOperation = operationRepository.save(operation);
            Entry entry = saveEntry(operationNode, savedOperation, account, title.path("description").asText());
            saveLink(savedOperation, entry);
            importedOperations++;
            importedEntries++;
        }

        long durationMillis = Duration.between(startedAt, Instant.now()).toMillis();
        return new TreasuryDirectImportSummary(importedOperations, importedEntries, skippedDuplicateOperations, durationMillis);
    }

    private JsonNode parseRoot(byte[] fileContent) {
        try {
            return objectMapper.readTree(fileContent);
        } catch (IOException ex) {
            throw new UnsupportedTreasuryDirectFormatException("Unsupported Treasury Direct file format", ex);
        }
    }

    private void validateRoot(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new UnsupportedTreasuryDirectFormatException("Unsupported Treasury Direct file format");
        }
        requireInt(root, "formatVersion");
        if (root.path("formatVersion").asInt() != SUPPORTED_FORMAT_VERSION) {
            throw new UnsupportedTreasuryDirectFormatException("Unsupported Treasury Direct format version");
        }
        requireObject(root, "source");
        requireObject(root, "institution");
        requireObject(root, "title");
        requireObject(root, "period");
        requireArray(root, "operations");
        if (root.path("operations").isEmpty()) {
            throw new UnsupportedTreasuryDirectFormatException("Treasury Direct file has no operations");
        }

        JsonNode source = root.path("source");
        requireText(source, "url");
        requireText(source, "dataType");
        if (!"operations".equals(source.path("dataType").asText())) {
            throw new UnsupportedTreasuryDirectFormatException("Unsupported Treasury Direct data type");
        }

        JsonNode institution = root.path("institution");
        requireInt(institution, "code");
        JsonNode title = root.path("title");
        requireInt(title, "code");
        requireText(title, "description");
        if (!title.path("maturityDate").isNull()) {
            parseDateTime(title.path("maturityDate").asText(), "title.maturityDate");
        }
    }

    private void validateOperation(JsonNode operationNode, int index) {
        if (operationNode == null || !operationNode.isObject()) {
            throw new UnsupportedTreasuryDirectFormatException("Treasury Direct operation must be an object");
        }
        requireText(operationNode, "date");
        parseDateTime(operationNode.path("date").asText(), "operations[" + index + "].date");
        requireText(operationNode, "operationType");
        requireNumber(operationNode, "amount");
        InvestmentOperationType operationType = mapOperationType(operationNode.path("operationType").asText(), operationNode.path("type"));
        if (changesQuantity(operationType)) {
            requireNumber(operationNode, "quantity");
            requireNumber(operationNode, "price");
        }
    }

    private Asset resolveOrCreateAsset(JsonNode title, JsonNode institution) {
        String name = title.path("description").asText().trim();
        return assetRepository.findByNameIgnoreCase(name)
            .orElseGet(() -> createAsset(title, institution, name));
    }

    private Asset createAsset(JsonNode title, JsonNode institution, String name) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType(AssetType.BOND_GOV);
        asset.setIssuer("Tesouro Nacional");
        asset.setCurrency("BRL");
        asset.setMaturityDate(title.path("maturityDate").isNull() ? null : parseDateTime(title.path("maturityDate").asText(), "title.maturityDate"));
        asset.setIndexerType(resolveIndexerType(name));
        return assetRepository.save(asset);
    }

    private InvestmentOperation toOperation(
        JsonNode operationNode,
        Asset asset,
        Account account,
        String externalId,
        String originalFileName,
        String fileHash,
        int index
    ) {
        InvestmentOperationType operationType = mapOperationType(operationNode.path("operationType").asText(), operationNode.path("type"));
        BigDecimal amount = money(operationNode.path("amount").decimalValue());
        InvestmentOperation operation = new InvestmentOperation();
        operation.setAsset(asset);
        operation.setAccount(account);
        operation.setOperationType(operationType);
        LocalDate date = parseDateTime(operationNode.path("date").asText(), "operation.date");
        operation.setTradeDate(date);
        operation.setSettlementDate(date);
        operation.setQuantity(optionalDecimal(operationNode, "quantity"));
        operation.setUnitPrice(optionalDecimal(operationNode, "price"));
        operation.setGrossAmount(amount);
        operation.setNetAmount(amount);
        operation.setFees(operationType == InvestmentOperationType.FEE ? amount : ZERO_MONEY);
        operation.setTaxes(ZERO_MONEY);
        operation.setCurrency("BRL");
        operation.setNotes(notes(operationNode, originalFileName, fileHash, index));
        operation.setExternalId(externalId);
        operation.setIndexerSpread(optionalRate(operationNode, "annualRate"));
        operation.setSourceType(InvestmentOperationSourceType.TREASURY_DIRECT);
        return operation;
    }

    private Entry saveEntry(JsonNode operationNode, InvestmentOperation operation, Account account, String assetName) {
        Entry entry = new Entry();
        entry.setAccount(account);
        entry.setMovementDate(operation.getTradeDate());
        entry.setSettlementDate(operation.getSettlementDate());
        entry.setDescription(entryDescription(operation.getOperationType(), assetName));
        entry.setNotes(entryNotes(operationNode));
        entry.setExternalId(operation.getExternalId());
        entry.setAmount(signedEntryAmount(operation));
        entry.setEntrySource(EntrySource.IMPORT);
        entry.setRegistrationDate(LocalDate.now());
        entry.setEntryType(EntryType.REGULAR);
        entry.setEntryEffect(InvestmentEntryEffectPolicy.resolve(operation.getOperationType()));
        applyCategorySuggestion(entry, account);
        return entryRepository.save(entry);
    }

    private void applyCategorySuggestion(Entry entry, Account account) {
        entryCategorySuggestionService.suggest(new EntryCategorySuggestionRequest(account.getId(), entry.getDescription(), entry.getAmount()))
            .ifPresentOrElse(
                suggestion -> applySuggestedCategory(entry, suggestion),
                () -> {
                    entry.setCategory(null);
                    entry.setSuggestedCategory(null);
                    entry.setCategorySuggestionConfidence(null);
                    entry.setCategorySuggestionStatus(EntryCategorySuggestionStatus.NONE);
                }
            );
    }

    private void applySuggestedCategory(Entry entry, EntryCategorySuggestion suggestion) {
        entry.setCategory(suggestion.category());
        entry.setSuggestedCategory(suggestion.category());
        entry.setCategorySuggestionConfidence(suggestion.confidence());
        entry.setCategorySuggestionStatus(EntryCategorySuggestionStatus.PENDING);
    }

    private void saveLink(InvestmentOperation operation, Entry entry) {
        InvestmentOperationEntryLink link = new InvestmentOperationEntryLink();
        link.setOperation(operation);
        link.setEntry(entry);
        link.setAllocatedAmount(operation.getNetAmount());
        linkRepository.save(link);
    }

    private String externalId(String sourceUrl, int titleCode, int index, JsonNode operationNode) {
        String payload = sourceUrl + "|" + titleCode + "|" + index + "|" + operationNode.toString();
        return "treasury-direct:v1:" + sha256(payload.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available", ex);
        }
    }

    private InvestmentOperationType mapOperationType(String rawType, JsonNode cashFlowType) {
        return switch (rawType) {
            case "purchase" -> InvestmentOperationType.BUY;
            case "sale" -> InvestmentOperationType.SELL;
            case "interest" -> InvestmentOperationType.INTEREST;
            case "fee" -> InvestmentOperationType.FEE;
            case "redemption" -> InvestmentOperationType.REDEMPTION;
            case "transfer" -> mapTransferType(cashFlowType);
            default -> throw new UnsupportedTreasuryDirectFormatException("Unsupported Treasury Direct operation type");
        };
    }

    private InvestmentOperationType mapTransferType(JsonNode cashFlowType) {
        if (cashFlowType == null || cashFlowType.isNull()) {
            throw new UnsupportedTreasuryDirectFormatException("Treasury Direct transfer type must be informed");
        }
        return switch (cashFlowType.asText()) {
            case "credit" -> InvestmentOperationType.TRANSFER_IN;
            case "debit" -> InvestmentOperationType.TRANSFER_OUT;
            default -> throw new UnsupportedTreasuryDirectFormatException("Unsupported Treasury Direct transfer direction");
        };
    }

    private BigDecimal signedEntryAmount(InvestmentOperation operation) {
        BigDecimal amount = operation.getNetAmount() == null ? ZERO_MONEY : operation.getNetAmount().abs();
        return switch (operation.getOperationType()) {
            case BUY, SUBSCRIPTION, TAX, FEE, TRANSFER_OUT -> amount.negate();
            case SELL, DIVIDEND, INTEREST, AMORTIZATION, COUPON, REDEMPTION, TRANSFER_IN -> amount;
            default -> amount;
        };
    }

    private String entryDescription(InvestmentOperationType operationType, String assetName) {
        return switch (operationType) {
            case BUY -> "Tesouro Direto - Compra - " + assetName;
            case SELL -> "Tesouro Direto - Venda - " + assetName;
            case REDEMPTION -> "Tesouro Direto - Resgate - " + assetName;
            case INTEREST -> "Tesouro Direto - Juros - " + assetName;
            case FEE -> "Tesouro Direto - Taxa - " + assetName;
            case TRANSFER_IN -> "Tesouro Direto - Transferência de entrada - " + assetName;
            case TRANSFER_OUT -> "Tesouro Direto - Transferência de saída - " + assetName;
            default -> "Tesouro Direto - " + assetName;
        };
    }

    private String notes(JsonNode operationNode, String originalFileName, String fileHash, int index) {
        return "Importado do Tesouro Direto. Arquivo: " + nullToBlank(originalFileName)
            + ". Hash: " + nullToBlank(fileHash)
            + ". Operação #" + (index + 1)
            + ". Tipo original: " + operationNode.path("operationType").asText()
            + ". Direção original: " + (operationNode.path("type").isNull() ? "não informada" : operationNode.path("type").asText()) + ".";
    }

    private String entryNotes(JsonNode operationNode) {
        return "Lançamento criado automaticamente pela importação do Tesouro Direto. Tipo original: "
            + operationNode.path("operationType").asText() + ".";
    }

    private AssetIndexerType resolveIndexerType(String name) {
        String normalizedName = name.toUpperCase(Locale.ROOT);
        if (normalizedName.contains("SELIC") || normalizedName.contains("LFT")) {
            return AssetIndexerType.SELIC;
        }
        if (normalizedName.contains("IPCA") || normalizedName.contains("NTN-B")) {
            return AssetIndexerType.IPCA;
        }
        if (normalizedName.contains("PREFIXADO") || normalizedName.contains("LTN") || normalizedName.contains("NTN-F")) {
            return AssetIndexerType.PREFIXED;
        }
        return AssetIndexerType.NONE;
    }

    private boolean changesQuantity(InvestmentOperationType type) {
        return switch (type) {
            case BUY, SELL, REDEMPTION, TRANSFER_IN, TRANSFER_OUT -> true;
            default -> false;
        };
    }

    private LocalDate parseDateTime(String value, String fieldName) {
        try {
            return OffsetDateTime.parse(value).toLocalDate();
        } catch (DateTimeParseException ex) {
            throw new UnsupportedTreasuryDirectFormatException("Invalid Treasury Direct date field: " + fieldName, ex);
        }
    }

    private BigDecimal optionalDecimal(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        return field.isMissingNode() || field.isNull() ? null : field.decimalValue();
    }

    private BigDecimal optionalRate(JsonNode node, String fieldName) {
        BigDecimal value = optionalDecimal(node, fieldName);
        return value == null ? null : value.setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private void requireObject(JsonNode node, String fieldName) {
        if (!node.path(fieldName).isObject()) {
            throw new UnsupportedTreasuryDirectFormatException("Treasury Direct object field is required: " + fieldName);
        }
    }

    private void requireArray(JsonNode node, String fieldName) {
        if (!node.path(fieldName).isArray()) {
            throw new UnsupportedTreasuryDirectFormatException("Treasury Direct array field is required: " + fieldName);
        }
    }

    private void requireText(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        if (!field.isTextual() || !StringUtils.hasText(field.asText())) {
            throw new UnsupportedTreasuryDirectFormatException("Treasury Direct text field is required: " + fieldName);
        }
    }

    private void requireInt(JsonNode node, String fieldName) {
        if (!node.path(fieldName).canConvertToInt()) {
            throw new UnsupportedTreasuryDirectFormatException("Treasury Direct integer field is required: " + fieldName);
        }
    }

    private void requireNumber(JsonNode node, String fieldName) {
        if (!node.path(fieldName).isNumber()) {
            throw new UnsupportedTreasuryDirectFormatException("Treasury Direct numeric field is required: " + fieldName);
        }
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
