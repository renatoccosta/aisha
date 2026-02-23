package dev.ccosta.aisha.application.entry.statement;

import org.springframework.stereotype.Component;

@Component
public class OfxStatementParser extends AbstractOfxStatementParser {

    @Override
    public EntryStatementFormat format() {
        return new EntryStatementFormat(
            "ofx",
            "entries.statementImport.format.ofx.label",
            "entries.statementImport.format.ofx.fileHelp"
        );
    }
}
