package dev.ccosta.aisha.application.investment.importing;

import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntrySource;
import dev.ccosta.aisha.domain.entry.EntryType;
import dev.ccosta.aisha.domain.investment.Asset;
import dev.ccosta.aisha.domain.investment.AssetType;
import dev.ccosta.aisha.domain.investment.BrokerageNote;
import dev.ccosta.aisha.domain.investment.InvestmentOperation;
import dev.ccosta.aisha.domain.investment.InvestmentOperationSourceType;
import dev.ccosta.aisha.domain.investment.InvestmentOperationType;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Parses Rico PDF brokerage notes into import-ready domain objects.
 */
@Component
public class RicoPdfBrokerageNoteProcessor implements BrokerageNoteProcessor {

    private static final String BROKER_NAME = "Rico";
    private static final DateTimeFormatter BRAZILIAN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final Pattern NOTE_HEADER_PATTERN = Pattern.compile("^(\\d+)\\s+(\\d+)\\s+(\\d{2}/\\d{2}/\\d{4})$");
    private static final Pattern BROKER_CNPJ_PATTERN = Pattern.compile("C\\.N\\.P\\.J:\\s*([0-9]{2}\\.[0-9]{3}\\.[0-9]{3}/[0-9]{4}-[0-9]{2})");
    private static final Pattern OPERATION_PATTERN = Pattern.compile(
        "^1-BOVESPA\\s+([CV])\\s+(\\S+)\\s+(.+)\\s+(\\d{1,3}(?:\\.\\d{3})*|\\d+)\\s+([\\d.]+,\\d{2})\\s+([\\d.]+,\\d{2})\\s+([CD])$"
    );
    private static final Pattern NET_SETTLEMENT_PATTERN = Pattern.compile("Líquido para\\s+(\\d{2}/\\d{2}/\\d{4})\\s+([\\d.]+,\\d{2})\\s+([CD])");
    private static final Pattern TICKER_PATTERN = Pattern.compile("^[A-Z]{4}\\d{1,2}[A-Z]?$");
    private static final Pattern TRAILING_MARKER_PATTERN = Pattern.compile("^(?:@.*|#.*|EDJ|EJ|EDR|ED|EB|ER|NM|N1|N2)$");

    /**
     * Checks whether the uploaded file is a Rico PDF brokerage note.
     *
     * @param request processing input
     * @return true when the PDF contains Rico brokerage note markers
     */
    @Override
    public boolean supports(BrokerageNoteProcessingRequest request) {
        return extractText(request)
            .map(text -> text.contains("NOTA DE NEGOCIAÇÃO")
                && text.toUpperCase(Locale.ROOT).contains("RICO")
                && text.contains("C.N.P.J:"))
            .orElse(false);
    }

    /**
     * Parses one or more Rico brokerage notes from the uploaded PDF.
     *
     * @param request processing input
     * @return parsed brokerage notes and operations
     */
    @Override
    public List<ParsedBrokerageNote> process(BrokerageNoteProcessingRequest request) {
        try (PDDocument document = Loader.loadPDF(request.fileContent())) {
            List<RicoPage> pages = extractPages(document);
            Map<NoteKey, List<RicoPage>> pagesByNote = groupPagesByNote(pages);
            List<ParsedBrokerageNote> parsedNotes = new ArrayList<>();
            for (Map.Entry<NoteKey, List<RicoPage>> entry : pagesByNote.entrySet()) {
                parsedNotes.add(parseNote(entry.getKey(), entry.getValue(), request));
            }
            return parsedNotes;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read Rico brokerage note PDF", ex);
        }
    }

    private Optional<String> extractText(BrokerageNoteProcessingRequest request) {
        try (PDDocument document = Loader.loadPDF(request.fileContent())) {
            PDFTextStripper stripper = textStripper();
            return Optional.of(stripper.getText(document));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    private List<RicoPage> extractPages(PDDocument document) throws IOException {
        List<RicoPage> pages = new ArrayList<>();
        PDFTextStripper stripper = textStripper();
        for (int pageIndex = 1; pageIndex <= document.getNumberOfPages(); pageIndex++) {
            stripper.setStartPage(pageIndex);
            stripper.setEndPage(pageIndex);
            String text = stripper.getText(document);
            pages.add(toPage(text, pageIndex));
        }
        return pages;
    }

    private PDFTextStripper textStripper() throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        return stripper;
    }

    private RicoPage toPage(String text, int pageIndex) {
        List<String> lines = text.lines()
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toList();
        for (String line : lines) {
            Matcher matcher = NOTE_HEADER_PATTERN.matcher(line);
            if (matcher.matches()) {
                return new RicoPage(
                    pageIndex,
                    matcher.group(1),
                    Integer.parseInt(matcher.group(2)),
                    parseDate(matcher.group(3)),
                    text,
                    lines
                );
            }
        }
        throw new IllegalArgumentException("Unable to identify Rico brokerage note page header");
    }

    private Map<NoteKey, List<RicoPage>> groupPagesByNote(List<RicoPage> pages) {
        Map<NoteKey, List<RicoPage>> pagesByNote = new LinkedHashMap<>();
        for (RicoPage page : pages) {
            pagesByNote.computeIfAbsent(new NoteKey(page.noteNumber(), page.tradeDate()), ignored -> new ArrayList<>()).add(page);
        }
        return pagesByNote;
    }

    private ParsedBrokerageNote parseNote(NoteKey noteKey, List<RicoPage> pages, BrokerageNoteProcessingRequest request) {
        RicoPage firstPage = pages.getFirst();
        RicoPage lastPage = pages.getLast();
        String brokerCnpj = extractBrokerCnpj(firstPage.text());
        Settlement settlement = extractSettlement(lastPage.text());
        BigDecimal totalCosts = extractTotalCosts(lastPage.text());
        BrokerageNote note = new BrokerageNote();
        note.setBrokerName(BROKER_NAME);
        note.setBrokerCnpj(brokerCnpj);
        note.setNoteNumber(noteKey.noteNumber());
        note.setTradeDate(noteKey.tradeDate());
        note.setSettlementDate(settlement.date());
        note.setTotalCosts(totalCosts);
        note.setNetAmount(signedMoney(settlement.amount(), settlement.side()));
        note.setImportedAt(LocalDateTime.now());
        note.setOriginalFileName(request.originalFileName());
        note.setFileHash(request.fileHash());
        note.setNetEntry(netEntry(note));

        List<InvestmentOperation> operations = pages.stream()
            .flatMap(page -> page.lines().stream())
            .map(line -> parseOperation(line, note))
            .flatMap(Optional::stream)
            .toList();
        return new ParsedBrokerageNote(note, operations);
    }

    private String extractBrokerCnpj(String text) {
        Matcher matcher = BROKER_CNPJ_PATTERN.matcher(text);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Unable to identify Rico broker CNPJ");
        }
        return matcher.group(1);
    }

    private Settlement extractSettlement(String text) {
        Matcher matcher = NET_SETTLEMENT_PATTERN.matcher(text);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Unable to identify Rico brokerage note settlement summary");
        }
        return new Settlement(parseDate(matcher.group(1)), parseMoney(matcher.group(2)), matcher.group(3));
    }

    private BigDecimal extractTotalCosts(String text) {
        BigDecimal clearingCost = extractMoneyAfterLabel(text, "Taxa de liquidação");
        BigDecimal exchangeCost = extractMoneyAfterLabel(text, "Total Bovespa / Soma");
        BigDecimal operationalCost = extractMoneyAfterLabel(text, "Total Custos / Despesas");
        return clearingCost.add(exchangeCost).add(operationalCost);
    }

    private BigDecimal extractMoneyAfterLabel(String text, String label) {
        Pattern pattern = Pattern.compile(Pattern.quote(label) + "\\s+([\\d.]+,\\d{2})(?:\\s+[CD])?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? parseMoney(matcher.group(1)) : BigDecimal.ZERO;
    }

    private Entry netEntry(BrokerageNote note) {
        Entry entry = new Entry();
        entry.setMovementDate(note.getTradeDate());
        entry.setSettlementDate(note.getSettlementDate());
        entry.setDescription("Nota de corretagem Rico " + note.getNoteNumber());
        entry.setAmount(note.getNetAmount());
        entry.setEntrySource(EntrySource.IMPORT);
        entry.setEntryType(EntryType.REGULAR);
        entry.setRegistrationDate(LocalDate.now());
        entry.setExternalId("BROKERAGE_NOTE:RICO:" + note.getBrokerCnpj() + ":" + note.getNoteNumber() + ":" + note.getTradeDate());
        return entry;
    }

    private Optional<InvestmentOperation> parseOperation(String line, BrokerageNote note) {
        Matcher matcher = OPERATION_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        AssetDescriptor assetDescriptor = describeAsset(matcher.group(3));
        InvestmentOperation operation = new InvestmentOperation();
        operation.setAsset(assetDescriptor.toAsset());
        operation.setOperationType(toOperationType(matcher.group(1)));
        operation.setTradeDate(note.getTradeDate());
        operation.setSettlementDate(note.getSettlementDate());
        operation.setQuantity(parseQuantity(matcher.group(4)));
        operation.setUnitPrice(parseMoney(matcher.group(5)));
        operation.setGrossAmount(parseMoney(matcher.group(6)));
        operation.setNetAmount(parseMoney(matcher.group(6)));
        operation.setFees(BigDecimal.ZERO);
        operation.setTaxes(BigDecimal.ZERO);
        operation.setCurrency("BRL");
        operation.setSourceType(InvestmentOperationSourceType.BROKER_NOTE);
        operation.setNotes("Rico " + matcher.group(2) + " - " + assetDescriptor.originalSpecification());
        return Optional.of(operation);
    }

    private InvestmentOperationType toOperationType(String side) {
        return "C".equals(side) ? InvestmentOperationType.BUY : InvestmentOperationType.SELL;
    }

    private AssetDescriptor describeAsset(String rawSpecification) {
        List<String> tokens = new ArrayList<>(List.of(rawSpecification.trim().replaceAll("\\s+", " ").split(" ")));
        while (!tokens.isEmpty() && TRAILING_MARKER_PATTERN.matcher(tokens.getLast()).matches()) {
            tokens.removeLast();
        }
        String ticker = tokens.stream()
            .filter(token -> TICKER_PATTERN.matcher(token).matches())
            .findFirst()
            .orElse(null);
        AssetType type = assetType(tokens);
        String name = String.join(" ", tokens);
        if (ticker != null) {
            name = name.replace(ticker, "").replaceAll("\\s+", " ").trim();
        }
        if (!StringUtils.hasText(name)) {
            name = ticker;
        }
        return new AssetDescriptor(rawSpecification.trim().replaceAll("\\s+", " "), name, ticker, type);
    }

    private AssetType assetType(List<String> tokens) {
        if (!tokens.isEmpty() && "FII".equals(tokens.getFirst())) {
            return AssetType.FII;
        }
        if (tokens.contains("ON") || tokens.contains("PN")) {
            return AssetType.STOCK;
        }
        if (tokens.contains("CI")) {
            return AssetType.ETF;
        }
        return AssetType.OTHER;
    }

    private LocalDate parseDate(String value) {
        return LocalDate.parse(value, BRAZILIAN_DATE_FORMAT);
    }

    private BigDecimal parseQuantity(String value) {
        return new BigDecimal(value.replace(".", ""));
    }

    private BigDecimal parseMoney(String value) {
        return new BigDecimal(value.replace(".", "").replace(",", "."));
    }

    private BigDecimal signedMoney(BigDecimal amount, String side) {
        return "D".equals(side) ? amount.negate() : amount;
    }

    private record NoteKey(String noteNumber, LocalDate tradeDate) {
    }

    private record RicoPage(int pageIndex, String noteNumber, int notePageNumber, LocalDate tradeDate, String text, List<String> lines) {
    }

    private record Settlement(LocalDate date, BigDecimal amount, String side) {
    }

    private record AssetDescriptor(String originalSpecification, String name, String ticker, AssetType type) {

        private Asset toAsset() {
            Asset asset = new Asset();
            asset.setName(name);
            asset.setTicker(ticker);
            asset.setType(type);
            asset.setCurrency("BRL");
            return asset;
        }
    }
}
