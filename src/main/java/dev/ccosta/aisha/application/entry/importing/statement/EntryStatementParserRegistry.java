package dev.ccosta.aisha.application.entry.importing.statement;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class EntryStatementParserRegistry {

    private final Map<String, EntryStatementParser> parserById;
    private final List<EntryStatementFormat> formats;

    public EntryStatementParserRegistry(List<EntryStatementParser> parsers) {
        this.parserById = parsers.stream().collect(
            java.util.stream.Collectors.toUnmodifiableMap(parser -> parser.format().id(), Function.identity())
        );
        this.formats = parsers
            .stream()
            .map(EntryStatementParser::format)
            .sorted(Comparator.comparing(EntryStatementFormat::id))
            .toList();
    }

    public List<EntryStatementFormat> listFormats() {
        return formats;
    }

    public EntryStatementParser resolve(String formatId) {
        if (formatId == null || formatId.isBlank()) {
            throw new IllegalArgumentException("Statement format must be informed");
        }

        EntryStatementParser parser = parserById.get(formatId);
        if (parser == null) {
            throw new IllegalArgumentException("Statement format is not supported");
        }

        return parser;
    }
}
