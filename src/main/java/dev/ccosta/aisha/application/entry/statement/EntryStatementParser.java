package dev.ccosta.aisha.application.entry.statement;

import java.util.List;

public interface EntryStatementParser {

    EntryStatementFormat format();

    List<EntryStatementImportRecord> parse(byte[] fileContent);
}
