package dev.ccosta.aisha.application.entry.statement;

import dev.ccosta.aisha.application.entry.EntryImportFailureCause;
import dev.ccosta.aisha.application.entry.EntryImportValidationException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public abstract class AbstractOfxStatementParser implements EntryStatementParser {

    private static final Pattern XML_ENCODING_PATTERN = Pattern.compile(
        "<\\?xml[^>]*encoding\\s*=\\s*['\"]([^'\"]+)['\"]",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern HEADER_LINE_PATTERN = Pattern.compile("(?m)^([A-Z]+)\\s*:\\s*(.+?)\\s*$");
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final int MAX_NOTES_LENGTH = 1000;
    private static final int MAX_EXTERNAL_ID_LENGTH = 255;
    private static final DateTimeFormatter OFX_BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter OFX_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4)
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .optionalStart()
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .optionalStart()
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .optionalStart()
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .optionalEnd()
        .optionalEnd()
        .optionalEnd()
        .toFormatter(Locale.ROOT);

    @Override
    public final List<EntryStatementImportRecord> parse(byte[] fileContent) {
        String rawContent = preprocessRawContent(decodeRawContent(fileContent));
        if (!StringUtils.hasText(rawContent)) {
            return List.of();
        }
        String bodyContent = extractBodyContent(rawContent);
        Document xmlDocument = tryParseXmlBody(bodyContent);
        if (xmlDocument != null) {
            return extractFromXmlDocument(xmlDocument);
        }
        return extractFromSgmlBody(bodyContent);
    }

    private String decodeRawContent(byte[] fileContent) {
        if (fileContent == null || fileContent.length == 0) {
            return "";
        }

        String asciiPreview = new String(fileContent, StandardCharsets.ISO_8859_1);
        Charset declaredCharset = resolveDeclaredCharset(asciiPreview);
        if (declaredCharset != null) {
            return new String(fileContent, declaredCharset);
        }

        if (isValidUtf8(fileContent)) {
            return new String(fileContent, StandardCharsets.UTF_8);
        }

        return new String(fileContent, Charset.forName("windows-1252"));
    }

    private Charset resolveDeclaredCharset(String asciiPreview) {
        String bodyPreview = extractBodyContent(asciiPreview);
        Matcher xmlMatcher = XML_ENCODING_PATTERN.matcher(bodyPreview);
        if (xmlMatcher.find()) {
            Charset xmlCharset = toCharset(xmlMatcher.group(1));
            if (xmlCharset != null) {
                return xmlCharset;
            }
        }

        Map<String, String> headers = parseHeaderMetadata(asciiPreview);
        Charset headerCharset = resolveOfxHeaderCharset(headers);
        if (headerCharset != null) {
            return headerCharset;
        }
        return null;
    }

    private Map<String, String> parseHeaderMetadata(String preview) {
        int firstTagIndex = preview.indexOf('<');
        String headerSection = firstTagIndex >= 0 ? preview.substring(0, firstTagIndex) : preview;
        Matcher matcher = HEADER_LINE_PATTERN.matcher(headerSection);
        Map<String, String> headers = new LinkedHashMap<>();
        while (matcher.find()) {
            headers.put(matcher.group(1).trim().toUpperCase(Locale.ROOT), matcher.group(2).trim());
        }
        return headers;
    }

    private Charset resolveOfxHeaderCharset(Map<String, String> headers) {
        String charsetValue = headers.get("CHARSET");
        String encodingValue = headers.get("ENCODING");

        if (charsetValue != null) {
            String normalizedCharset = charsetValue.trim().toUpperCase(Locale.ROOT);
            if ("1252".equals(normalizedCharset)) {
                return Charset.forName("windows-1252");
            }
            if ("UTF-8".equals(normalizedCharset) || "UTF8".equals(normalizedCharset) || "65001".equals(normalizedCharset)) {
                return StandardCharsets.UTF_8;
            }
            if ("ISO-8859-1".equals(normalizedCharset) || "8859-1".equals(normalizedCharset)) {
                return StandardCharsets.ISO_8859_1;
            }
            Charset directCharset = toCharset(charsetValue);
            if (directCharset != null) {
                return directCharset;
            }
        }

        if (encodingValue != null) {
            String normalizedEncoding = encodingValue.trim().toUpperCase(Locale.ROOT);
            if ("USASCII".equals(normalizedEncoding) && "1252".equals(headers.getOrDefault("CHARSET", "").trim())) {
                return Charset.forName("windows-1252");
            }
            if ("UNICODE".equals(normalizedEncoding) || "UTF-8".equals(normalizedEncoding) || "UTF8".equals(normalizedEncoding)) {
                return StandardCharsets.UTF_8;
            }
        }

        return null;
    }

    private Charset toCharset(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Charset.forName(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isValidUtf8(byte[] bytes) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            CharBuffer ignored = decoder.decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException ex) {
            return false;
        }
    }

    protected String preprocessRawContent(String rawContent) {
        return rawContent == null ? "" : rawContent.trim();
    }

    protected String resolveDescription(OfxTransactionData transactionData) {
        return firstNonBlank(transactionData.fields().get("NAME"), transactionData.fields().get("MEMO"));
    }

    protected String resolveNotes(OfxTransactionData transactionData) {
        return trimToNull(transactionData.fields().get("MEMO"));
    }

    protected String resolveExternalId(OfxTransactionData transactionData) {
        return trimToNull(transactionData.fields().get("FITID"));
    }

    protected BigDecimal resolveAmount(OfxTransactionData transactionData) {
        String rawAmount = requireValue(transactionData.rowPosition(), "amount", transactionData.fields().get("TRNAMT"));
        try {
            return new BigDecimal(rawAmount);
        } catch (NumberFormatException ex) {
            throw validationError(transactionData.rowPosition(), "amount", "Invalid amount format");
        }
    }

    protected LocalDate resolveMovementDate(OfxTransactionData transactionData) {
        return parseOfxDate(
            requireValue(transactionData.rowPosition(), "movementDate", transactionData.fields().get("DTPOSTED")),
            transactionData.rowPosition(),
            "movementDate"
        );
    }

    protected LocalDate resolveSettlementDateForBank(OfxStatementContext context, OfxTransactionData transactionData) {
        return resolveMovementDate(transactionData);
    }

    protected LocalDate resolveSettlementDateForCreditCard(OfxStatementContext context, OfxTransactionData transactionData) {
        if (context.ledgerBalanceDate() == null) {
            throw validationError(transactionData.rowPosition(), "settlementDate", "Missing credit card ledger balance date");
        }
        return context.ledgerBalanceDate();
    }

    protected EntryImportValidationException validationError(int rowPosition, String columnName, String message) {
        return new EntryImportValidationException(rowPosition, EntryImportFailureCause.INVALID_FORMAT, columnName, message);
    }

    protected LocalDate parseOfxDate(String rawValue, int rowPosition, String fieldName) {
        String value = trimToNull(rawValue);
        if (value == null) {
            throw validationError(rowPosition, fieldName, "Missing required field: " + fieldName);
        }

        String normalized = normalizeOfxDateValue(value);
        try {
            if (normalized.contains("T")) {
                return OffsetDateTime.parse(normalized).toLocalDate();
            }
            return LocalDate.parse(normalized, OFX_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(normalized.substring(0, 8), OFX_BASIC_DATE);
            } catch (Exception ignored) {
                throw validationError(rowPosition, fieldName, "Invalid date format: " + fieldName);
            }
        }
    }

    protected enum OfxMessageType {
        BANK,
        CREDIT_CARD
    }

    protected record OfxStatementContext(OfxMessageType messageType, LocalDate ledgerBalanceDate) {
    }

    protected record OfxTransactionData(int rowPosition, Map<String, String> fields) {
    }

    private List<EntryStatementImportRecord> extractFromXmlDocument(Document document) {
        Element root = document.getDocumentElement();
        if (root == null || !"OFX".equalsIgnoreCase(root.getTagName())) {
            throw new EntryImportValidationException(1, EntryImportFailureCause.INVALID_FORMAT, null, "Invalid OFX root element");
        }

        List<EntryStatementImportRecord> records = new ArrayList<>();
        records.addAll(extractBankTransactionsFromXml(root));
        records.addAll(extractCreditCardTransactionsFromXml(root));
        if (records.isEmpty()) {
            throw new EntryImportValidationException(1, EntryImportFailureCause.INVALID_FORMAT, null, "No OFX transactions found");
        }
        return records;
    }

    private List<EntryStatementImportRecord> extractBankTransactionsFromXml(Element root) {
        List<EntryStatementImportRecord> results = new ArrayList<>();
        NodeList messageSets = root.getElementsByTagName("BANKMSGSRSV1");
        int row = 1;
        for (int i = 0; i < messageSets.getLength(); i++) {
            Element messageSet = asElement(messageSets.item(i));
            if (messageSet == null) {
                continue;
            }
            NodeList transactions = messageSet.getElementsByTagName("STMTTRN");
            for (int j = 0; j < transactions.getLength(); j++) {
                Element stmtTrn = asElement(transactions.item(j));
                if (stmtTrn == null) {
                    continue;
                }
                row++;
                results.add(toImportRecord(new OfxStatementContext(OfxMessageType.BANK, null), toTransactionDataFromXml(stmtTrn, row)));
            }
        }
        return results;
    }

    private List<EntryStatementImportRecord> extractCreditCardTransactionsFromXml(Element root) {
        List<EntryStatementImportRecord> results = new ArrayList<>();
        NodeList messageSets = root.getElementsByTagName("CREDITCARDMSGSRSV1");
        int row = 10_000;
        for (int i = 0; i < messageSets.getLength(); i++) {
            Element messageSet = asElement(messageSets.item(i));
            if (messageSet == null) {
                continue;
            }
            NodeList statements = messageSet.getElementsByTagName("CCSTMTRS");
            for (int s = 0; s < statements.getLength(); s++) {
                Element ccStatement = asElement(statements.item(s));
                if (ccStatement == null) {
                    continue;
                }
                LocalDate ledgerDate = parseOfxDate(
                    requireValue(row, "settlementDate", directChildText(optionalDirectChild(ccStatement, "LEDGERBAL"), "DTASOF")),
                    row,
                    "settlementDate"
                );
                NodeList transactions = ccStatement.getElementsByTagName("STMTTRN");
                for (int j = 0; j < transactions.getLength(); j++) {
                    Element stmtTrn = asElement(transactions.item(j));
                    if (stmtTrn == null) {
                        continue;
                    }
                    row++;
                    results.add(toImportRecord(new OfxStatementContext(OfxMessageType.CREDIT_CARD, ledgerDate), toTransactionDataFromXml(stmtTrn, row)));
                }
            }
        }
        return results;
    }

    private List<EntryStatementImportRecord> extractFromSgmlBody(String body) {
        SgmlCursor cursor = new SgmlCursor(body);
        Deque<String> path = new ArrayDeque<>();
        List<PendingTransaction> pendingTransactions = new ArrayList<>();
        Map<Integer, LocalDate> creditCardLedgerDateByStatementId = new LinkedHashMap<>();

        Map<String, String> currentTransactionFields = null;
        int currentTransactionRow = 1;
        OfxMessageType currentMessageType = null;
        boolean insideLedgerBal = false;
        Integer currentStatementId = null;
        int statementIdSequence = 0;

        SgmlToken token;
        while ((token = cursor.nextToken()) != null) {
            if (token.type() == SgmlTokenType.OPEN_TAG) {
                String normalizedTag = token.tagName().toUpperCase(Locale.ROOT);
                String inlineText = trimToNull(token.inlineText());

                if ("BANKMSGSRSV1".equals(normalizedTag)) {
                    currentMessageType = OfxMessageType.BANK;
                    path.push(normalizedTag);
                    continue;
                }
                if ("CREDITCARDMSGSRSV1".equals(normalizedTag)) {
                    currentMessageType = OfxMessageType.CREDIT_CARD;
                    path.push(normalizedTag);
                    continue;
                }
                if ("STMTRS".equals(normalizedTag) || "CCSTMTRS".equals(normalizedTag)) {
                    currentStatementId = ++statementIdSequence;
                    path.push(normalizedTag);
                    continue;
                }
                if ("STMTTRN".equals(normalizedTag)) {
                    currentTransactionFields = new LinkedHashMap<>();
                    currentTransactionRow = token.line();
                    path.push(normalizedTag);
                    continue;
                }
                if ("LEDGERBAL".equals(normalizedTag)) {
                    insideLedgerBal = true;
                    path.push(normalizedTag);
                    continue;
                }

                if (inlineText != null) {
                    if (currentTransactionFields != null && pathContains(path, "STMTTRN")) {
                        currentTransactionFields.put(normalizedTag, inlineText);
                    } else if (insideLedgerBal && "DTASOF".equals(normalizedTag) && currentMessageType == OfxMessageType.CREDIT_CARD && currentStatementId != null) {
                        creditCardLedgerDateByStatementId.put(currentStatementId, parseOfxDate(inlineText, token.line(), "settlementDate"));
                    }
                    continue;
                }

                path.push(normalizedTag);
                continue;
            }

            String normalizedTag = token.tagName().toUpperCase(Locale.ROOT);
            if ("STMTTRN".equals(normalizedTag) && currentTransactionFields != null && currentMessageType != null) {
                pendingTransactions.add(new PendingTransaction(currentMessageType, currentStatementId, new OfxTransactionData(currentTransactionRow, currentTransactionFields)));
                currentTransactionFields = null;
            }
            if ("LEDGERBAL".equals(normalizedTag)) {
                insideLedgerBal = false;
            }
            if ("STMTRS".equals(normalizedTag) || "CCSTMTRS".equals(normalizedTag)) {
                currentStatementId = null;
            }
            if ("BANKMSGSRSV1".equals(normalizedTag) || "CREDITCARDMSGSRSV1".equals(normalizedTag)) {
                currentMessageType = null;
            }
            popUntil(path, normalizedTag);
        }

        if (pendingTransactions.isEmpty()) {
            throw new EntryImportValidationException(1, EntryImportFailureCause.INVALID_FORMAT, null, "No OFX transactions found");
        }

        List<EntryStatementImportRecord> results = new ArrayList<>(pendingTransactions.size());
        for (PendingTransaction pending : pendingTransactions) {
            LocalDate ledgerDate = pending.messageType() == OfxMessageType.CREDIT_CARD
                ? creditCardLedgerDateByStatementId.get(pending.statementId())
                : null;
            results.add(toImportRecord(new OfxStatementContext(pending.messageType(), ledgerDate), pending.transactionData()));
        }
        return results;
    }

    private EntryStatementImportRecord toImportRecord(OfxStatementContext context, OfxTransactionData transactionData) {
        LocalDate movementDate = resolveMovementDate(transactionData);
        LocalDate settlementDate = context.messageType() == OfxMessageType.CREDIT_CARD
            ? resolveSettlementDateForCreditCard(context, transactionData)
            : resolveSettlementDateForBank(context, transactionData);
        String description = requireValue(transactionData.rowPosition(), "description", resolveDescription(transactionData));
        validateLength(description, MAX_DESCRIPTION_LENGTH, transactionData.rowPosition(), "description");
        String notes = resolveNotes(transactionData);
        validateLength(notes, MAX_NOTES_LENGTH, transactionData.rowPosition(), "notes");
        String externalId = resolveExternalId(transactionData);
        validateLength(externalId, MAX_EXTERNAL_ID_LENGTH, transactionData.rowPosition(), "externalId");
        BigDecimal amount = resolveAmount(transactionData);

        return new EntryStatementImportRecord(
            transactionData.rowPosition(),
            movementDate,
            settlementDate,
            description,
            amount,
            notes,
            externalId
        );
    }

    private OfxTransactionData toTransactionDataFromXml(Element stmtTrn, int rowPosition) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String tag : List.of("TRNTYPE", "DTPOSTED", "TRNAMT", "FITID", "NAME", "MEMO")) {
            String value = directChildText(stmtTrn, tag);
            if (value != null) {
                fields.put(tag, value);
            }
        }
        return new OfxTransactionData(rowPosition, fields);
    }

    private String stripOfxProcessingInstructions(String content) {
        String trimmed = content.stripLeading();
        if (trimmed.startsWith("<?xml")) {
            int xmlEnd = trimmed.indexOf("?>");
            if (xmlEnd >= 0) {
                String afterXml = trimmed.substring(xmlEnd + 2).stripLeading();
                if (afterXml.startsWith("<?OFX")) {
                    int ofxEnd = afterXml.indexOf("?>");
                    if (ofxEnd >= 0) {
                        return afterXml.substring(ofxEnd + 2).stripLeading();
                    }
                }
                return trimmed;
            }
        }
        if (trimmed.startsWith("<?OFX")) {
            int ofxEnd = trimmed.indexOf("?>");
            if (ofxEnd >= 0) {
                return trimmed.substring(ofxEnd + 2).stripLeading();
            }
        }
        return trimmed;
    }

    private String extractBodyContent(String rawContent) {
        String trimmed = rawContent == null ? "" : rawContent.stripLeading();
        int xmlStart = trimmed.indexOf("<?xml");
        int ofxPiStart = trimmed.indexOf("<?OFX");
        int ofxTagStart = trimmed.indexOf("<OFX");

        int bodyStart = firstNonNegative(xmlStart, ofxPiStart, ofxTagStart);
        if (bodyStart < 0) {
            return trimmed;
        }
        return trimmed.substring(bodyStart);
    }

    private Document tryParseXmlBody(String bodyContent) {
        try {
            String xmlCandidate = stripOfxProcessingInstructions(bodyContent);
            if (!xmlCandidate.stripLeading().startsWith("<OFX")) {
                return null;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xmlCandidate)));
            document.getDocumentElement().normalize();
            return document;
        } catch (Exception ex) {
            return null;
        }
    }

    private int firstNonNegative(int... values) {
        int result = -1;
        for (int value : values) {
            if (value < 0) {
                continue;
            }
            if (result < 0 || value < result) {
                result = value;
            }
        }
        return result;
    }

    private String normalizeOfxDateValue(String rawValue) {
        String value = rawValue.trim();
        int bracketIndex = value.indexOf('[');
        if (bracketIndex >= 0) {
            value = value.substring(0, bracketIndex);
        }
        int dotIndex = value.indexOf('.');
        if (dotIndex >= 0) {
            value = value.substring(0, dotIndex);
        }
        if (value.length() >= 14) {
            return value.substring(0, 4) + "-" + value.substring(4, 6) + "-" + value.substring(6, 8)
                + "T" + value.substring(8, 10) + ":" + value.substring(10, 12) + ":" + value.substring(12, 14) + ZoneOffset.UTC;
        }
        return value;
    }

    private String requireValue(int rowPosition, String fieldName, String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new EntryImportValidationException(
                rowPosition,
                EntryImportFailureCause.MISSING_REQUIRED_FIELD,
                fieldName,
                "Missing required field: " + fieldName
            );
        }
        return normalized;
    }

    private void validateLength(String value, int maxLength, int rowPosition, String fieldName) {
        if (value == null || value.length() <= maxLength) {
            return;
        }
        throw validationError(rowPosition, fieldName, "Invalid field size: " + fieldName);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String firstNonBlank(String first, String second) {
        String normalizedFirst = trimToNull(first);
        return normalizedFirst != null ? normalizedFirst : trimToNull(second);
    }

    private boolean pathContains(Deque<String> path, String tagName) {
        String normalized = tagName.toUpperCase(Locale.ROOT);
        return path.stream().anyMatch(normalized::equals);
    }

    private void popUntil(Deque<String> path, String targetTag) {
        while (!path.isEmpty()) {
            String current = path.pop();
            if (targetTag.equals(current)) {
                return;
            }
        }
    }

    private Element asElement(Node node) {
        return node instanceof Element element ? element : null;
    }

    private Element optionalDirectChild(Element parent, String tagName) {
        if (parent == null) {
            return null;
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element element && tagName.equalsIgnoreCase(element.getTagName())) {
                return element;
            }
        }
        return null;
    }

    private String directChildText(Element parent, String tagName) {
        Element child = optionalDirectChild(parent, tagName);
        return child == null ? null : trimToNull(child.getTextContent());
    }

    private record PendingTransaction(OfxMessageType messageType, Integer statementId, OfxTransactionData transactionData) {
    }

    private enum SgmlTokenType {
        OPEN_TAG,
        CLOSE_TAG
    }

    private record SgmlToken(SgmlTokenType type, String tagName, String inlineText, int line) {
    }

    private static final class SgmlCursor {
        private final String input;
        private int index;
        private int line = 1;

        private SgmlCursor(String input) {
            this.input = input == null ? "" : input;
        }

        private SgmlToken nextToken() {
            while (index < input.length()) {
                char current = input.charAt(index);
                if (current == '\n') {
                    line++;
                }
                if (current != '<') {
                    index++;
                    continue;
                }

                int tokenLine = line;
                int closeIndex = input.indexOf('>', index);
                if (closeIndex < 0) {
                    return null;
                }
                String rawTag = input.substring(index + 1, closeIndex).trim();
                index = closeIndex + 1;
                if (rawTag.isEmpty() || rawTag.startsWith("?")) {
                    continue;
                }
                if (rawTag.startsWith("/")) {
                    return new SgmlToken(SgmlTokenType.CLOSE_TAG, rawTag.substring(1).trim(), null, tokenLine);
                }

                int nextTagIndex = input.indexOf('<', index);
                String text = nextTagIndex < 0 ? input.substring(index) : input.substring(index, nextTagIndex);
                String inlineText = StringUtils.hasText(text) ? text.trim() : null;
                if (inlineText != null) {
                    for (int i = index; i < (nextTagIndex < 0 ? input.length() : nextTagIndex); i++) {
                        if (input.charAt(i) == '\n') {
                            line++;
                        }
                    }
                    index = nextTagIndex < 0 ? input.length() : nextTagIndex;
                }
                return new SgmlToken(SgmlTokenType.OPEN_TAG, rawTag, inlineText, tokenLine);
            }
            return null;
        }
    }
}
