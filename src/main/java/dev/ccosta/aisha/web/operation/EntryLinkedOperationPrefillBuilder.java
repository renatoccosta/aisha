package dev.ccosta.aisha.web.operation;

import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.asset.Asset;
import dev.ccosta.aisha.domain.operation.InvestmentOperationType;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Builds an investment operation form prefilled from a regular financial entry.
 */
@Component
public class EntryLinkedOperationPrefillBuilder {

    private static final Pattern TICKER_PATTERN = Pattern.compile("\\b([A-Z]{4}\\d{1,2}F?|[A-Z]{3,6}\\d{2})\\b");
    private static final Pattern QUANTITY_WITH_LABEL_PATTERN = Pattern.compile(
        "(?i)(?:qtd|qtde|quantidade|cotas?|acoes?|a[cç][oõ]es)\\s*[:=]?\\s*([0-9]+(?:[.,][0-9]+)?)"
    );
    private static final Pattern LABEL_AFTER_QUANTITY_PATTERN = Pattern.compile(
        "(?i)([0-9]+(?:[.,][0-9]+)?)\\s*(?:cotas?|acoes?|a[cç][oõ]es)\\b"
    );
    private static final Pattern QUANTITY_BEFORE_ASSET_PATTERN = Pattern.compile(
        "(?i)\\b([0-9]+(?:\\.[0-9]{3})*(?:,[0-9]+)?|[0-9]+(?:,[0-9]+)?)\\s+DE\\s+[A-Z0-9]"
    );
    private static final Pattern OPERATION_PREFIX_QUANTITY_PATTERN = Pattern.compile(
        "(?i)^\\s*(?:compra|venda|aplic(?:a[cç][aã]o)?|resgate)\\s+([0-9]+(?:\\.[0-9]{3})*(?:,[0-9]+)?|[0-9]+(?:,[0-9]+)?)\\s+"
    );
    private static final Pattern SLASH_TRAILING_QUANTITY_PATTERN = Pattern.compile(
        "(?i)\\bS/\\s*([0-9]+(?:[.,][0-9]+)*)\\s*$"
    );
    private static final Pattern DESCRIPTIVE_BUY_ASSET_PATTERN = Pattern.compile(
        "(?i)^\\s*(?:compra|aplic(?:a[cç][aã]o)?)\\s+(?:[0-9]+(?:\\.[0-9]{3})*(?:,[0-9]+)?|[0-9]+(?:,[0-9]+)?)\\s+(.+?)\\s*$"
    );
    private static final Pattern TRAILING_DATE_PATTERN = Pattern.compile("\\s+\\d{1,2}/\\d{1,2}/\\d{2,4}\\s*$");

    /**
     * Creates a form using entry data and best-effort description inference.
     *
     * @param entry source regular entry
     * @param availableAssets assets that can be selected by the rendered form
     * @param newAssetOptionId sentinel value used by the operation form for a new asset
     * @return a prefilled operation form
     */
    public InvestmentOperationForm build(Entry entry, List<Asset> availableAssets, Long newAssetOptionId) {
        InvestmentOperationForm form = new InvestmentOperationForm();
        String description = entry.getDescription();

        form.setAccountId(entry.getAccount().getId());
        form.setTradeDate(entry.getMovementDate());
        form.setSettlementDate(entry.getSettlementDate());
        form.setOperationType(inferOperationType(description));
        form.setQuantity(inferQuantity(description).orElse(null));
        form.setGrossAmount(entry.getAmount());
        form.setNetAmount(entry.getAmount());
        form.setFees(BigDecimal.ZERO);
        form.setTaxes(BigDecimal.ZERO);
        form.setNotes(description);
        form.setLinkedEntryIds(List.of(entry.getId()));

        Optional<Asset> inferredAsset = inferExistingAsset(description, availableAssets);
        if (inferredAsset.isPresent()) {
            form.setAssetId(inferredAsset.get().getId());
            return form;
        }

        inferNewAssetName(description).ifPresent(assetName -> {
            form.setAssetId(newAssetOptionId);
            form.setNewAssetName(assetName);
        });
        return form;
    }

    InvestmentOperationType inferOperationType(String description) {
        String normalized = normalize(description);
        if (containsAny(normalized, "venda", "vendido")) {
            return InvestmentOperationType.SELL;
        }
        if (containsAny(normalized, "dividendo", "dividendos", "div ")) {
            return InvestmentOperationType.DIVIDEND;
        }
        if (containsAny(normalized, "jcp", "juros sobre capital", "juros", "rendimento", "rendimentos")) {
            return InvestmentOperationType.INTEREST;
        }
        if (containsAny(normalized, "amortizacao", "amortiz")) {
            return InvestmentOperationType.AMORTIZATION;
        }
        if (containsAny(normalized, "resgate")) {
            return InvestmentOperationType.REDEMPTION;
        }
        if (containsAny(normalized, "subscricao", "subscr")) {
            return InvestmentOperationType.SUBSCRIPTION;
        }
        if (containsAny(normalized, "irrf", "imposto", "tributo", "ir s/")) {
            return InvestmentOperationType.TAX;
        }
        if (containsAny(normalized, "taxa", "tarifa", "corretagem", "emolumento")) {
            return InvestmentOperationType.FEE;
        }
        if (containsAny(normalized, "compra", "comprado", "aplicacao", "aplic ")) {
            return InvestmentOperationType.BUY;
        }
        return InvestmentOperationType.BUY;
    }

    Optional<BigDecimal> inferQuantity(String description) {
        Optional<BigDecimal> quantity = firstQuantityMatch(QUANTITY_WITH_LABEL_PATTERN, description);
        if (quantity.isPresent()) {
            return quantity;
        }
        quantity = firstQuantityMatch(LABEL_AFTER_QUANTITY_PATTERN, description);
        if (quantity.isPresent()) {
            return quantity;
        }
        quantity = firstQuantityMatch(QUANTITY_BEFORE_ASSET_PATTERN, description);
        if (quantity.isPresent()) {
            return quantity;
        }
        quantity = firstQuantityMatch(OPERATION_PREFIX_QUANTITY_PATTERN, description);
        return quantity.isPresent() ? quantity : firstQuantityMatch(SLASH_TRAILING_QUANTITY_PATTERN, description);
    }

    Optional<Asset> inferExistingAsset(String description, List<Asset> assets) {
        String normalizedDescription = normalize(description);
        return assets.stream()
            .filter(asset -> asset.getId() != null)
            .filter(asset -> assetMatches(normalizedDescription, asset))
            .max(Comparator.comparingInt(asset -> bestMatchLength(asset)));
    }

    Optional<String> inferNewAssetName(String description) {
        if (!StringUtils.hasText(description)) {
            return Optional.empty();
        }
        Matcher matcher = TICKER_PATTERN.matcher(description.toUpperCase(Locale.ROOT));
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        Optional<String> descriptiveAssetName = inferDescriptiveAssetName(description);
        if (descriptiveAssetName.isPresent()) {
            return descriptiveAssetName;
        }
        return Optional.empty();
    }

    private Optional<String> inferDescriptiveAssetName(String description) {
        if (isGenericPublicBondDescription(description)) {
            return Optional.empty();
        }
        Matcher matcher = DESCRIPTIVE_BUY_ASSET_PATTERN.matcher(description);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String assetName = TRAILING_DATE_PATTERN.matcher(matcher.group(1).trim()).replaceFirst("").trim();
        return StringUtils.hasText(assetName) ? Optional.of(assetName) : Optional.empty();
    }

    private boolean isGenericPublicBondDescription(String description) {
        String normalized = normalize(description);
        return containsAny(normalized, "tit publicos", "titulos publicos", "titls.publicos", "tesouro direto");
    }

    private Optional<BigDecimal> firstQuantityMatch(Pattern pattern, String description) {
        if (!StringUtils.hasText(description)) {
            return Optional.empty();
        }
        Matcher matcher = pattern.matcher(description);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String numericText = normalizeBrazilianNumber(matcher.group(1));
        try {
            return Optional.of(new BigDecimal(numericText));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private String normalizeBrazilianNumber(String rawNumericText) {
        String value = rawNumericText.trim();
        if (value.matches("\\d{1,3}(?:[.,]\\d{3})+")) {
            return value.replace(".", "").replace(",", "");
        }

        int lastComma = value.lastIndexOf(',');
        int lastDot = value.lastIndexOf('.');
        int decimalSeparatorIndex = Math.max(lastComma, lastDot);
        if (decimalSeparatorIndex < 0) {
            return value;
        }

        String integerPart = value.substring(0, decimalSeparatorIndex).replace(".", "").replace(",", "");
        String decimalPart = value.substring(decimalSeparatorIndex + 1);
        return integerPart + "." + decimalPart;
    }

    private boolean assetMatches(String normalizedDescription, Asset asset) {
        return tokenMatches(normalizedDescription, asset.getTicker())
            || tokenMatches(normalizedDescription, asset.getName())
            || tokenMatches(normalizedDescription, asset.getIsin());
    }

    private boolean tokenMatches(String normalizedDescription, String candidate) {
        String normalizedCandidate = normalize(candidate);
        return StringUtils.hasText(normalizedCandidate) && normalizedDescription.contains(normalizedCandidate);
    }

    private int bestMatchLength(Asset asset) {
        return Math.max(
            Math.max(normalize(asset.getTicker()).length(), normalize(asset.getName()).length()),
            normalize(asset.getIsin()).length()
        );
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).trim();
    }
}
